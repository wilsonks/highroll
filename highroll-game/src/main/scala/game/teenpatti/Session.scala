package game

package teenpatti

import game.teenpatti.Player.Hand

sealed abstract class Session extends Product with Serializable

sealed abstract class SessionCommand extends Product with Serializable

sealed abstract class SessionEvent extends Product with Serializable

object Session {

  type Transition = (Session, SessionEvent)
}

object SessionCommand {

  sealed abstract class GameCommand extends SessionCommand {
    def game: GameId
  }

  final case class PlaceBet(game: GameId, player: String, hand: Hand, chips: Chips) extends SessionCommand

  final case class RemoveBet(game: GameId, player: String, hand: Hand, chips: Chips) extends SessionCommand
}

object SessionEvent {

  sealed abstract class GameEvent extends SessionEvent {
    def game: GameId
  }

  final case class BetPlaced(game: GameId, player: String, hand: Hand, chips: Chips) extends SessionEvent

  final case class BetRemoved(game: GameId, player: String, hand: Hand, chips: Chips) extends SessionEvent
}
