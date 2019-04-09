package game

package teenpatti

import deck.Card.Stock

sealed abstract class Table extends Product with Serializable

sealed abstract class TableCommand extends Product with Serializable

sealed abstract class TableEvent extends Product with Serializable

object Table {

  type Transition = (Table, TableEvent)

  sealed abstract class Gaming extends Table {
    def game: GameId
  }

  final case class Started(game: GameId) extends Gaming

  final case class Drawing(game: GameId, turn: List[Player]) extends Gaming

  final case object Opened extends Table

  final case object Closed extends Table

  object Gaming {

    def unapply(arg: Table): Option[GameId] = arg match {
      case g: Gaming => Some(g.game)
      case _         => None
    }
  }
}

object TableCommand {

  sealed abstract class GameCommand extends TableCommand {
    def game: GameId
  }

  final case class CloseBetting(game: GameId) extends GameCommand

  final case class CompleteGame(game: GameId) extends GameCommand

  final case class AssignCard(game: GameId, card: Stock) extends GameCommand

  final case class VoidGame(game: GameId) extends GameCommand

  final case object OpenBetting extends TableCommand

  final case object OpenTable extends TableCommand

  final case object CloseTable extends TableCommand

}

object TableEvent {

  sealed abstract class GameEvent extends TableEvent {
    def game: GameId
  }

  final case class CommandRejected(command: TableCommand, rejections: List[Dealer.Rejection]) extends TableEvent

  final case class BettingOpened(game: GameId) extends GameEvent

  final case class BettingClosed(game: GameId) extends GameEvent

  final case class CardAssigned(game: GameId, player: String, card: Stock) extends GameEvent

  final case class GameCompleted(game: GameId, winners: List[String]) extends GameEvent

  final case class GameVoided(game: GameId) extends GameEvent

  final case object TableOpened extends TableEvent

  final case object TableClosed extends TableEvent
}
