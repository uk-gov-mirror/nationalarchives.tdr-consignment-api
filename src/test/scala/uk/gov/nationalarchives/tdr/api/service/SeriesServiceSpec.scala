package uk.gov.nationalarchives.tdr.api.service

import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.Tables.SeriesRow
import uk.gov.nationalarchives.tdr.api.db.repository.SeriesRepository
import uk.gov.nationalarchives.tdr.api.graphql.fields.SeriesFields
import uk.gov.nationalarchives.tdr.api.graphql.fields.SeriesFields.AddSeriesInput
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._

import scala.concurrent.{ExecutionContext, Future}

class SeriesServiceSpec extends AnyFlatSpec with MockitoSugar with Matchers {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  "getSeries" should "return the correct series object" in {
    val series = SeriesRow(Option.apply(2), Option.apply("name"), Option.apply("code"), Option.apply("description"), Option.apply(1))
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

  "addSeries" should "insert series and return inserted series object" in {
    val seriesId: Long = 123456789
    val bodyId: Long = 987654321
    val seriesDescription: String = "Series Description"
    val seriesName: String = "Series Name"
    val seriesCode: String = "Series Code"

    val mockSeriesRow = SeriesRow(Option.apply(bodyId), Option.apply(seriesCode), Option.apply(seriesName),
      Option.apply(seriesDescription), Option.apply(seriesId))
    val mockResponse: Future[SeriesRow] = Future.successful(mockSeriesRow)

    val repoMock = mock[SeriesRepository]
    when(repoMock.addSeries(any)).thenReturn(mockResponse)

    val newSeriesInput = new AddSeriesInput(
      Option.apply(bodyId),
      Option.apply(seriesCode),
      Option.apply(seriesName),
      Option.apply(seriesDescription))

    val seriesService: SeriesService = new SeriesService(repoMock)
    val seriesResponse: SeriesFields.Series = seriesService.addSeries(newSeriesInput).await()

    verify(repoMock, times(1)).addSeries(any())
    seriesResponse.seriesid should equal(seriesId)
    seriesResponse.bodyid.get should equal(bodyId)
    seriesResponse.code.get should equal(seriesCode)
    seriesResponse.name.get should equal(seriesName)
    seriesResponse.description.get should equal(seriesDescription)
  }
}
