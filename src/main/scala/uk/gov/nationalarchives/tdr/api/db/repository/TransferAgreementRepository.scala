package uk.gov.nationalarchives.tdr.api.db.repository

import java.util.UUID

import slick.jdbc.PostgresProfile.api._
import uk.gov.nationalarchives.Tables.{Transferagreement, TransferagreementRow}

import scala.concurrent.Future

class TransferAgreementRepository(db: Database) {
  private val insertQuery = Transferagreement returning Transferagreement.map(_.transferagreementid) into
    ((transferagreement, transferagreementid) => transferagreement.copy(transferagreementid = transferagreementid))

  def getTransferAgreement(consignmentId: UUID): Future[Seq[TransferagreementRow]] = {
    val query = Transferagreement.filter(_.consignmentid === consignmentId)
    db.run(query.result)
  }

  def addTransferAgreement(transferAgreementRow: TransferagreementRow): Future[TransferagreementRow] = {
    db.run(insertQuery += transferAgreementRow)
  }
}
