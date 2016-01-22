package reactive

import java.util.concurrent.TimeUnit

import akka.actor.Cancellable
import akka.stream.scaladsl.Source

import scala.concurrent.duration.FiniteDuration

object SlowNumerable {
  def source: (Source[Int, Cancellable]) forSome {type source >: Source[Unit, Cancellable] <: Source[Unit, Cancellable]} = {
    val source = Source.tick(FiniteDuration(100, TimeUnit.MILLISECONDS), FiniteDuration(900, TimeUnit.MILLISECONDS), ())
    val it = Iterator.from(0)
    source.map(_ => it.next())
  }
}
