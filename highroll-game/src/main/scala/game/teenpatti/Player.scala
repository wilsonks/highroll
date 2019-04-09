package game

package teenpatti

import deck.Card.Stock

final case class Player(name: String, cards: List[Stock] = Nil)

object Player {

  def score(player: Player): Score = score(player.cards)

  def score(cards: List[Stock]): Score = {
    val size = cards.size
    val straightMask = (1 << size) - 1
    val straightWrap: Int = (1 << 12) + ((straightMask - 1) >> 1)
    val weights = cards.map(_.value.rank)

    val suits = cards.map(_.value.suit)
    val ors = weights.foldLeft(0)((a, b) => a | (1 << (b - 1)))
    val counts = weights.groupBy(r => r).mapValues(_.length).toSeq.sortBy(_._2).reverse
    val sum = counts.foldLeft(0) {
      case (s, (r, i)) => s + (r << ((i - 1) * 4))
    }
    val trail = weights.distinct.lengthCompare(1) == 0
    val straight = ors == straightWrap || ((ors >> Integer.numberOfTrailingZeros(ors)) == straightMask)
    val flush = suits.distinct.lengthCompare(1) == 0
    val pair = counts.exists(_._2 == 2)
    if (trail) ThreeOfAKind(ors)
    else if (straight & flush) StraightFlush(ors)
    else if (straight) Straight(ors)
    else if (flush) Flush(ors)
    else if (pair) Pair(sum)
    else HighCard(ors)
  }

  sealed abstract class Score(val hand: Hand) extends Product with Serializable with Ordered[Score] {

    def value: Int

    override def compare(that: Score): Chips = (hand, value).compare((that.hand, that.value))
  }

  sealed abstract class Hand(val rank: Int) extends Product with Serializable with Ordered[Hand] {
    def compare(that: Hand): Int = rank.compare(that.rank)
  }

  case class HighCard(value: Int) extends Score(HighCard)

  case class Pair(value: Int) extends Score(Pair)

  case class Flush(value: Int) extends Score(Flush)

  case class Straight(value: Int) extends Score(Straight)

  case class StraightFlush(value: Int) extends Score(StraightFlush)

  case class ThreeOfAKind(value: Int) extends Score(ThreeOfAKind)

  case object HighCard extends Hand(0)

  case object Pair extends Hand(1)

  case object Flush extends Hand(2)

  case object Straight extends Hand(3)

  case object StraightFlush extends Hand(4)

  case object ThreeOfAKind extends Hand(5)

}
