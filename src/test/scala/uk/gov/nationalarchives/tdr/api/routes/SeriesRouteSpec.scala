package uk.gov.nationalarchives.tdr.api.routes

import java.sql.PreparedStatement

import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.io.Source.fromResource
import uk.gov.nationalarchives.tdr.api.db.DbConnection
import uk.gov.nationalarchives.tdr.api.http.Routes.route
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._

class SeriesRouteSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest with BeforeAndAfterEach  {

  private val getSeriesJsonFilePrefix: String = "json/getseries_"
  private val addSeriesJsonFilePrefix: String = "json/addseries_"

  override def beforeEach(): Unit = {
    DbConnection.db.source.createConnection().prepareStatement("delete from consignmentapi.Series").executeUpdate()
  }

  val getSeriesQuery: String = fromResource(getSeriesJsonFilePrefix + "query_somedata.json").mkString

  "The api" should "return an empty series list" in {
    val expectedResult: String = fromResource(getSeriesJsonFilePrefix + "data_empty.json").mkString

    Post("/graphql").withEntity(ContentTypes.`application/json`, getSeriesQuery) ~> addCredentials(validUserToken("Body2")) ~> route ~> check {
      responseAs[String] shouldEqual expectedResult
    }
  }

  "The api" should "return the expected data" in {
    val ps: PreparedStatement = DbConnection.db.source.createConnection()
      .prepareStatement("insert into consignmentapi.Series (SeriesId, BodyId) VALUES (1, 1)")
    ps.executeUpdate()

    val expectedResult: String = fromResource(getSeriesJsonFilePrefix + "data_some.json").mkString

    Post("/graphql").withEntity(ContentTypes.`application/json`, getSeriesQuery) ~> addCredentials(validUserToken()) ~> route ~> check {
      responseAs[String] shouldEqual expectedResult
    }
  }

  "The api" should "return all requested fields" in {
    val sql = "insert into consignmentapi.Series (SeriesId, BodyId, Name, Code, Description) VALUES (1,1,'Name','Code','Description')"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.executeUpdate()

    val query: String = fromResource(getSeriesJsonFilePrefix + "query_alldata.json").mkString
    val expectedResult: String = fromResource(getSeriesJsonFilePrefix + "data_all.json").mkString

    Post("/graphql").withEntity(ContentTypes.`application/json`, query) ~> addCredentials(validUserToken()) ~> route ~> check {
      responseAs[String] shouldEqual expectedResult
    }
  }

  "The api" should "return an error if a user queries without a body argument" in {
    val query: String = fromResource(getSeriesJsonFilePrefix + "query_no_body.json").mkString
    val expectedResponse: String = fromResource(getSeriesJsonFilePrefix + "data_error_no_body.json").mkString

    Post("/graphql").withEntity(ContentTypes.`application/json`, query) ~> addCredentials(validUserToken()) ~> route ~> check {
      responseAs[String] shouldEqual expectedResponse
    }
  }

  "The api" should "return an error if a user queries with a different body to their own" in {
    val query: String = fromResource(getSeriesJsonFilePrefix + "query_incorrect_body.json").mkString
    val expectedResponse: String = fromResource(getSeriesJsonFilePrefix + "data_incorrect_body.json").mkString

    Post("/graphql").withEntity(ContentTypes.`application/json`, query) ~> addCredentials(validUserToken()) ~> route ~> check {
      responseAs[String] shouldEqual expectedResponse
    }
  }

  "The api" should "return an error if a user queries with the correct body but it is not set on their user" in {
    val query: String = fromResource(getSeriesJsonFilePrefix + "query_incorrect_body.json").mkString
    val expectedResponse: String = fromResource(getSeriesJsonFilePrefix + "data_error_incorrect_user.json").mkString

    Post("/graphql").withEntity(ContentTypes.`application/json`, query) ~> addCredentials(validUserTokenNoBody) ~> route ~> check {
      responseAs[String] shouldEqual expectedResponse
    }
  }

  "The api" should "return all series if an admin user queries without a body argument" in {
    val sql = "insert into consignmentapi.Series (SeriesId, BodyId) VALUES (1,1), (2, 2)"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.executeUpdate()

    val query: String = fromResource(getSeriesJsonFilePrefix + "query_admin.json").mkString
    val expectedResponse: String = fromResource(getSeriesJsonFilePrefix + "data_multipleseries.json").mkString

    Post("/graphql").withEntity(ContentTypes.`application/json`, query) ~> addCredentials(validAdminToken) ~> route ~> check {
      responseAs[String] shouldEqual expectedResponse
    }
  }

  "The api" should "return the correct series if an admin queries with a body argument" in {
    val sql = "insert into consignmentapi.Series (SeriesId, BodyId) VALUES (1,1), (2, 2)"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.executeUpdate()

    val query: String = fromResource(getSeriesJsonFilePrefix + "query_somedata.json").mkString
    val expectedResponse: String = fromResource(getSeriesJsonFilePrefix + "data_some.json").mkString

    Post("/graphql").withEntity(ContentTypes.`application/json`, query) ~> addCredentials(validAdminToken) ~> route ~> check {
      responseAs[String] shouldEqual expectedResponse
    }
  }

  "The api" should "return all requested fields from inserted Series object" in {

    val mutation: String = fromResource(addSeriesJsonFilePrefix + "mutation_alldata.json").mkString
    val expectedResult: String = fromResource(addSeriesJsonFilePrefix + "data_all.json").mkString

    Post("/graphql").withEntity(ContentTypes.`application/json`, mutation) ~> addCredentials(validAdminToken) ~> route ~> check {
      responseAs[String] shouldEqual expectedResult
    }
  }

  "The api" should "return the expected data from inserted Series object" in {

    val mutation: String = fromResource(addSeriesJsonFilePrefix + "mutation_somedata.json").mkString
    val expectedResult: String = fromResource(addSeriesJsonFilePrefix + "data_some.json").mkString

    Post("/graphql").withEntity(ContentTypes.`application/json`, mutation) ~> addCredentials(validAdminToken) ~> route ~> check {
      responseAs[String] shouldEqual expectedResult
    }
  }
}
