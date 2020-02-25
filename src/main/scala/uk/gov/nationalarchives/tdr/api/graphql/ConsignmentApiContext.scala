package uk.gov.nationalarchives.tdr.api.graphql

import org.keycloak.representations.AccessToken
import uk.gov.nationalarchives.tdr.api.service.{SeriesService, TransferAgreementService}

case class ConsignmentApiContext(accessToken: AccessToken, seriesService: SeriesService, transferAgreementService: TransferAgreementService)
