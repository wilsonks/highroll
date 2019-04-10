package game

package teenpatti

import deck.Card.Stock

sealed abstract class Table extends Product with Serializable {
  def game: GameId
}

sealed abstract class TableCommand extends Product with Serializable

sealed abstract class TableEvent extends Product with Serializable

object Table {

  type Transition = (Table, TableEvent)

  sealed abstract class Dealing extends Table

  final case class Opened(game: GameId) extends Dealing

  final case class Started(game: GameId) extends Dealing

  final case class Drawing(game: GameId, turn: List[Player]) extends Dealing

  final case class Closed(game: GameId) extends Table

  object Dealing {

    def unapply(arg: Table): Option[GameId] = arg match {
      case table: Dealing => Some(table.game)
      case _              => None
    }
  }
}

object TableCommand {

  sealed abstract class GameCommand extends TableCommand {
    def game: GameId
  }

  final case class StartDraw(game: GameId) extends GameCommand

  final case class CompleteGame(game: GameId) extends GameCommand

  final case class AssignCard(game: GameId, card: Stock) extends GameCommand

  final case class VoidGame(game: GameId) extends GameCommand

  final case object StartGame extends TableCommand

  final case object OpenTable extends TableCommand

  final case object CloseTable extends TableCommand

}

object TableEvent {

  sealed abstract class GameEvent extends TableEvent {
    def game: GameId
  }

  final case class CommandRejected(command: TableCommand, errors: List[TableFactory.Error]) extends TableEvent

  final case class GameStarted(game: GameId) extends GameEvent

  final case class DrawStarted(game: GameId) extends GameEvent

  final case class CardAssigned(game: GameId, player: String, card: Stock) extends GameEvent

  final case class GameCompleted(game: GameId, winners: List[String]) extends GameEvent

  final case class GameVoided(game: GameId) extends GameEvent

  final case object TableOpened extends TableEvent

  final case object TableClosed extends TableEvent
}
