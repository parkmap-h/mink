package reactive

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.http.scaladsl.server.Directives._

import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.io.StdIn


object Application extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher


  val flowLocal = Flow[Int].map(_ * 2)
  // 別のWebServiceにおきかえたい
  //  val flowWeb = ???

  val route =
    path(IntNumber) { n =>
      get {
        complete {
          val result = Source.single(n).via(flowLocal).runWith(Sink.head)
          // TODO 結果が変える前に処理おえたい。値は別の方法で返す。今仕方ないので、Awaitする
          Await.result(result, Duration.Inf).toString
        }
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 3000)

  println(s"Server online at http://localhost:3000/\nPress RETURN to stop...")
  StdIn.readLine() // for the future transformations
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ ⇒ system.shutdown()) // and shutdown when done

}