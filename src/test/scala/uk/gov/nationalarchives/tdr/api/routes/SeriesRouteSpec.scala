package uk.gov.nationalarchives.tdr.api.routes

import java.sql.PreparedStatement

import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import uk.gov.nationalarchives.tdr.api.db.DbConnection
import uk.gov.nationalarchives.tdr.api.http.Routes.route
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.io.Source.fromResource
import uk.gov.nationalarchives.tdr.api.utils.TestUtils.validToken

class SeriesRouteSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest with BeforeAndAfterEach  {

  override def beforeEach(): Unit = {
    DbConnection.db.source.createConnection().prepareStatement("delete from consignmentapi.Series").executeUpdate()
  }

  val getSeriesQuery: String = fromResource("json/getseries_query_somedata.json").mkString

  "The api" should "return an empty series list" in {
    val expectedResult: String = fromResource("json/getseries_data_empty.json").mkString

    Post("/graphql").withEntity(ContentTypes.`application/json`, getSeriesQuery) ~> addCredentials(validToken) ~> route ~> check {
      responseAs[String] shouldEqual expectedResult
    }
  }

  "The api" should "return the expected data" in {
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement("insert into consignmentapi.Series (SeriesId) VALUES (1)")
    ps.executeUpdate()

    val expectedResult: String = fromResource("json/getseries_data_some.json").mkString

    Post("/graphql").withEntity(ContentTypes.`application/json`, getSeriesQuery) ~> addCredentials(validToken) ~> route ~> check {
      responseAs[String] shouldEqual expectedResult
    }
  }

  "The api" should "return all requested fields" in {
    val sql = "insert into consignmentapi.Series (SeriesId, BodyId, Name, Code, Description) VALUES (1,1,'Name','Code','Description')"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.executeUpdate()

    val query: String = fromResource("json/getseries_query_alldata.json").mkString
    val expectedResult: String = fromResource("json/getseries_data_all.json").mkString

    Post("/graphql").withEntity(ContentTypes.`application/json`, query) ~> addCredentials(validToken) ~> route ~> check {
      responseAs[String] shouldEqual expectedResult
    }
  }

  "The api" should "return all requested fields from inserted Series object" in {

    val mutation: String = fromResource("json/addseries_mutation_alldata.json").mkString
    val expectedResult: String = fromResource("json/addseries_data_all.json").mkString

    Post("/graphql").withEntity(ContentTypes.`application/json`, mutation) ~> addCredentials(validToken) ~> route ~> check {
      responseAs[String] shouldEqual expectedResult
    }
  }

  "The api" should "return the expected data from inserted Series object" in {

    val mutation: String = fromResource("json/addseries_mutation_somedata.json").mkString
    val expectedResult: String = fromResource("json/addseries_data_some.json").mkString

    Post("/graphql").withEntity(ContentTypes.`application/json`, mutation) ~> addCredentials(validToken) ~> route ~> check {
      responseAs[String] shouldEqual expectedResult
    }
  }
}
