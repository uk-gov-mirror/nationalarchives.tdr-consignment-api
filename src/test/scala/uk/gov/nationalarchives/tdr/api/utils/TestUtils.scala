package uk.gov.nationalarchives.tdr.api.utils

import java.sql.{PreparedStatement, Timestamp}
import java.time.Instant
import java.util.UUID

import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.unmarshalling.{FromResponseUnmarshaller, Unmarshaller}
import akka.stream.Materializer
import com.tngtech.keycloakmock.api.KeycloakVerificationMock
import com.tngtech.keycloakmock.api.TokenConfig.aTokenConfig
import io.circe.Decoder
import io.circe.parser.decode
import uk.gov.nationalarchives.tdr.api.db.DbConnection
import uk.gov.nationalarchives.tdr.api.service.FileMetadataService._

import scala.concurrent.ExecutionContext
import scala.io.Source.fromResource

object TestUtils {

  val defaultFileId: UUID = UUID.fromString("07a3a4bd-0281-4a6d-a4c1-8fa3239e1313")

  private val tdrPort: Int = 8000
  private val testPort: Int = 8001
  private val tdrMock: KeycloakVerificationMock = createServer("tdr", tdrPort)
  private val testMock: KeycloakVerificationMock = createServer("test", testPort)

  private def createServer(realm: String, port: Int): KeycloakVerificationMock = {
    val mock: KeycloakVerificationMock = new KeycloakVerificationMock(port, "tdr")
    mock.start()
    mock
  }

  val userId: UUID = UUID.fromString("4ab14990-ed63-4615-8336-56fbb9960300")
  val backendChecksUser: UUID = UUID.fromString("6847253d-b9c6-4ea9-b3c9-57542b8c6375")

  def validUserToken(body: String = "Code"): OAuth2BearerToken = OAuth2BearerToken(tdrMock.getAccessToken(
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
      .withClaim("user_id", backendChecksUser)
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
    val seriesId = UUID.fromString("1436ad43-73a2-4489-a774-85fa95daff32")
    createConsignment(consignmentId, userId, seriesId)
    createFile(defaultFileId, consignmentId)
    addClientSideProperties()
    createClientFileMetadata(defaultFileId)
  }

  def createConsignment(consignmentId: UUID, userId: UUID, seriesId: UUID = UUID.fromString("9e2e2a51-c2d0-4b99-8bef-2ca322528861")): Unit = {
    val sql = s"insert into Consignment (ConsignmentId, SeriesId, UserId) VALUES ('$consignmentId', '$seriesId', '$userId')"
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

  //scalastyle:off magic.number
  def addAntivirusMetadata(fileId: String, result: String = "Result of AVMetadata processing"): Unit = {
    val sql = s"insert into AVMetadata (FileId, Software, SoftwareVersion, DatabaseVersion, Result, Datetime) VALUES (?, ?, ?, ?, ?, ?)"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.setString(1, fileId)
    ps.setString(2, "Some antivirus software")
    ps.setString(3, "Some software version")
    ps.setString(4, "Some database version")
    ps.setString(5, result)
    ps.setTimestamp(6, Timestamp.from(FixedTimeSource.now))
    ps.executeUpdate()
  }

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

  def addFFIDMetadata(fileId: String): Unit = {
    val ffidMetadataId = java.util.UUID.randomUUID()
    val sql = s"insert into FFIDMetadata" +
      s"(FileId, Software, SoftwareVersion, BinarySignatureFileVersion, ContainerSignatureFileVersion, Method, Datetime, FFIDMetadataId)" +
      s"VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.setString(1, fileId)
    ps.setString(2, "TEST DATA software")
    ps.setString(3, "TEST DATA software version")
    ps.setString(4, "TEST DATA binary signature file version")
    ps.setString(5, "TEST DATA container signature file version")
    ps.setString(6, "TEST DATA method")
    ps.setTimestamp(7, Timestamp.from(FixedTimeSource.now))
    ps.setObject(8, ffidMetadataId)

    ps.executeUpdate()
  }

  def createClientFileMetadata(fileId: UUID): Unit = {
    val sql = "INSERT INTO FileMetadata(MetadataId, FileId, PropertyId, Value, Datetime, UserId) VALUES (?,?,?,?,?,?)"
    clientSideProperties.foreach(propertyName => {
      val selectSql = "SELECT PropertyId FROM FileProperty where Name = ?"
      val psSelect: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(selectSql)
      psSelect.setString(1, propertyName)
      val rs = psSelect.executeQuery()
      rs.next()
      val value = propertyName match {
        case ClientSideFileLastModifiedDate => Timestamp.from(Instant.now()).toString
        case ClientSideFileSize => "1"
        case _ => s"$propertyName Value"
      }
      val propertyId = rs.getString("PropertyId")
      val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
      ps.setString(1, UUID.randomUUID().toString)
      ps.setString(2, fileId.toString)
      ps.setString(3, propertyId)
      ps.setString(4, value)
      ps.setTimestamp(5, Timestamp.from(Instant.now()))
      ps.setString(6, UUID.randomUUID().toString)
      ps.executeUpdate()
    })

  }

  //scalastyle:on magic.number

  def addFileProperty(propertyId: String, name: String): Unit = {
    val sql = s"insert into FileProperty (PropertyId, Name) VALUES (?, ?)"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.setString(1, propertyId)
    ps.setString(2, name)

    ps.executeUpdate()
  }

  def addParentFolderName(consignmentId: UUID, parentFolderName: String): Unit = {
    val sql = s"update Consignment set ParentFolder=\'${parentFolderName}\' where ConsignmentId=\'${consignmentId}\'"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)

    ps.executeUpdate()
  }

  def addSeries(seriesId: UUID, bodyId: UUID, code: String): Unit = {
    val sql = s"insert into Series (SeriesId, BodyId, Code) VALUES (?, ?, ?)"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.setString(1, seriesId.toString)
    ps.setString(2, bodyId.toString)
    ps.setString(3, code)

    ps.executeUpdate()
  }

  def addClientSideProperties(): Unit = {
    clientSideProperties.foreach(propertyName => {
      addFileProperty(UUID.randomUUID().toString, propertyName)
    })
  }
}

