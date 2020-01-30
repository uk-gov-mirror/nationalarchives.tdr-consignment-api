package service

import db.repository.SeriesRepository
import graphql.fields.SeriesFields
import org.mockito.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.Tables.SeriesRow
import utils.TestUtils._

import scala.concurrent.{ExecutionContext, Future}

class SeriesServiceSpec extends AnyFlatSpec with MockitoSugar with Matchers {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  "getSeries" should "return the correct series object" in {
    val series = SeriesRow(1, Option.apply(2), Option.apply("name"), Option.apply("code"), Option.apply("description"))
    val mockResponse: Future[Seq[SeriesRow]] = Future.successful(Seq(series))

    val repoMock = mock[SeriesRepository]
    when(repoMock.getSeries()).thenReturn(mockResponse)

    val seriesService: SeriesService = new SeriesService(repoMock)
    val seriesResponse: Seq[SeriesFields.Series] = seriesService.getSeries().await()

    verify(repoMock, times(1)).getSeries()
    seriesResponse.length should equal(1)
    seriesResponse.head.seriesid should equal(1)
    seriesResponse.head.bodyid.get should equal(2)
    seriesResponse.head.name.get should equal("name")
    seriesResponse.head.code.get should equal("code")
    seriesResponse.head.description.get should equal("description")
  }
}
