package utils

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object TestUtils {
  implicit class AwaitFuture[T](future: Future[T]) {
    def await(timeout: Duration = 2.seconds): T = {
      Await.result(future, timeout)
    }
  }

}
