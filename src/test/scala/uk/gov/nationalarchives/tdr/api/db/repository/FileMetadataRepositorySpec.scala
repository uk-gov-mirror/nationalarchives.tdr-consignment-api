package uk.gov.nationalarchives.tdr.api.db.repository

import java.sql.{PreparedStatement, Timestamp}
import java.time.Instant
import java.util.UUID

import org.scalatest.{Assertion, BeforeAndAfterEach}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import uk.gov.nationalarchives.tdr.api.db.DbConnection
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileMetadataFields.SHA256ServerSideChecksum
import uk.gov.nationalarchives.tdr.api.utils.TestUtils
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._
import uk.gov.nationalarchives.Tables.FilemetadataRow

class FileMetadataRepositorySpec extends AnyFlatSpec with ScalaFutures with Matchers with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    val connection = DbConnection.db.source.createConnection()
    val psFileMetadata = connection.prepareStatement("delete from FileMetadata")
    val psFileProperty = connection.prepareStatement("delete from FileProperty")
    psFileMetadata.execute()
    psFileProperty.execute()
  }

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(2, Seconds))

  private def getFileChecksumMatches(fileId: UUID): Boolean = {
    val sql = s"SELECT ChecksumMatches FROM File where FileId = ?"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.setString(1, fileId.toString)
    val rs = ps.executeQuery()
    rs.next()
    rs.getBoolean("ChecksumMatches")
  }

  private def checkFileMetadataExists(fileId: UUID, fileProperty: String): Assertion = {
    val sql = s"SELECT COUNT(*) as count FROM FileMetadata WHERE FileId = ? AND PropertyName = ?"
    val ps = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.setString(1, fileId.toString)
    ps.setString(2, fileProperty)
    val rs = ps.executeQuery()
    rs.next()
    rs.getInt("count") should be(1)
  }

  "countProcessedChecksumInConsignment" should "return 0 if a consignment has no files" in {
    val db = DbConnection.db
    val fileMetadataRepository = new FileMetadataRepository(db)
    val consignmentId = UUID.fromString("c03fd4be-58c1-4cee-8d3c-d162bb4f7c01")

    TestUtils.createConsignment(consignmentId, userId)

    val consignmentFiles = fileMetadataRepository.countProcessedChecksumInConsignment(consignmentId).futureValue

    consignmentFiles shouldBe 0
  }

  "countProcessedChecksumInConsignment" should "return 0 if consignment has no checksum metadata for files" in {
    val db = DbConnection.db
    val fileMetadataRepository = new FileMetadataRepository(db)
    val consignmentId = UUID.fromString("64456c78-49bb-4bff-85c8-2fff053b9f8d")
    val fileOneId = UUID.fromString("2c2dedf5-56a1-497f-9f8a-19102739a056")
    val fileTwoId = UUID.fromString("c2d5c569-0fc4-4688-9523-157c4028f1b1")
    val fileThreeId = UUID.fromString("317c7084-d3d4-435b-acd2-cfb317793844")

    TestUtils.createConsignment(consignmentId, userId)
    TestUtils.createFile(fileOneId, consignmentId)
    TestUtils.createFile(fileTwoId, consignmentId)
    TestUtils.createFile(fileThreeId, consignmentId)
//  We have created files, but not added any fileMetadata for those files to the FileMetadata repository.
//  Thus, calling the countProcessedChecksumInConsignment method should return 0.

    val consignmentFiles = fileMetadataRepository.countProcessedChecksumInConsignment(consignmentId).futureValue

    consignmentFiles shouldBe 0
  }

  "countProcessedChecksumInConsignment" should "return the number of fileMetadata for files in a specified consignment" in {
    val db = DbConnection.db
    val fileMetadataRepository = new FileMetadataRepository(db)
    val consignmentOne = UUID.fromString("049a11d7-06f5-4b11-b786-640000de76e2")
    val consignmentTwo = UUID.fromString("dc2acd2c-3370-44fe-aeae-16aee30dc410")
    val fileOneId = "5a102380-25ec-4881-a484-87d56fe9a0b4"
    val fileTwoId = "f616272d-4d80-44cf-90de-996c3984847d"
    val fileThreeId = "40c67a87-70c0-4424-b934-a395459ddbe1"
    val metadataOneId = "e4440f43-20c6-4b6c-811d-349e633617e4"
    val metadataTwoId = "7e5abff1-1d86-4d7d-8ab4-c6a2934ec611"
    val metadataThreeId = "efbd5da6-909a-45b1-964f-87a3baf48816"

//  Need to create a consignment with files in it
    TestUtils.createConsignment(consignmentOne, userId)
    TestUtils.createConsignment(consignmentTwo, userId)
    TestUtils.createFile(UUID.fromString(fileOneId), consignmentOne)
    TestUtils.createFile(UUID.fromString(fileTwoId), consignmentOne)
    TestUtils.createFile(UUID.fromString(fileThreeId), consignmentTwo)

    TestUtils.addFileProperty(SHA256ServerSideChecksum)

//  Then need to add data to the FileMetadata repository for these files
    TestUtils.addFileMetadata(metadataOneId, fileOneId, SHA256ServerSideChecksum)
    TestUtils.addFileMetadata(metadataTwoId, fileTwoId, SHA256ServerSideChecksum)
    TestUtils.addFileMetadata(metadataThreeId, fileThreeId, SHA256ServerSideChecksum)

    val fileMetadataFilesConsignmentOne = fileMetadataRepository.countProcessedChecksumInConsignment(consignmentOne).futureValue
    val fileMetadataFilesConsignmentTwo = fileMetadataRepository.countProcessedChecksumInConsignment(consignmentTwo).futureValue

    fileMetadataFilesConsignmentOne shouldBe 2
    fileMetadataFilesConsignmentTwo shouldBe 1
  }

  "countProcessedChecksumInConsignment" should "return number of fileMetadata rows with repetitive data filtered out" in {
    val db = DbConnection.db
    val fileMetadataRepository = new FileMetadataRepository(db)
    val consignmentId = UUID.fromString("c6f78fef-704a-46a8-82c0-afa465199e66")
    val fileOneId = "20e0676a-f0a1-4051-9540-e7df1344ac11"
    val fileTwoId = "b5111f11-4dca-4f92-8239-505da567b9d0"
    val metadataId = "f4440f43-20c6-4b6c-811d-349e633617e5"

    TestUtils.createConsignment(consignmentId, userId)
    TestUtils.createFile(UUID.fromString(fileOneId), consignmentId)
    TestUtils.createFile(UUID.fromString(fileTwoId), consignmentId)
    TestUtils.addFileProperty(SHA256ServerSideChecksum)

    (1 to 7).foreach { _ => TestUtils.addFileMetadata(UUID.randomUUID().toString, fileOneId, SHA256ServerSideChecksum)}

    TestUtils.addFileMetadata(metadataId, fileTwoId, SHA256ServerSideChecksum)
    val fileMetadataFiles = fileMetadataRepository.countProcessedChecksumInConsignment(consignmentId).futureValue

    fileMetadataFiles shouldBe 2
  }

  "addFileMetadata" should "add metadata with the correct values" in {
    val db = DbConnection.db
    val fileMetadataRepository = new FileMetadataRepository(db)
    val consignmentId = UUID.fromString("d4c053c5-f83a-4547-aefe-878d496bc5d2")
    val fileId = UUID.fromString("ba176f90-f0fd-42ef-bb28-81ba3ffb6f05")
    addFileProperty("FileProperty")
    createConsignment(consignmentId, userId)
    createFile(fileId, consignmentId)
    val input = Seq(FilemetadataRow(UUID.randomUUID(), fileId, "value", Timestamp.from(Instant.now()), UUID.randomUUID(), "FileProperty"))
    val result = fileMetadataRepository.addFileMetadata(input).futureValue.head
    result.propertyname should equal("FileProperty")
    result.value should equal("value")
    checkFileMetadataExists(fileId, "FileProperty")
  }

  "addChecksumMetadata" should "update the checksum validation field on the file table" in {
    val db = DbConnection.db
    val fileMetadataRepository = new FileMetadataRepository(db)
    val consignmentId = UUID.fromString("f25fc436-12f1-48e8-8e1a-3fada106940a")
    val fileId = UUID.fromString("59ce7106-57f2-48ff-b451-4148e6bf74f9")
    createFile(fileId, consignmentId)
    addFileProperty("FileProperty")
    addFileMetadata(UUID.randomUUID().toString, fileId.toString, "FileProperty")
    createConsignment(consignmentId, userId)
    val input = FilemetadataRow(UUID.randomUUID(), fileId, "value", Timestamp.from(Instant.now()), UUID.randomUUID(), "FileProperty")
    fileMetadataRepository.addChecksumMetadata(input, Option(true)).futureValue
    getFileChecksumMatches(fileId) should equal(true)
  }

  "getFileMetadata" should "return the correct metadata" in {
    val db = DbConnection.db
    val fileMetadataRepository = new FileMetadataRepository(db)
    val consignmentId = UUID.fromString("d511ecee-89ac-4643-b62d-76a41984a92b")
    val fileId = UUID.fromString("4d5a5a00-77b4-4a97-aa3f-a75f7b13f284")
    createFile(fileId, consignmentId)
    addFileProperty("FileProperty")
    addFileMetadata(UUID.randomUUID().toString, fileId.toString, "FileProperty")
    createConsignment(consignmentId, userId)
    val response = fileMetadataRepository.getFileMetadata(fileId, "FileProperty").futureValue.head
    response.value should equal("Result of FileMetadata processing")
    response.propertyname should equal("FileProperty")
    response.fileid should equal(fileId)
  }
}
