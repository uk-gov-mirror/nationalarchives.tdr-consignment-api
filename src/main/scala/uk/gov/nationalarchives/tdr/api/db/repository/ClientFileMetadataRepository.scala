package uk.gov.nationalarchives.tdr.api.db.repository

import java.util.UUID

import slick.jdbc.PostgresProfile.api._
import uk.gov.nationalarchives.Tables.{Clientfilemetadata, ClientfilemetadataRow}

import scala.concurrent.Future

class ClientFileMetadataRepository(db: Database) {
  private val insertQuery = Clientfilemetadata returning Clientfilemetadata.map(_.clientfilemetadataid) into
    ((clientFileMetadata, clientfilemetadataid) => clientFileMetadata.copy(clientfilemetadataid = clientfilemetadataid))

  def addClientFileMetadata(clientFileMetadataRows: Seq[ClientfilemetadataRow]): Future[Seq[ClientfilemetadataRow]] = {
    db.run(insertQuery ++= clientFileMetadataRows)
  }

  def getClientFileMetadata(fileId: UUID) = {
    val query = Clientfilemetadata.filter(_.fileid === fileId)
    db.run(query.result)
  }
}
