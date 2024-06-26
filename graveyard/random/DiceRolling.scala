package random

import scala.util.Random

def rollDice(): Int =
  Random.nextInt(6) + 1

@main
def randNumEx =
  println(rollDice())
  println(rollDice())

enum GameState:
  case InProgress(roundResult: String)
  case Win
  case Lose

def scoreRound(input: Int): GameState =
  input match
    case 6 =>
      GameState.Win
    case 1 =>
      GameState.Lose
    case _ =>
      GameState.InProgress("Attempt: " + input)

def fullRound(): GameState =
  val roll =
    rollDice()
  scoreRound(roll)

@main
def playASingleRound() =
  println(fullRound())

val rollDiceZ: ZIO[Any, Nothing, Int] =
  zio.Random.nextIntBetween(1, 7)

import zio.{ZIO, ZIOAppDefault}
object RollTheDice extends ZIOAppDefault:
  val logic =
    rollDiceZ.debug

  def run =
    logic

val fullRoundZ: ZIO[Any, Nothing, GameState] =
  rollDiceZ.map(scoreRound)

// The problem above is that you can test the winner logic completely separate from the random number generator.
// The next example cannot be split so easily.

import zio.Ref

val threeChances =
  defer {
    val remainingChancesR =
      Ref.make(3).run
    val gameState =
      Ref
        .make[GameState](
          GameState.InProgress("Starting")
        )
        .run

    while (
      gameState.get.run == GameState.InProgress
    ) {
      rollDiceZ.run
      val remainingChances =
        remainingChancesR.getAndUpdate(_ - 1).run
      if (remainingChances == 0)
        gameState.set(GameState.Lose).run
    }

    val finalGameState =
      gameState
        .get
        .run // note: this has to be outside the debug parameter
    ZIO
      .debug(
        "Final game result: " + finalGameState
      )
      .run
  }

/* Rewrite these as test cases using the standard
 * zio.Random
 *
 * object ThreeChances extends ZIOAppDefault:
 * def run =
 * threeChances.provide(
 * RandomBoundedIntFake.apply(Seq(2, 5, 6)) )
 *
 * object LoseInTwoChances extends ZIOAppDefault:
 * def run =
 * threeChances.provide(
 * RandomBoundedIntFake.apply(Seq(2, 1)) ) */
