package uk.gov.nationalarchives.tdr.api.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.keycloak.representations.AccessToken
import uk.gov.nationalarchives.tdr.api.db.DbConnection
import uk.gov.nationalarchives.tdr.api.db.repository.{ConsignmentRepository, SeriesRepository, TransferAgreementRepository}
import uk.gov.nationalarchives.tdr.api.graphql.{ConsignmentApiContext, GraphQlTypes}
import sangria.ast.Document
import sangria.execution._
import sangria.marshalling.sprayJson._
import sangria.parser.QueryParser
import uk.gov.nationalarchives.tdr.api.service.{ConsignmentService, SeriesService, TransferAgreementService}
import spray.json.{JsObject, JsString, JsValue}
import uk.gov.nationalarchives.tdr.api.auth.ValidationAuthoriser.AuthorisationException
import uk.gov.nationalarchives.tdr.api.auth.ValidationAuthoriser

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


object GraphQLServer {

  val exceptionHandler = ExceptionHandler {
    case (_, AuthorisationException(message)) => HandledException(message)
  }

  def endpoint(requestJSON: JsValue, accessToken: AccessToken)(implicit ec: ExecutionContext): Route = {

    val JsObject(fields) = requestJSON

    val JsString(query) = fields("query")

    QueryParser.parse(query) match {
      case Success(queryAst) =>
        val operation = fields.get("operationName") collect {
          case JsString(op) => op
        }
        val variables = fields.get("variables") match {
          case Some(obj: JsObject) => obj
          case _ => JsObject.empty
        }
        complete(executeGraphQLQuery(queryAst, operation, variables, accessToken))
      case Failure(error) =>
        complete(BadRequest, JsObject("error" -> JsString(error.getMessage)))
    }

  }

  private def executeGraphQLQuery(query: Document, operation: Option[String], vars: JsObject, accessToken: AccessToken)
                                 (implicit ec: ExecutionContext): Future[(StatusCode with Serializable, JsValue)] = {
    val db = DbConnection.db
    val seriesService = new SeriesService(new SeriesRepository(db))
    val consignmentService = new ConsignmentService(new ConsignmentRepository(db))
    val transferAgreementService = new TransferAgreementService(new TransferAgreementRepository(db))
    val context = ConsignmentApiContext(accessToken, seriesService, consignmentService, transferAgreementService)
    Executor.execute(
      GraphQlTypes.schema,
      query,  context,
      variables = vars,
      operationName = operation,
      middleware = ValidationAuthoriser :: Nil,
      exceptionHandler = exceptionHandler
    ).map(OK -> _)
      .recover {
        case error: QueryAnalysisError => BadRequest -> error.resolveError
        case error: ErrorWithResolver => InternalServerError -> error.resolveError
      }
  }

}
