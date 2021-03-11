package uk.gov.nationalarchives.tdr.api.db

import akka.stream.alpakka.slick.scaladsl.SlickSession
import com.typesafe.config.ConfigFactory
import scalacache.CacheConfig
import scalacache.caffeine.CaffeineCache
import scalacache.memoization._
import scalacache.modes.try_._
import slick.jdbc.JdbcBackend
import slick.jdbc.hikaricp.HikariCPJdbcDataSource
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.rds.RdsUtilities
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object DbConnection {
  implicit val passwordCache: CaffeineCache[String] = CaffeineCache[String](CacheConfig())
  val slickSession: SlickSession = SlickSession.forConfig("consignmentapi")

  def db: JdbcBackend#DatabaseDef = {
    val db = slickSession.db
    db.source match {
      case hikariDataSource: HikariCPJdbcDataSource =>
        val configBean = hikariDataSource.ds.getHikariConfigMXBean
        getPassword match {
          case Failure(exception) => throw exception
          case Success(password) =>
            configBean.setPassword(password)
            db
        }
      case _ =>
        db
    }
  }

  def getPassword: Try[String] = memoize[Try, String](Some(60.seconds)) {
    val configFactory = ConfigFactory.load
    val useIamAuth = configFactory.getBoolean("consignmentapi.useIamAuth")
    if (useIamAuth) {
      val rdsClient = RdsUtilities.builder().region(Region.EU_WEST_2).build()
      val port = 5432
      val request = GenerateAuthenticationTokenRequest.builder()
        .credentialsProvider(DefaultCredentialsProvider.builder().build())
        .hostname(sys.env("DB_ADDR"))
        .port(port)
        .username("api_iam")
        .region(Region.EU_WEST_2)
        .build()
      rdsClient.generateAuthenticationToken(request)
    } else {
      configFactory.getString("consignmentapi.db.password")
    }
  }
}
