package routes

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.StatusCodes.ServerError
import akka.http.scaladsl.model.headers.{Authorization, HttpChallenge, OAuth2BearerToken}
import akka.http.scaladsl.server.{AuthenticationFailedRejection, Route}
import akka.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import akka.http.scaladsl.testkit.ScalatestRouteTest
import http.Routes.route

class GraphqlRouteSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest {
  val token =
    "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJpdHdBYVBhaVJGNWtOR2FPd0VPMHkycW8tbG5jUHJwQWJwZmwzXzlUYmxNIn0.eyJqdGkiOiI0ZjA0ODdiYi0xOGU2LTQzYTktODM0ZC1mY2UwYTZlNGMwY2MiLCJleHAiOjE1ODA3MzYyMDYsIm5iZiI6MCwiaWF0IjoxNTgwNzM1OTA2LCJpc3MiOiJodHRwczovL2F1dGgudGRyLWludGVncmF0aW9uLm5hdGlvbmFsYXJjaGl2ZXMuZ292LnVrL2F1dGgvcmVhbG1zL3RkciIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiI1MjY0MWE2NS0xMTM1LTRlNjYtYWQ3Ni1kNjdkMzEzMGQxNDgiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJ0ZHIiLCJhdXRoX3RpbWUiOjAsInNlc3Npb25fc3RhdGUiOiJmNzVlN2JkYS00YTQ5LTQ3NzUtYTBmZC1jZTgzN2RlYWUwNTIiLCJhY3IiOiIxIiwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6InByb2ZpbGUgZW1haWwiLCJjbGllbnRIb3N0IjoiODkuMTk3LjExNC4xOTciLCJjbGllbnRJZCI6InRkciIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwicHJlZmVycmVkX3VzZXJuYW1lIjoic2VydmljZS1hY2NvdW50LXRkciIsImNsaWVudEFkZHJlc3MiOiI4OS4xOTcuMTE0LjE5NyIsImVtYWlsIjoic2VydmljZS1hY2NvdW50LXRkckBwbGFjZWhvbGRlci5vcmcifQ.aiShr-d7k_iEyfvbkmPEBsp3E4L-9-Rt8yKyHhcGOEuPEISJa1WiGlwDPa4cNJw7-RC7HAtZBXh8sMB1CSgGy9xiycVdbDyCp_l6ROBz9vAcBnM9dpBF0hfgBul6L0fC1t5jvEm0PX91lQWoVulDgy-Cg-m20jwo9oHS9WpJxWgfcy9KBb3ZY2z9vHiN7Dlzg-8-RSXH363Yip3sIhLSVolINEk_UlTyCB5iOyixKqb-rmNnUUNnBOoZe-OSEu_BooN8bC7OEm4Z2Ix_0vA4tBVE8w5qHnZWC2S_QFIUsbBaTVVxm11ckmzpzZWXOHOLRIIT3KmsQLXQpDjncqaJKA"

  "The api " should "return ok" in {
    Get("/") ~> route ~> check {
      responseAs[String] shouldEqual "OK"
    }
  }

  "The api" should "return an missing credentials error" in {
    val credentials: OAuth2BearerToken = OAuth2BearerToken(token)
    Post("/graphql") ~> addCredentials(credentials) ~> Route.seal(route) ~> check {
      status should not be a[ServerError]
    }
  }
}
