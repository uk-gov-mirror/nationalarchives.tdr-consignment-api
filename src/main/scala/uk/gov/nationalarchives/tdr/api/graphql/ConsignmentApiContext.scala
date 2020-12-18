package uk.gov.nationalarchives.tdr.api.graphql
import uk.gov.nationalarchives.tdr.api.service._
import uk.gov.nationalarchives.tdr.keycloak.Token

case class ConsignmentApiContext(accessToken: Token,
                                 antivirusMetadataService: AntivirusMetadataService,
                                 clientFileMetadataService: ClientFileMetadataService,
                                 consignmentMetadataService: ConsignmentMetadataService,
                                 consignmentService: ConsignmentService,
                                 ffidMetadataService: FFIDMetadataService,
                                 fileMetadataService: FileMetadataService,
                                 fileService: FileService,
                                 seriesService: SeriesService,
                                 transferAgreementService: TransferAgreementService,
                                 transferringBodyService: TransferringBodyService,
                                 )
