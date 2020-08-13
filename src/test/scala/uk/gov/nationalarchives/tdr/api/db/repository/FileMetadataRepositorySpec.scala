package uk.gov.nationalarchives.tdr.api.db.repository

import java.util.UUID

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.tdr.api.db.DbConnection
import uk.gov.nationalarchives.tdr.api.utils.TestUtils
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._


class FileMetadataRepositorySpec extends AnyFlatSpec with ScalaFutures with Matchers {

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
    val propertyId = "7a1b272c-e2f7-4b8f-8291-5e9dc312edb7"
    val metadataOneId = "e4440f43-20c6-4b6c-811d-349e633617e4"
    val metadataTwoId = "7e5abff1-1d86-4d7d-8ab4-c6a2934ec611"
    val metadataThreeId = "efbd5da6-909a-45b1-964f-87a3baf48816"

//  Need to create a consignment with files in it
    TestUtils.createConsignment(consignmentOne, userId)
    TestUtils.createConsignment(consignmentTwo, userId)
    TestUtils.createFile(UUID.fromString(fileOneId), consignmentOne)
    TestUtils.createFile(UUID.fromString(fileTwoId), consignmentOne)
    TestUtils.createFile(UUID.fromString(fileThreeId), consignmentTwo)

//  Then need to add data to the FileMetadata repository for these files
    TestUtils.addFileMetadata(metadataOneId, fileOneId, propertyId)
    TestUtils.addFileMetadata(metadataTwoId, fileTwoId, propertyId)
    TestUtils.addFileMetadata(metadataThreeId, fileThreeId, propertyId)

    TestUtils.addFileProperty(propertyId, "SHA256ServerSideChecksum")

    val fileMetadataFiles = fileMetadataRepository.countProcessedChecksumInConsignment(consignmentOne).futureValue

    fileMetadataFiles shouldBe 2
  }

  "countProcessedChecksumInConsignment" should "return number of fileMetadata rows with repetitive data filtered out" in {
    val db = DbConnection.db
    val fileMetadataRepository = new FileMetadataRepository(db)
    val consignmentId = UUID.fromString("c6f78fef-704a-46a8-82c0-afa465199e66")
    val fileOneId = "20e0676a-f0a1-4051-9540-e7df1344ac11"
    val fileTwoId = "b5111f11-4dca-4f92-8239-505da567b9d0"
    val propertyId = "7a1b272c-e2f7-4b8f-8291-5e9dc312edb7"
    val metadataId = "f4440f43-20c6-4b6c-811d-349e633617e5"

    TestUtils.createConsignment(consignmentId, userId)
    TestUtils.createFile(UUID.fromString(fileOneId), consignmentId)
    TestUtils.createFile(UUID.fromString(fileTwoId), consignmentId)

    (1 to 7).foreach { _ => TestUtils.addFileMetadata(UUID.randomUUID().toString, fileOneId, propertyId)}

    TestUtils.addFileMetadata(metadataId, fileTwoId, propertyId)
    val fileMetadataFiles = fileMetadataRepository.countProcessedChecksumInConsignment(consignmentId).futureValue

    fileMetadataFiles shouldBe 2
  }
}
