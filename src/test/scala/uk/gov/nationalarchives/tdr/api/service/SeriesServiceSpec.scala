package uk.gov.nationalarchives.tdr.api.service

import java.util.UUID

import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.mockito.ArgumentMatchers._
import uk.gov.nationalarchives.Tables.SeriesRow
import uk.gov.nationalarchives.tdr.api.db.repository.SeriesRepository
import uk.gov.nationalarchives.tdr.api.graphql.fields.SeriesFields
import uk.gov.nationalarchives.tdr.api.graphql.fields.SeriesFields.AddSeriesInput
import uk.gov.nationalarchives.tdr.api.utils.FixedUUIDSource
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._

import scala.concurrent.{ExecutionContext, Future}

class SeriesServiceSpec extends AnyFlatSpec with MockitoSugar with Matchers {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  "getSeries" should "return all series if no argument is provided" in {
    val fixedUuidSource = new FixedUUIDSource()
    val repoMock = setupSeriesResponses
    val seriesService: SeriesService = new SeriesService(repoMock, fixedUuidSource)
    val seriesResponse: Seq[SeriesFields.Series] = seriesService.getSeries(Option.empty).await()

    verify(repoMock, times(1)).getSeries()
    verify(repoMock, times(0)).getSeries(anyString())
    seriesResponse.length should equal(2)
    checkFields(seriesResponse.head, SeriesCheck(fixedUuidSource.uuid, fixedUuidSource.uuid, "name1", "code1", "description1"))
    checkFields(seriesResponse.tail.head, SeriesCheck(fixedUuidSource.uuid, fixedUuidSource.uuid, "name2", "code2", "description2"))
  }

  "getSeries" should "return the specfic series for a body if one is provided" in {
    val fixedUuidSource = new FixedUUIDSource()
    val repoMock = setupSeriesResponses

    val seriesService: SeriesService = new SeriesService(repoMock, fixedUuidSource)
    val seriesResponse: Seq[SeriesFields.Series] = seriesService.getSeries(Option("1")).await()

    verify(repoMock, times(0)).getSeries()
    verify(repoMock, times(1)).getSeries(anyString())
    seriesResponse.length should equal(1)
    checkFields(seriesResponse.head, SeriesCheck(fixedUuidSource.uuid, fixedUuidSource.uuid, "name1", "code1", "description1"))
  }

  "addSeries" should "insert series and return inserted series object" in {
    val fixedUuidSource = new FixedUUIDSource()
    val seriesId = UUID.randomUUID()
    val bodyId = UUID.randomUUID()
    val seriesDescription: String = "Series Description"
    val seriesName: String = "Series Name"
    val seriesCode: String = "Series Code"

    val mockSeriesRow = SeriesRow(seriesId, bodyId, Option.apply(seriesCode), Option.apply(seriesName),
      Option.apply(seriesDescription))
    val mockResponse: Future[SeriesRow] = Future.successful(mockSeriesRow)

    val repoMock = mock[SeriesRepository]
    when(repoMock.addSeries(any)).thenReturn(mockResponse)

    val newSeriesInput = AddSeriesInput(
      bodyId,
      Option.apply(seriesCode),
      Option.apply(seriesName),
      Option.apply(seriesDescription))

    val seriesService: SeriesService = new SeriesService(repoMock, fixedUuidSource)
    val seriesResponse: SeriesFields.Series = seriesService.addSeries(newSeriesInput).await()

    verify(repoMock, times(1)).addSeries(any())
    checkFields(seriesResponse, SeriesCheck(seriesId, bodyId, seriesName, seriesCode, seriesDescription))
  }

  case class SeriesCheck(seriesId: UUID, bodyId: UUID, name: String, code: String, description: String)

  private def checkFields(series: SeriesFields.Series, seriesCheck: SeriesCheck) = {
    series.seriesid should equal(seriesCheck.seriesId)
    series.bodyid should equal(seriesCheck.bodyId)
    series.name.get should equal(seriesCheck.name)
    series.code.get should equal(seriesCheck.code)
    series.description.get should equal(seriesCheck.description)
  }

  private def setupSeriesResponses = {
    val fixedUuidSource = new FixedUUIDSource()
    val seriesOne = SeriesRow(fixedUuidSource.uuid, fixedUuidSource.uuid, Option.apply("name1"), Option.apply("code1"), Option.apply("description1"))
    val seriesTwo = SeriesRow(fixedUuidSource.uuid, fixedUuidSource.uuid, Option.apply("name2"), Option.apply("code2"), Option.apply("description2"))
    val mockResponseAll: Future[Seq[SeriesRow]] = Future.successful(Seq(seriesOne, seriesTwo))
    val mockResponseOne: Future[Seq[SeriesRow]] = Future.successful(Seq(seriesOne))

    val repoMock = mock[SeriesRepository]
    when(repoMock.getSeries()).thenReturn(mockResponseAll)
    when(repoMock.getSeries(anyString())).thenReturn(mockResponseOne)
    repoMock
  }
}
