package service

import db.repository.SeriesRepository
import graphql.fields.SeriesFields.Series

import scala.concurrent.{ExecutionContext, Future}

class SeriesService(seriesRepository: SeriesRepository)(implicit val executionContext: ExecutionContext) {

  def getSeries(): Future[Seq[Series]] = {
    seriesRepository.getSeries().map(seriesRows =>
      seriesRows.map(s => Series(s.seriesid,s.bodyid, s.name, s.code, s.description)
      ))
    }
}
