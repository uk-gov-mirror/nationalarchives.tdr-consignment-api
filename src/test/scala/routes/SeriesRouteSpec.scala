package routes

import java.sql.PreparedStatement

import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import db.DbConnection
import http.Routes.route
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import utils.TestUtils.validToken

class SeriesRouteSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest with BeforeAndAfterEach  {

  override def beforeEach(): Unit = {
    DbConnection.db.source.createConnection().prepareStatement("delete from consignmentapi.Series").executeUpdate()
  }

  "The api " should "return an empty series list" in {
    val query: String = """{"query":"{getSeries{seriesid}}"}"""
    Post("/graphql").withEntity(ContentTypes.`application/json`, query) ~> addCredentials(validToken) ~> route ~> check {
      responseAs[String] shouldEqual """{"data":{"getSeries":[]}}"""
    }
  }

  "The api " should "return the expected data" in {
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement("insert into consignmentapi.Series (SeriesId) VALUES (1)")
    ps.executeUpdate()
    val query: String = """{"query":"{getSeries{seriesid}}"}"""
    Post("/graphql").withEntity(ContentTypes.`application/json`, query) ~> addCredentials(validToken) ~> route ~> check {
      responseAs[String] shouldEqual "{\"data\":{\"getSeries\":[{\"seriesid\":1}]}}"
    }
  }

  "The api " should "return all requested fields" in {
    val sql = "insert into consignmentapi.Series (SeriesId, BodyId, Name, Code, Description) VALUES (1,1,'Name','Code','Description')"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.executeUpdate()
    val query: String = """{"query":"{getSeries{seriesid, bodyid, name, code, description}}"}"""
    Post("/graphql").withEntity(ContentTypes.`application/json`, query) ~> addCredentials(validToken) ~> route ~> check {
      val result = "{\"data\":{\"getSeries\":[{\"name\":\"Name\",\"description\":\"Description\",\"seriesid\":1,\"code\":\"Code\",\"bodyid\":1}]}}"
      responseAs[String] shouldEqual result
    }
  }
}
