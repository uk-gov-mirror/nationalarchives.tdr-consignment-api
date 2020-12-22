package uk.gov.nationalarchives.tdr.api.db.repository

import java.sql.Timestamp
import java.util.UUID

import slick.jdbc.PostgresProfile
import uk.gov.nationalarchives.Tables.{Consignmentmetadata, _}
import slick.jdbc.PostgresProfile.api._
import uk.gov.nationalarchives.tdr.api.db.repository.ConsignmentMetadataRepository.ConsignmentMetadataRowWithName

import scala.concurrent.{ExecutionContext, Future}

class ConsignmentMetadataRepository(db: Database)(implicit val executionContext: ExecutionContext) {
  def sqlRowToConsignmentMetadata(propertyName: String, row: ConsignmentmetadataRow): ConsignmentMetadataRowWithName =
    ConsignmentMetadataRowWithName(propertyName, row.metadataid, row.consignmentid, row.value, row.datetime, row.userid)

  val insertQuery: PostgresProfile.IntoInsertActionComposer[ConsignmentmetadataRow, ConsignmentMetadataRowWithName] =
    Consignmentmetadata returning Consignmentmetadata.join(Consignmentproperty).on(_.propertyid === _.propertyid).map(_._2) into
      ((consignmentMetadata, consignmentproperty) => sqlRowToConsignmentMetadata(consignmentproperty.name.get, consignmentMetadata))

  def addConsignmentMetadata(rows: Seq[ConsignmentMetadataRowWithName]): Future[Seq[ConsignmentMetadataRowWithName]] = {
    db.run(DBIO.seq(rows.map(consignmentMetadataRowAction): _*).transactionally).map(_ => rows)
  }

  private def consignmentMetadataRowAction(row: ConsignmentMetadataRowWithName) = {
    for {
      property <- Consignmentproperty.filter(_.name === row.propertyName).result.head
      r <- Consignmentmetadata += ConsignmentmetadataRow(row.metadataid, row.consignmentid, Some(property.propertyid), row.value, row.datetime, row.userid)
    } yield row
  }

  def getConsignmentMetadata(consignmentId: UUID, propertyName: String*): Future[Seq[ConsignmentMetadataRowWithName]] = {
    val query = Consignmentmetadata.join(Consignmentproperty)
      .on(_.propertyid === _.propertyid)
      .filter(_._1.consignmentid === consignmentId)
      .filter(_._2.name inSet propertyName.toSet)
      .map(f => f._2.name.get -> f._1)
    db.run(query.result).map(_.map(f => sqlRowToConsignmentMetadata(f._1, f._2)))
  }
}

object ConsignmentMetadataRepository {

  case class ConsignmentMetadataRowWithName(propertyName: String,
                                            metadataid: UUID,
                                            consignmentid: Option[UUID],
                                            value: Option[String],
                                            datetime: Timestamp,
                                            userid: UUID)
}
