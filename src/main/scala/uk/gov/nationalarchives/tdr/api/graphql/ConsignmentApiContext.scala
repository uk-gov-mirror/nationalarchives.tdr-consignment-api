package uk.gov.nationalarchives.tdr.api.graphql

import org.keycloak.representations.AccessToken
import uk.gov.nationalarchives.tdr.api.service.{TransferAgreementService, ConsignmentService, SeriesService}

case class ConsignmentApiContext(accessToken: AccessToken, seriesService: SeriesService, consignmentService: ConsignmentService, transferAgreementService: TransferAgreementService)
