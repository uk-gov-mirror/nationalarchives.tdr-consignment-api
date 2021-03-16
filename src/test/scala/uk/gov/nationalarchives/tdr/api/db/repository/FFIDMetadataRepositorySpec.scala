package uk.gov.nationalarchives.tdr.api.db.repository

import java.util.UUID

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.tdr.api.db.DbConnection
import uk.gov.nationalarchives.tdr.api.utils.{TestDatabase, TestUtils}
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._


class FFIDMetadataRepositorySpec extends AnyFlatSpec with TestDatabase with ScalaFutures with Matchers {

  "countProcessedFfidMetadata" should "return 0 if consignment has no files" in {
    val db = DbConnection.db
    val ffidMetadataRepository = new FFIDMetadataRepository(db)
    val consignmentId = UUID.fromString("40c26c38-b6b4-4325-9d28-c9d0b50e89aa")

    TestUtils.createConsignment(consignmentId, userId)

    val ffidMetadataFiles = ffidMetadataRepository.countProcessedFfidMetadata(consignmentId).futureValue

    ffidMetadataFiles shouldBe 0
  }

  "countProcessedFfidMetadata" should "return 0 if consignment has no FFID metadata for files" in {
    val db = DbConnection.db
    val ffidMetadataRepository = new FFIDMetadataRepository(db)
    val consignmentId = UUID.fromString("95dc7b75-e88b-41b4-ac65-004c61874145")
    val fileOneId = UUID.fromString("d74650ff-21b1-402d-8c59-b114698a8341")
    val fileTwoId = UUID.fromString("51c55218-1322-4453-9ef8-2300ef1c0fef")
    val fileThreeId = UUID.fromString("7076f399-b596-4161-a95d-e686c6435710")

    TestUtils.createConsignment(consignmentId, userId)
    TestUtils.createFile(fileOneId, consignmentId)
    TestUtils.createFile(fileTwoId, consignmentId)
    TestUtils.createFile(fileThreeId, consignmentId)

    val ffidMetadataFiles = ffidMetadataRepository.countProcessedFfidMetadata(consignmentId).futureValue

    ffidMetadataFiles shouldBe 0
  }

  "countProcessedFfidMetadata" should "return the number of FFIDMetadata for files in a specified consignment" in {
    val db = DbConnection.db
    val ffidMetadataRepository = new FFIDMetadataRepository(db)
    val consignmentOne = UUID.fromString("a9ccec45-5325-4e07-a0cd-1b0f4dc0d6fd")
    val consignmentTwo = UUID.fromString("c50d3aff-1d06-4c94-9960-e7b25a882086")
    val fileOneId = "d734da24-90a7-4c05-ab37-a78746556323"
    val fileTwoId = "be77573a-8710-42a2-9a9f-522bd681d467"
    val fileThreeId = "8efd6bfb-f079-4029-9d62-2cc0f4ebe501"

    TestUtils.createConsignment(consignmentOne, userId)
    TestUtils.createConsignment(consignmentTwo, userId)

    TestUtils.createFile(UUID.fromString(fileOneId), consignmentOne)
    TestUtils.createFile(UUID.fromString(fileTwoId), consignmentOne)
    TestUtils.createFile(UUID.fromString(fileThreeId), consignmentTwo)

    TestUtils.addFFIDMetadata(fileOneId)
    TestUtils.addFFIDMetadata(fileTwoId)
    TestUtils.addFFIDMetadata(fileThreeId)

    val fileMetadataFiles = ffidMetadataRepository.countProcessedFfidMetadata(consignmentOne).futureValue

    fileMetadataFiles shouldBe 2
  }

  "countProcessedFfidMetadata" should "return number of ffidMetadata rows with repetitive data filtered out" in {
    val db = DbConnection.db
    val ffidMetadataRepository = new FFIDMetadataRepository(db)
    val consignmentId = UUID.fromString("21061d4d-ed73-485f-b433-c48b0868fffb")

    val fileOneId = "50f4f290-cdcc-4a0f-bd26-3bb40f320b71"
    val fileTwoId = "7f55565e-bfa5-4cf9-9e02-7e75ff033b3b"
    val fileThreeId = "89b6183d-e761-4af9-9e37-2ffa09922dba"
    val fileFourId = "939999e5-c082-4052-a99a-6b45092d826f"

    TestUtils.createConsignment(consignmentId, userId)
    TestUtils.createFile(UUID.fromString(fileOneId), consignmentId)
    TestUtils.createFile(UUID.fromString(fileTwoId), consignmentId)
    TestUtils.createFile(UUID.fromString(fileThreeId), consignmentId)
    TestUtils.createFile(UUID.fromString(fileFourId), consignmentId)

    (1 to 7).foreach { _ => TestUtils.addFFIDMetadata(fileOneId) }

    TestUtils.addFFIDMetadata(fileTwoId)
    TestUtils.addFFIDMetadata(fileThreeId)

    val ffidMetadataFiles = ffidMetadataRepository.countProcessedFfidMetadata(consignmentId).futureValue

    ffidMetadataFiles shouldBe 3
  }

