package game

import deck.Card.Stock

final case class Player(name: String, cards: List[Stock] = Nil) {
  def score: Hand.Score = Hand.score(cards)
}

object Player {

  final case class Bet(player: String, hand: Hand, chips: Chips)

  final case class Win(chips: Chips, bet: Bet)
}
