package uk.gov.nationalarchives.tdr.api.graphql
import uk.gov.nationalarchives.tdr.api.service._
import uk.gov.nationalarchives.tdr.keycloak.Token

case class ConsignmentApiContext(accessToken: Token,
                                 clientFileMetadataService: ClientFileMetadataService,
                                 consignmentService: ConsignmentService,
                                 fileService: FileService,
                                 seriesService: SeriesService,
                                 transferAgreementService: TransferAgreementService,
                                 transferringBodyService: TransferringBodyService)
