package uk.gov.nationalarchives.tdr.api.routes

import akka.http.scaladsl.model.{HttpProtocol, HttpResponse, StatusCode, StatusCodes}
import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.server.Directives._
import nl.altindag.log.LogCaptor
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import uk.gov.nationalarchives.tdr.api.http.{RouteLogging, Routes}
import uk.gov.nationalarchives.tdr.api.utils.TestRequest

import scala.jdk.CollectionConverters._

class RouteLoggingSpec extends AnyFlatSpec with BeforeAndAfterEach with TestRequest {

  private val logCaptor = LogCaptor.forClass(classOf[Routes])

  override def beforeEach(): Unit = {
    logCaptor.clearLogs()
  }

  "200 response" should "not be logged as warning" in {
    val resp: HttpResponse = createHttpResponse(StatusCodes.OK)

    Post("/") ~> RouteLogging.logging(complete(resp))

    logCaptor.getWarnLogs.isEmpty shouldBe(true)
  }

  "non 200 response" should "be logged as warning" in {
    val expectedWarningLogEntry = "Non 200 Response - Request: HttpRequest(HttpMethod(POST),http://example.com/," +
      "List(),HttpEntity.Strict(none/none,0 bytes total),HttpProtocol(HTTP/1.1)), Request Entity: FulfilledFuture(), " +
      "Response: FulfilledFuture(entity)"
    val resp: HttpResponse = createHttpResponse(StatusCodes.BadRequest)

    Post("/") ~> RouteLogging.logging(complete(resp))

    val warnings = logCaptor.getWarnLogs.asScala.toList
    warnings.size shouldBe(1)
    warnings.head shouldEqual(expectedWarningLogEntry)
  }

  "rejected request" should "be logged as warning" in {
    Post("/") ~> RouteLogging.logging(reject(AuthorizationFailedRejection))

    val warnings = logCaptor.getWarnLogs.asScala.toList
    warnings.size shouldBe(1)
    warnings.head shouldEqual("Rejected Reason: AuthorizationFailedRejection")
  }

  private def createHttpResponse(status: StatusCode): HttpResponse = {
    new HttpResponse(status, Seq(), "entity", HttpProtocol("Http/1.0"))
  }
}
