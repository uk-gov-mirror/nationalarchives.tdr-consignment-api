package uk.gov.nationalarchives.tdr.api.graphql
import uk.gov.nationalarchives.tdr.keycloak.Token
import uk.gov.nationalarchives.tdr.api.service.{ConsignmentService, SeriesService}

case class ConsignmentApiContext(accessToken: Token, seriesService: SeriesService, consignmentService: ConsignmentService)
