package game

package teenpatti

import cats.effect.{IO, SyncIO}
import deck.Card
import fs2.Stream
import fs2.concurrent.Queue
import fxml.Showcase
import fxml.io.driver._
import fxml.io.{Display, DisplayApp}
import javafx.stage.Stage

import scala.io.StdIn

object DealerTerminal extends DisplayApp {

  def load(args: List[String])(implicit stage: Stage): SyncIO[Unit] =
    for {
      q <- sync(Queue.bounded[IO, TableCommand](1))
    } yield
      Display("dealer.fxml")
        .copy(showcase = Showcase(screen = 1))
        .portal
        .bind(table(q).driver)
        .bind(shoe.reader)
        .bind(q.writer)
        .show

  def table(q: Queue[IO, TableCommand]): Stream[IO, Table.Transition] =
    factory.build(Table.Opened(0), q)

  def factory = TableFactory(List("LION", "TIGER", "DRAGON"), 3)

  def shoe: Stream[IO, Card.Stock] =
    Stream
      .repeatEval(IO(StdIn.readLine()))
      .takeWhile(_.nonEmpty)
      .map(s => Card.Stock.values.find(_.toString == s))
      .collect {
        case Some(s) => Card.Stock(s)
      }
}
