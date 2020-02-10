package uk.gov.nationalarchives.tdr.api.service

import uk.gov.nationalarchives.tdr.api.db.repository.SeriesRepository
import uk.gov.nationalarchives.tdr.api.graphql.fields.SeriesFields.Series

import scala.concurrent.{ExecutionContext, Future}

class SeriesService(seriesRepository: SeriesRepository)(implicit val executionContext: ExecutionContext) {

  def getSeries(bodyOption: Option[String]): Future[Seq[Series]] = {
    val series = if(bodyOption.isDefined) {
      seriesRepository.getSeries(bodyOption.get)
    } else {
      seriesRepository.getSeries()
    }
    series.map(seriesRows =>
      seriesRows.map(s => Series(s.seriesid.get,s.bodyid, s.name, s.code, s.description)
      ))
    }

  def addSeries(input: AddSeriesInput): Future[Series] = {
    val newSeries = SeriesRow(input.bodyid, input.code, input.name, input.description)

    seriesRepository.addSeries(newSeries).map(sr => Series(sr.bodyid, sr.code, sr.name, sr.description, sr.seriesid))
  }
}
