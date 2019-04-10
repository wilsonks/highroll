package game

package teenpatti

import cats.Functor
import cats.data.ValidatedNel
import cats.syntax.functor._
import cats.syntax.validated._
import deck.Card.Stock
import fs2.Stream
import fs2.concurrent.Queue
import game.teenpatti.TableFactory._

final case class TableFactory(players: List[String], handLimit: Int) {

  import Table._
  import TableCommand._
  import TableEvent._

  def build[F[_]](boot: Table, input: Queue[F, TableCommand])(implicit F: Functor[F]): Stream[F, Table.Transition] =
    Stream
      .eval(step(boot, input))
      .flatMap(Stream.iterateEval(_) {
        case (last, _) => step(last, input)
      })

  def step[F[_]](state: Table, input: Queue[F, TableCommand])(implicit F: Functor[F]): F[Table.Transition] =
    input.dequeue1.map {
      case command if eval(command).isDefinedAt(state) =>
        eval(command)(state).fold(rejections => (state, CommandRejected(command, rejections.toList)), identity)
      case command =>
        (state, CommandRejected(command, Nil))
    }

  def eval: TableCommand => PartialFunction[Table, TableFactory.Validation[Table.Transition]] = {
    case CompleteGame(game) => {
      case Drawing(`game`, turn) =>
        NoWinner(turn).map(winners => (Opened(game), GameCompleted(game, winners)))
    }
    case AssignCard(game, card) => {
      case Drawing(`game`, head :: tail) =>
        HandLimitExceeded(head, card, handLimit).map(player =>
          (Drawing(game, tail ::: List(player)), CardAssigned(game, player.name, card)))
    }
    case StartDraw(game) => {
      case Started(`game`) => (Drawing(game, players.map(Player(_))), DrawStarted(game)).validNel
    }
    case VoidGame(game) => {
      case Dealing(`game`) => (Opened(game), GameVoided(game)).validNel
    }
    case StartGame => {
      case Opened(game) =>
        (game + 1).validNel.map(next => (Started(next), GameStarted(next)))
    }
    case OpenTable => {
      case Closed(game) => (Opened(game), TableOpened).validNel
    }
    case CloseTable => {
      case Opened(game) => (Closed(game), TableClosed).validNel
    }
  }
}

object TableFactory {
  type Validation[A] = ValidatedNel[Error, A]

  sealed abstract class Error extends Product with Serializable

  final case object HandLimitExceeded extends Error {

    def apply(turn: List[Player], limit: Int): Validation[Unit] =
      if (turn.forall(_.cards.size == limit)) ().validNel else HandLimitExceeded.invalidNel

    def apply(player: Player, card: Stock, limit: Int): Validation[Player] =
      if (player.cards.size > limit) HandLimitExceeded.invalidNel
      else player.copy(cards = card :: player.cards).validNel
  }

  final case object NoWinner extends Error {

    def apply(turn: List[Player]): Validation[List[String]] =
      if (turn.isEmpty) NoWinner.invalidNel
      else
        turn
          .foldLeft(List.empty[Player]) {
            case (Nil, next) => List(next)
            case (winners, next) =>
              next.score.compare(winners.head.score) match {
                case 0          => next :: winners
                case i if i > 0 => List(next)
                case _          => winners
              }
          }
          .map(_.name)
          .validNel
  }

}
