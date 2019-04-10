package game

package teenpatti

import cats.Functor
import cats.data.ValidatedNel
import cats.syntax.apply._
import cats.syntax.functor._
import cats.syntax.validated._
import fs2.Stream
import fs2.concurrent.Queue

final case class SessionFactory(maxBet: Chips, payout: Map[Hand, BigDecimal]) {

  import Session._
  import SessionCommand._
  import SessionEvent._
  import SessionFactory._
  import TableEvent._

  def build[F[_]](chips: Chips, input: Queue[F, Input])(implicit F: Functor[F]): Stream[F, Transition] =
    Stream
      .iterateEval[F, Transition](Settled(chips) -> ChipsIssued(chips)) {
        case (last, _) => step(last, input)
      }
      .takeThrough(_._1 != Closed)

  def step[F[_]](state: Session, input: Queue[F, Input])(implicit F: Functor[F]): F[Session.Transition] =
    input.dequeue1.map {
      case in if !eval.isDefinedAt(in)        => (state, InputRejected(in, Nil))
      case in if !eval(in).isDefinedAt(state) => (state, InputRejected(in, Nil))
      case in                                 => eval(in)(state).fold(rejections => (state, InputRejected(in, rejections.toList)), identity)
    }

  def eval: PartialFunction[Input, PartialFunction[Session, Validation[Transition]]] = {
    case Left(GameCompleted(game, winners)) => {
      case Staking(bank, `game`, bets) =>
        PayoutNotFound(winners, bank, bets, payout).map {
          case (chips, wins) => (Settled(chips), BetsSettled(game, wins))
        }
    }
    case Left(DrawStarted(game)) => {
      case Staking(bank, `game`, Nil) => (Settled(bank), BettingDeclined(game)).validNel
      case staking: Staking           => (staking, BettingDeclined(game)).validNel
    }
    case Left(GameStarted(game)) => {
      case Settled(bank) => BankInsufficient(bank).map(Staking(_, game, Nil) -> BettingOpened(game))
    }
    case Right(PlaceBet(game, bet)) => {
      case Staking(bank, `game`, bets) =>
        (BankInsufficient(bank, bet), MaxBetExceeded(bet :: bets, maxBet)).mapN {
          case (chips, next) => (Staking(chips, game, next), BetPlaced(game, bet))
        }
    }
    case Right(RemoveBet(game, bet)) => {
      case Staking(bank, `game`, bets) =>
        BetNotFound(bet, bank, bets).map {
          case (chips, next) => (Staking(chips, game, next), BetRemoved(game, bet))
        }
    }
    case Right(CloseSession) => {
      case Staking(bank, _, bets) => (Closed, SessionClosed(bank + bets.iterator.map(_.chips).sum)).validNel
      case Banked(chips)          => (Closed, SessionClosed(chips)).validNel
    }
  }
}

object SessionFactory {

  type Validation[A] = ValidatedNel[Error, A]

  sealed abstract class Error extends Product with Serializable

  final case object BankInsufficient extends Error {

    def apply(bank: Chips): Validation[Chips] =
      if (bank > 0) bank.validNel else BankInsufficient.invalidNel

    def apply(bank: Chips, bet: Player.Bet): Validation[Chips] =
      if (bank >= bet.chips) (bank - bet.chips).validNel else BankInsufficient.invalidNel
  }

  final case object BetNotFound extends Error {

    def apply(bet: Player.Bet, bank: Chips, bets: List[Player.Bet]): Validation[(Chips, List[Player.Bet])] =
      if (bets.contains(bet)) (bank + bet.chips, bets diff List(bet)).validNel else BetNotFound.invalidNel
  }

  final case object MaxBetExceeded extends Error {

    def apply(bets: List[Player.Bet], limit: Chips): Validation[List[Player.Bet]] =
      if (bets.iterator.map(_.chips).sum <= limit) bets.validNel else MaxBetExceeded.invalidNel
  }

  final case object PayoutNotFound extends Error {

    def apply(
      winners: List[String],
      bank: Chips,
      bets: List[Player.Bet],
      payout: Map[Hand, BigDecimal]
    ): Validation[(Chips, List[Player.Win])] =
      if (bets.exists(bet => !payout.contains(bet.hand))) PayoutNotFound.invalidNel
      else
        bets
          .map(bet => Player.Win(if (winners.contains(bet.player)) bet.chips * payout(bet.hand) else 0, bet))
          .validNel
          .map(wins => wins.iterator.map(_.chips).sum -> wins)
  }
}
