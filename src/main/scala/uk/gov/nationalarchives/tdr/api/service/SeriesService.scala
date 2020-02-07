package uk.gov.nationalarchives.tdr.api.service

import uk.gov.nationalarchives.Tables.SeriesRow
import uk.gov.nationalarchives.tdr.api.db.repository.SeriesRepository
import uk.gov.nationalarchives.tdr.api.graphql.fields.SeriesFields.{AddSeriesInput, Series}

import scala.concurrent.{ExecutionContext, Future}

class SeriesService(seriesRepository: SeriesRepository)(implicit val executionContext: ExecutionContext) {

  def getSeries(): Future[Seq[Series]] = {
    seriesRepository.getSeries().map(seriesRows =>
      seriesRows.map(s => Series(s.bodyid, s.name, s.code, s.description, s.seriesid)
      ))
    }

  def addSeries(input: AddSeriesInput): Future[Series] = {
    val newSeries = SeriesRow(input.bodyid, input.code, input.name, input.description)

    seriesRepository.addSeries(newSeries).map(sr => Series(sr.bodyid, sr.code, sr.name, sr.description, sr.seriesid))
  }
}
