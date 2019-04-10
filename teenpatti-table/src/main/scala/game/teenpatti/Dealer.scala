package game

package teenpatti

import deck.Card
import fxml.FX.{Driver, Reader, Writer}
import fxml.io.Cell
import fxml.syntax._
import game.teenpatti.TableCommand.{AssignCard, StartGame}
import javafx.scene.control.{Button, Label}
import javafx.scene.input.MouseEvent.MOUSE_RELEASED
import scalafxml.core.macros.sfxml

@sfxml
class Dealer(val gameId: Label, val start: Button)(
  D: Driver[Table.Transition],
  R: Reader[Card.Stock],
  W: Writer[TableCommand]) {

  val pause = new Cell(false)
  val game: Cell[GameId] = D.map(_._1.game).hold(0)
  R.open(pause)

  R.map(AssignCard(game.sample(), _)).listen(W.push)

  game.map(_.toString).updates(gameId.textProperty())

  start.handle(MOUSE_RELEASED).delay(W.push(StartGame))

  D.listen(println)
}