  "getFFIDMetadata" should "return the same number of ffidMetadata rows as the number of files added" in {
    val db = DbConnection.db
    val ffidMetadataRepository = new FFIDMetadataRepository(db)
    val consignmentId = UUID.fromString("21061d4d-ed73-485f-b433-c48b0868fffb")

    val fileOneId = "50f4f290-cdcc-4a0f-bd26-3bb40f320b71"
    val fileTwoId = "7f55565e-bfa5-4cf9-9e02-7e75ff033b3b"
    val fileThreeId = "89b6183d-e761-4af9-9e37-2ffa09922dba"
    val fileFourId = "939999e5-c082-4052-a99a-6b45092d826f"

    TestUtils.createConsignment(consignmentId, userId)
    TestUtils.createFile(UUID.fromString(fileOneId), consignmentId)
    TestUtils.createFile(UUID.fromString(fileTwoId), consignmentId)
    TestUtils.createFile(UUID.fromString(fileThreeId), consignmentId)
    TestUtils.createFile(UUID.fromString(fileFourId), consignmentId)

    TestUtils.addFFIDMetadata(fileOneId)
    TestUtils.addFFIDMetadata(fileTwoId)
    TestUtils.addFFIDMetadata(fileThreeId)

    val ffidMetadataRows = ffidMetadataRepository.getFFIDMetadata(consignmentId).futureValue

    ffidMetadataRows.length shouldBe 3
  }
  "getFFIDMetadata" should "return the fileIds of the files that had ffidMetadata added to them" in {
    val db = DbConnection.db
    val ffidMetadataRepository = new FFIDMetadataRepository(db)
    val consignmentId = UUID.fromString("21061d4d-ed73-485f-b433-c48b0868fffb")

    val fileOneId = "50f4f290-cdcc-4a0f-bd26-3bb40f320b71"
    val fileTwoId = "7f55565e-bfa5-4cf9-9e02-7e75ff033b3b"
    val fileThreeId = "89b6183d-e761-4af9-9e37-2ffa09922dba"
    val fileFourId = "939999e5-c082-4052-a99a-6b45092d826f"

    TestUtils.createConsignment(consignmentId, userId)
    TestUtils.createFile(UUID.fromString(fileOneId), consignmentId)
    TestUtils.createFile(UUID.fromString(fileTwoId), consignmentId)
    TestUtils.createFile(UUID.fromString(fileThreeId), consignmentId)
    TestUtils.createFile(UUID.fromString(fileFourId), consignmentId)

    TestUtils.addFFIDMetadata(fileOneId)
    TestUtils.addFFIDMetadata(fileTwoId)
    TestUtils.addFFIDMetadata(fileThreeId)

    val ffidMetadataRows = ffidMetadataRepository.getFFIDMetadata(consignmentId).futureValue
    val fileIds: Set[UUID] = ffidMetadataRows.toMap.keySet

    fileIds.contains(UUID.fromString(fileOneId)) should equal(true)
    fileIds.contains(UUID.fromString(fileTwoId)) should equal(true)
    fileIds.contains(UUID.fromString(fileThreeId)) should equal(true)
    fileIds.contains(UUID.fromString(fileFourId)) should equal(false)
  }

  "getFFIDMetadata" should "return ffidMetadata and ffidMetadataMatches that have matching fileMetadataIds" in {
    val db = DbConnection.db
    val ffidMetadataRepository = new FFIDMetadataRepository(db)
    val consignmentId = UUID.fromString("21061d4d-ed73-485f-b433-c48b0868fffb")

    val fileOneId = "50f4f290-cdcc-4a0f-bd26-3bb40f320b71"
    val fileTwoId = "7f55565e-bfa5-4cf9-9e02-7e75ff033b3b"
    val fileThreeId = "89b6183d-e761-4af9-9e37-2ffa09922dba"
    val fileFourId = "939999e5-c082-4052-a99a-6b45092d826f"

    TestUtils.createConsignment(consignmentId, userId)
    TestUtils.createFile(UUID.fromString(fileOneId), consignmentId)
    TestUtils.createFile(UUID.fromString(fileTwoId), consignmentId)
    TestUtils.createFile(UUID.fromString(fileThreeId), consignmentId)
    TestUtils.createFile(UUID.fromString(fileFourId), consignmentId)

    TestUtils.addFFIDMetadata(fileOneId)
    TestUtils.addFFIDMetadata(fileTwoId)
    TestUtils.addFFIDMetadata(fileThreeId)

    val ffidMetadataRows = ffidMetadataRepository.getFFIDMetadata(consignmentId).futureValue

    ffidMetadataRows.toMap.values.foreach(
      ffidMetadataAndMatches =>
        ffidMetadataAndMatches._1.ffidmetadataid == ffidMetadataAndMatches._2.ffidmetadataid should equal(true)
    )
  }
}
