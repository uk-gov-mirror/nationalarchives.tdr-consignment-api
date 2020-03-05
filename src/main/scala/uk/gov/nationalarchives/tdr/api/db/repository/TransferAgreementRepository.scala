package uk.gov.nationalarchives.tdr.api.db.repository

import slick.jdbc.MySQLProfile.api._
import uk.gov.nationalarchives.Tables.{Transferagreement, TransferagreementRow}

import scala.concurrent.Future

class TransferAgreementRepository(db: Database) {
  private val insertQuery = Transferagreement returning Transferagreement.map(_.transferagreementid) into
    ((transferagreement, transferagreementid) => transferagreement.copy(transferagreementid = Some(transferagreementid)))

  def addTransferAgreement(transferAgreementRow: TransferagreementRow): Future[TransferagreementRow] = {
    db.run(insertQuery += transferAgreementRow)
  }
}
