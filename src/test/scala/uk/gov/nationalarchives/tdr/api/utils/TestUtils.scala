package uk.gov.nationalarchives.tdr.api.utils

import java.sql.{PreparedStatement, Timestamp}
import java.util.UUID

import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.unmarshalling.{FromResponseUnmarshaller, Unmarshaller}
import akka.stream.Materializer
import com.tngtech.keycloakmock.api.KeycloakVerificationMock
import com.tngtech.keycloakmock.api.TokenConfig.aTokenConfig
import io.circe.Decoder
import io.circe.parser.decode
import uk.gov.nationalarchives.tdr.api.db.DbConnection

import scala.concurrent.ExecutionContext
import scala.io.Source.fromResource

object TestUtils {

  val defaultFileId = UUID.fromString("07a3a4bd-0281-4a6d-a4c1-8fa3239e1313")

  private val tdrPort: Int = 8000
  private val testPort: Int = 8001
  private val tdrMock: KeycloakVerificationMock = createServer("tdr", tdrPort)
  private val testMock: KeycloakVerificationMock = createServer("test", testPort)

  private def createServer(realm: String, port: Int) = {
    val mock: KeycloakVerificationMock = new KeycloakVerificationMock(port, "tdr")
    mock.start()
    mock
  }

  val userId: UUID = UUID.fromString("4ab14990-ed63-4615-8336-56fbb9960300")
  val backendChecksUser: UUID = UUID.fromString("6847253d-b9c6-4ea9-b3c9-57542b8c6375")
//  val metadataId: UUID = UUID.fromString("4ab14990-ed63-4615-8336-56fbb9960300")

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
      .withClaim("user_id", backendChecksUser)
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
    val sql = s"insert into File (FileId, ConsignmentId, UserId, Datetime) VALUES (?, ?, ?, ?)"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.setString(1, fileId.toString)
    ps.setString(2, consignmentId.toString)
    ps.setString(3, userId.toString)
    ps.setTimestamp(4, Timestamp.from(FixedTimeSource.now))
    ps.executeUpdate()
  }

  def addAntivirusMetadata(fileId: String): Unit = {
    val sql = s"insert into AVMetadata (FileId, Result, Datetime) VALUES (?, ?, ?)"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.setString(1, fileId)
    ps.setString(2, "Result of AVMetadata processing")
    ps.setTimestamp(3, Timestamp.from(FixedTimeSource.now))
    ps.executeUpdate()
  }

  //scalastyle:off magic.number
  def addFileMetadata(metadataId: String, fileId: String, propertyId: String): Unit = {
    val sql = s"insert into FileMetadata (MetadataId, FileId, PropertyId, Value, Datetime, UserId) VALUES (?, ?, ?, ?, ?, ?)"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.setString(1, metadataId)
    ps.setString(2, fileId)
    ps.setString(3, propertyId)
    ps.setString(4, "Result of FileMetadata processing")
    ps.setTimestamp(5, Timestamp.from(FixedTimeSource.now))
    ps.setString(6, userId.toString)

    ps.executeUpdate()
  }

  def addFileProperty(propertyId: String, name: String): Unit = {
    val sql = s"insert into FileProperty (PropertyId, Name) VALUES (?, ?)"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.setString(1, propertyId)
    ps.setString(2, name)

    ps.executeUpdate()
  }
}
