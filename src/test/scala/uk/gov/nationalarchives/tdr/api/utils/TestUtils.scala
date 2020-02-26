package uk.gov.nationalarchives.tdr.api.utils

import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.unmarshalling.{FromResponseUnmarshaller, Unmarshaller}
import akka.stream.Materializer
import com.tngtech.keycloakmock.api.KeycloakVerificationMock
import com.tngtech.keycloakmock.api.TokenConfig.aTokenConfig
import io.circe.Decoder
import io.circe.parser.decode

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.Source.fromResource

object TestUtils  {

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

  def validUserToken(body: String = "Body"): OAuth2BearerToken = OAuth2BearerToken(tdrMock.getAccessToken(
    aTokenConfig()
      .withResourceRole("tdr", "tdr_user")
      .withClaim("body", "Body")
      .withClaim("user_id", "1")
      .build)
  )
  def validUserTokenNoBody: OAuth2BearerToken = OAuth2BearerToken(tdrMock.getAccessToken(
    aTokenConfig()
      .withResourceRole("tdr", "tdr_user")
      .withClaim("user_id", "1")
      .build)
  )
  def validAdminToken: OAuth2BearerToken = OAuth2BearerToken(tdrMock.getAccessToken(
    aTokenConfig()
      .withResourceRole("tdr", "tdr_admin")
      .build)
  )
  def invalidToken: OAuth2BearerToken = OAuth2BearerToken(testMock.getAccessToken(aTokenConfig().build))

  case class GraphqlError(message: String, locations: List[Locations])

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


}
