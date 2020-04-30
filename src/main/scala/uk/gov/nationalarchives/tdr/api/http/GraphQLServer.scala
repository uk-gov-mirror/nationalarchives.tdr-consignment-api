package uk.gov.nationalarchives.tdr.api.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.config.ConfigFactory
import sangria.ast.Document
import sangria.execution._
import sangria.marshalling.sprayJson._
import sangria.parser.QueryParser
import spray.json.{JsObject, JsString, JsValue}
import uk.gov.nationalarchives.tdr.api.auth.{AuthorisationException, ValidationAuthoriser}
import uk.gov.nationalarchives.tdr.api.db.DbConnection
import uk.gov.nationalarchives.tdr.api.db.repository.{ClientFileMetadataRepository, ConsignmentRepository, SeriesRepository, TransferAgreementRepository, _}
import uk.gov.nationalarchives.tdr.api.graphql.{ConsignmentApiContext, ErrorCodes, GraphQlTypes}
import uk.gov.nationalarchives.tdr.api.service._
import uk.gov.nationalarchives.tdr.keycloak.Token

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


object GraphQLServer {

  val exceptionHandler = ExceptionHandler {
    case (resultMarshaller, AuthorisationException(message)) => {
      val node = resultMarshaller.scalarNode(ErrorCodes.notAuthorised, "String", Set.empty)
      val additionalFields = Map("code" -> node)
      HandledException(message, additionalFields)
    }
  }

  def endpoint(requestJSON: JsValue, accessToken: Token)(implicit ec: ExecutionContext): Route = {

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

  private def executeGraphQLQuery(query: Document, operation: Option[String], vars: JsObject, accessToken: Token)
                                 (implicit ec: ExecutionContext): Future[(StatusCode with Serializable, JsValue)] = {
    val uuidSourceClass: Class[_] = Class.forName(ConfigFactory.load().getString("source.uuid"))
    val uuidSource: UUIDSource = uuidSourceClass.getDeclaredConstructor().newInstance().asInstanceOf[UUIDSource]
    val db = DbConnection.db

    val consignmentRepository = new ConsignmentRepository(db)

    val seriesService = new SeriesService(new SeriesRepository(db), uuidSource)
    val consignmentService = new ConsignmentService(consignmentRepository, new CurrentTimeSource, uuidSource)
    val transferAgreementService = new TransferAgreementService(new TransferAgreementRepository(db), uuidSource)
    val clientFileMetadataService = new ClientFileMetadataService(new ClientFileMetadataRepository(db), uuidSource)
    val fileService = new FileService(new FileRepository(db), consignmentRepository, new CurrentTimeSource, uuidSource)
    val transferringBodyService = new TransferringBodyService(new TransferringBodyRepository(db))
    val avMetadataService = new AVMetadataService(new AVMetadataRepository(db))

    val context = ConsignmentApiContext(
      accessToken,
      clientFileMetadataService,
      consignmentService,
      fileService,
      seriesService,
      transferAgreementService,
      transferringBodyService,
      avMetadataService
    )
    Executor.execute(
      GraphQlTypes.schema,
      query,  context,
      variables = vars,
      operationName = operation,
      middleware = new ValidationAuthoriser :: Nil,
      exceptionHandler = exceptionHandler
    ).map(OK -> _)
      .recover {
        case error: QueryAnalysisError => BadRequest -> error.resolveError
        case error: ErrorWithResolver => InternalServerError -> error.resolveError
      }
  }
}
