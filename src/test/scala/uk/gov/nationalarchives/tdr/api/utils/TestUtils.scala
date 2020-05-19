package uk.gov.nationalarchives.tdr.api.utils

import java.sql.PreparedStatement
import java.util.UUID

import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.unmarshalling.{FromResponseUnmarshaller, Unmarshaller}
import akka.stream.Materializer
import com.tngtech.keycloakmock.api.KeycloakVerificationMock
import com.tngtech.keycloakmock.api.TokenConfig.aTokenConfig
import io.circe.Decoder
import io.circe.parser.decode
import uk.gov.nationalarchives.tdr.api.db.DbConnection
import uk.gov.nationalarchives.tdr.api.utils.TestUtils.userId

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.Source.fromResource

object TestUtils  {

  val defaultFileId = UUID.fromString("07a3a4bd-0281-4a6d-a4c1-8fa3239e1313")

  implicit class AwaitFuture[T](future: Future[T]) {
    def await(timeout: Duration = 2.seconds): T = {
      Await.result(future, timeout)
    }
  }
  private val tdrPort: Int = 8000
  private val testPort: Int = 8001
  private val tdrMock: KeycloakVerificationMock = createServer("tdr", tdrPort)
  private val testMock: KeycloakVerificationMock = createServer("test", testPort)

  private def createServer(realm: String, port: Int) = {
    val mock: KeycloakVerificationMock = new KeycloakVerificationMock(port, "tdr")
    mock.start()
    mock
  }

  val userId = UUID.fromString("4ab14990-ed63-4615-8336-56fbb9960300")

  def validUserToken(body: String = "Body"): OAuth2BearerToken = OAuth2BearerToken(tdrMock.getAccessToken(
    aTokenConfig()
      .withResourceRole("tdr", "tdr_user")
      .withClaim("body", body)
      .withClaim("user_id", userId)
      .build)
  )
  def validUserTokenNoBody: OAuth2BearerToken = OAuth2BearerToken(tdrMock.getAccessToken(
    aTokenConfig()
      .withResourceRole("tdr", "tdr_user")
      .withClaim("user_id", userId)
      .build)
  )
  def validBackendChecksToken(role: String): OAuth2BearerToken = OAuth2BearerToken(tdrMock.getAccessToken(
    aTokenConfig()
      .withResourceRole("tdr-backend-checks", role)
      .withClaim("user_id", userId)
      .build
  ))
  def invalidBackendChecksToken(): OAuth2BearerToken = OAuth2BearerToken(tdrMock.getAccessToken(
    aTokenConfig()
      .withResourceRole("tdr-backend-checks", "some_role").build
  ))

  def invalidToken: OAuth2BearerToken = OAuth2BearerToken(testMock.getAccessToken(aTokenConfig().build))

  case class GraphqlError(message: String, extensions: Option[GraphqlErrorExtensions])

  case class GraphqlErrorExtensions(code: String)

  case class Locations(column: Int, line: Int)

  def getDataFromFile[A](prefix: String)(fileName: String)(implicit decoder: Decoder[A]): A = {
    getDataFromString(fromResource(s"$prefix$fileName.json").mkString)
  }

  def getDataFromString[A](dataString: String)(implicit decoder: Decoder[A]): A = {
    val result: Either[io.circe.Error, A] = decode[A](dataString)
    result match {
      case Right(data) => data
      case Left(e) => throw e
    }
  }

  def unmarshalResponse[A]()(implicit mat: Materializer, ec: ExecutionContext, decoder: Decoder[A]): FromResponseUnmarshaller[A] = Unmarshaller(_ => {
    res => {
      Unmarshaller.stringUnmarshaller(res.entity).map(s => getDataFromString[A](s))
    }
  })

  def seedDatabaseWithDefaultEntries(): Unit = {
    val consignmentId = UUID.fromString("eb197bfb-43f7-40ca-9104-8f6cbda88506")
    createConsignment(consignmentId, userId)
    createFile(defaultFileId, consignmentId)
  }

  def createConsignment(consignmentId: UUID, userId: UUID): Unit = {
    val sql = s"insert into Consignment (ConsignmentId, SeriesId, UserId) VALUES ('$consignmentId', 1, '$userId')"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.executeUpdate()
  }

  def createFile(fileId: UUID, consignmentId: UUID): Unit = {
    val sql = s"insert into File (FileId, ConsignmentId) VALUES ('$fileId', '$consignmentId')"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.executeUpdate()
  }



}
