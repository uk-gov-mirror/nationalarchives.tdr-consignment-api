package uk.gov.nationalarchives.tdr.api.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.Materializer
import com.typesafe.scalalogging.Logger
import uk.gov.nationalarchives.tdr.api.http.Routes.route

import scala.concurrent.Await
import scala.language.postfixOps

object ApiServer extends App {

  val PORT = 8080
  val logger = Logger("ApiServer")

  implicit val actorSystem: ActorSystem = ActorSystem("graphql-server")
  implicit val materializer: Materializer = Materializer(actorSystem)

  import scala.concurrent.duration._

  scala.sys.addShutdownHook(() -> shutdown())

  Http().bindAndHandle(route, "0.0.0.0", PORT)
  logger.info(s"open a browser with URL: http://localhost:$PORT")


  def shutdown(): Unit = {
    actorSystem.terminate()
    Await.result(actorSystem.whenTerminated, 30 seconds)
  }
}
