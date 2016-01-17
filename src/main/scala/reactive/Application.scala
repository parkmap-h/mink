package reactive

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpResponse, HttpRequest }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.http.scaladsl.server.Directives._

import scala.io.StdIn
import scala.util.Try


object Application extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher


  val flowLocal: Flow[Int, Int, Unit] = Flow[Int].map(_ * 2)
  // 別のWebServiceにおきかえたい
  val flowWeb = {
    val requestFlow: Flow[Int, (HttpRequest, Int), Unit] = Flow[Int].map(n => HttpRequest(uri = s"/$n") -> 42) // 42 はコネクション識別用のキー
    val poolClientFlow = Http().cachedHostConnectionPool[Int]("localhost", 3001)
    val valueFlow = Flow[(Try[HttpResponse], Int)].flatMapConcat(n => n._1.get.entity.dataBytes.map(_.utf8String))
    requestFlow.via(poolClientFlow).via(valueFlow)
  }


  val route =
    path(IntNumber) { n =>
      get {
        complete {
          // val result = Source.single(n).via(flowLocal).runWith(Sink.head)
          val result = Source.single(n).via(flowWeb).runWith(Sink.head)
          // Await.result(result, Duration.Inf).toString // 同期的にする場合はこちら
          result.onComplete(n => println(s"response: $n"))
          "Success"
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