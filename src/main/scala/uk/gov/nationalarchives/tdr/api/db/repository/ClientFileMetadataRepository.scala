package uk.gov.nationalarchives.tdr.api.db.repository

import slick.jdbc.PostgresProfile.api._
import uk.gov.nationalarchives.Tables.{Clientfilemetadata, ClientfilemetadataRow}

import scala.concurrent.Future

class ClientFileMetadataRepository(db: Database) {
  private val insertQuery = Clientfilemetadata returning Clientfilemetadata.map(_.clientfilemetadataid) into
    ((clientFileMetadata, clientfilemetadataid) => clientFileMetadata.copy(clientfilemetadataid = clientfilemetadataid))

  def addClientFileMetadata(clientFileMetadataRow: ClientfilemetadataRow): Future[ClientfilemetadataRow] = {
    db.run(insertQuery += clientFileMetadataRow)
  }
}
