package game

package teenpatti

sealed abstract class Session extends Product with Serializable

sealed abstract class SessionCommand extends Product with Serializable

sealed abstract class SessionEvent extends Product with Serializable

object Session {

  type Input = Either[TableEvent, SessionCommand]
  type Transition = (Session, SessionEvent)

  sealed abstract class Banked extends Session {

    def bank: Chips
  }

  final case class Settled(bank: Chips) extends Banked

  final case class Staking(bank: Chips, game: GameId, bets: List[Player.Bet]) extends Banked

  final case object Closed extends Session

  object Banked {

    def unapply(arg: Session): Option[Chips] = arg match {
      case session: Banked => Some(session.bank)
      case _               => None
    }
  }
}

object SessionCommand {

  sealed abstract class GameCommand extends SessionCommand {
    def game: GameId
  }

  final case class PlaceBet(game: GameId, bet: Player.Bet) extends GameCommand

  final case class RemoveBet(game: GameId, bet: Player.Bet) extends GameCommand

  final case object CloseSession extends SessionCommand
}

object SessionEvent {

  sealed abstract class GameEvent extends SessionEvent {
    def game: GameId
  }

  final case class InputRejected(input: Session.Input, errors: List[SessionFactory.Error]) extends SessionEvent

  final case class ChipsIssued(chips: Chips) extends SessionEvent

  final case class BettingOpened(game: GameId) extends GameEvent

  final case class BetPlaced(game: GameId, bet: Player.Bet) extends GameEvent

  final case class BetRemoved(game: GameId, bet: Player.Bet) extends GameEvent

  final case class BettingDeclined(game: GameId) extends GameEvent

  final case class BettingClosed(game: GameId) extends GameEvent

  final case class BettingVoided(game: GameId) extends GameEvent

  final case class BetsSettled(game: GameId, wins: List[Player.Win]) extends GameEvent

  final case class SessionClosed(chips: Chips) extends SessionEvent
}
