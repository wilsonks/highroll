package game

package teenpatti

import java.util.concurrent.TimeUnit

import cats.data.ValidatedNel
import cats.effect.{Concurrent, Timer}
import cats.effect.syntax.concurrent._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.validated._
import deck.Card.Stock
import fs2.concurrent.Queue
import fs2.Stream
import game.teenpatti.Dealer._

import scala.concurrent.duration._

final case class Dealer(players: List[String], timeout: FiniteDuration, handLimit: Int) {

  import Table._
  import TableCommand._
  import TableEvent._

  def build[F[_]](boot: Table, input: Queue[F, TableCommand])(
    implicit F: Concurrent[F],
    T: Timer[F]): Stream[F, Table.Transition] =
    Stream
      .eval(step(boot, input))
      .flatMap(Stream.iterateEval(_) {
        case (last, _) => step(last, input)
      })

  def step[F[_]](state: Table, input: Queue[F, TableCommand])(
    implicit F: Concurrent[F],
    T: Timer[F]): F[Table.Transition] =
    load(state, input).map {
      case command if eval(command).isDefinedAt(state) =>
        eval(command)(state).fold(rejections => (state, CommandRejected(command, rejections.toList)), identity)
      case command =>
        (state, CommandRejected(command, Nil))
    }

  def load[F[_]](state: Table, input: Queue[F, TableCommand])(implicit F: Concurrent[F], T: Timer[F]): F[TableCommand] =
    state match {
      case Gaming(game) => remaining(game).flatMap(input.dequeue1.timeoutTo(_, F.pure(VoidGame(game))))
      case _            => input.dequeue1
    }

  def remaining[F[_]](game: GameId)(implicit F: Concurrent[F], T: Timer[F]): F[FiniteDuration] =
    T.clock.realTime(TimeUnit.MILLISECONDS).map(now => 0.max(game.time + timeout.toMillis - now).millis)

  def eval: TableCommand => PartialFunction[Table, Dealer.Validation[Table.Transition]] = {
    case CompleteGame(game) => {
      case Drawing(`game`, turn) =>
        NoWinner(turn).map(winners => (Opened, GameCompleted(game, winners)))
    }
    case AssignCard(game, card) => {
      case Drawing(`game`, head :: tail) =>
        HandLimitExceeded(head, card, handLimit).map(player =>
          (Drawing(game, tail ::: List(player)), CardAssigned(game, player.name, card)))
    }
    case CloseBetting(game) => {
      case Started(`game`) => (Drawing(game, players.map(Player(_))), BettingClosed(game)).validNel
    }
    case OpenBetting => {
      case Opened => GameId().validNel.map(game => (Started(game), BettingOpened(game)))
    }
    case OpenTable => {
      case Closed => (Opened, TableOpened).validNel
    }
    case CloseTable => {
      case Opened => (Closed, TableClosed).validNel
    }
  }
}

object Dealer {
  type Validation[A] = ValidatedNel[Rejection, A]

  sealed abstract class Rejection extends Product with Serializable

  final case object HandLimitExceeded extends Rejection {

    def apply(turn: List[Player], limit: Int): Validation[Unit] =
      if (turn.forall(_.cards.size == limit)) ().validNel else HandLimitExceeded.invalidNel

    def apply(player: Player, card: Stock, limit: Int): Validation[Player] =
      if (player.cards.size > limit) HandLimitExceeded.invalidNel
      else player.copy(cards = card :: player.cards).validNel
  }

  final case object NoWinner extends Rejection {

    def apply(turn: List[Player]): Validation[List[String]] =
      if (turn.isEmpty) NoWinner.invalidNel
      else
        turn
          .foldLeft(List.empty[Player]) {
            case (Nil, next) => List(next)
            case (winners, next) =>
              Player.score(next).compare(Player.score(winners.head)) match {
                case 0          => next :: winners
                case i if i > 0 => List(next)
                case _          => winners
              }
          }
          .map(_.name)
          .validNel
  }
}
