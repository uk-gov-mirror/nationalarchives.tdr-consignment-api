package uk.gov.nationalarchives.tdr.api.utils

import akka.http.scaladsl.model.headers.OAuth2BearerToken
import com.tngtech.keycloakmock.api.KeycloakVerificationMock
import com.tngtech.keycloakmock.api.TokenConfig.aTokenConfig

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object TestUtils {
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

  def validToken: OAuth2BearerToken = OAuth2BearerToken(tdrMock.getAccessToken(aTokenConfig().build))
  def invalidToken: OAuth2BearerToken = OAuth2BearerToken(testMock.getAccessToken(aTokenConfig().build))

}
