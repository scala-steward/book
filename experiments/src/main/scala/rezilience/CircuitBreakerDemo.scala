package rezilience

import nl.vroste.rezilience.*
import nl.vroste.rezilience.CircuitBreaker.*
import zio.{Schedule, ZLayer}
import java.time.Instant
import scala.concurrent.TimeoutException


case class Cost(value: Int)
case class Analysis(content: String)

trait ExpensiveSystem:
  def call: ZIO[Any, String, Analysis]
  val billToDate:ZIO[Any, String, Cost]

object Scenario:
  enum Step:
    case Success,
      Failure

import rezilience.Scenario.Step
import Scenario.Step.*

object CircuitBreakerDemo extends ZIOAppDefault:

  val makeCircuitBreaker
      : ZIO[Scope, Nothing, CircuitBreaker[
        Any
      ]] =
    CircuitBreaker.make(
      trippingStrategy =
        TrippingStrategy
          .failureCount(maxFailures = 2),
      resetPolicy =
        Retry
          .Schedules
          .common(),
//          .exponentialBackoff(
//            min = 1.second,
//            max = 4.second,
//          ),

      onStateChange = (state) =>
        ZIO.debug(
          s"State change: $state"
        )
        
    )

  def run =
    defer:
      ZIO.serviceWithZIO[ExpensiveSystem](_.call)
        .ignore
        .repeat(Schedule.recurs(8) && Schedule.spaced(200.millis))
        .run
      ZIO.serviceWithZIO[ExpensiveSystem](_.billToDate)
        .debug
        .run

    .provide:
//       ExternalSystem // TOGGLE
      ExternalSystemProtected // TOGGLE
        .live

end CircuitBreakerDemo

case class ExternalSystemProtected(
    externalSystem: ExpensiveSystem,
    circuitBreaker: CircuitBreaker[String]
                                  ) extends ExpensiveSystem:
  val billToDate: ZIO[Any, String, Cost] =
    externalSystem.billToDate

  def call: ZIO[Any, String, Analysis] =
    circuitBreaker:
      externalSystem.call
    .tap(r => ZIO.debug(s"Result: $r"))
    .mapError:
      case CircuitBreakerOpen =>
        "Circuit breaker blocked the call to our external system"
      case WrappedError(e) =>
        println("ignored boom?")
        s"External system threw an exception: $e"
    .tapError(e => ZIO.debug(e))

object ExternalSystemProtected:
  val live: ZLayer[Any, Nothing, ExpensiveSystem] =
    ZLayer.fromZIO:
      defer:
        ExternalSystemProtected(
          ZIO.service[ExpensiveSystem].run,
          CircuitBreakerDemo.makeCircuitBreaker.run
        )
      .provide(ExternalSystem.live, Scope.default)

// Invisible mdoc fencess

object ExternalSystem:
  val live: ZLayer[Any, Nothing, ExpensiveSystem] =
    ZLayer.fromZIO:
      defer:
        val valueProducer =
          scheduledValues(
            (300.millis, Success),
            (200.millis, Failure),
            // TODO Restore when I can get CB to reconnect :(
            (400.millis, Failure),
            (5.seconds, Success)
          ).run
        ExternalSystem(
          Ref.make(0).run,
          valueProducer
        )

case class ExternalSystem(
                           requests: Ref[Int],
                           responseAction: ZIO[
                             Any, // access time
                             TimeoutException,
                             Scenario.Step
                           ]

                         ) extends ExpensiveSystem:

  // TODO: Better error type than Throwable
  val billToDate:ZIO[Any, String, Cost] =
    requests.get.map:
      Cost(_)

  def call: ZIO[Any, String, Analysis] =
    defer:
      ZIO.debug("Called underlying ExternalSystem").run
      val requestCount =
        requests.updateAndGet(_ + 1).run
      responseAction.orDie.run match
        case Success =>
          ZIO
            .succeed:
              Analysis:
                s"Expensive report #$requestCount"
            .run
        case Failure =>
          ZIO.debug:
            "boom"
          .run
          ZIO.fail:
            "Something went wrong"
          .run

end ExternalSystem


// TODO Consider deleting
object InstantOps:
  extension (i: Instant)
    def plusZ(duration: zio.Duration): Instant =
      i.plus(duration.asJava)

import InstantOps._

/* Goal: If I accessed this from:
 * 0-1 seconds, I would get "First Value" 1-4
 * seconds, I would get "Second Value" 4-14
 * seconds, I would get "Third Value" 14+
 * seconds, it would fail */

// TODO Consider TimeSequence as a name
def scheduledValues[A](
                        value: (Duration, A),
                        values: (Duration, A)*
                      ): ZIO[
  Any, // construction time
  Nothing,
  ZIO[
    Any, // access time
    TimeoutException,
    A
  ]
] =
  defer {
    val startTime = Clock.instant.run
    val timeTable =
      createTimeTableX(
        startTime,
        value,
        values* // Yay Scala3 :)
      )
    accessX(timeTable)
  }

// TODO Some comments, tests, examples, etc to
// make this function more obvious
private def createTimeTableX[A](
                                       startTime: Instant,
                                       value: (Duration, A),
                                       values: (Duration, A)*
                                     ): Seq[ExpiringValue[A]] =
  values.scanLeft(
    ExpiringValue(
      startTime.plusZ(value._1),
      value._2
    )
  ) {
    case (
      ExpiringValue(elapsed, _),
      (duration, value)
      ) =>
      ExpiringValue(
        elapsed.plusZ(duration),
        value
      )
  }

/** Input: (1 minute, "value1") (2 minute,
 * "value2")
 *
 * Runtime: Zero value: (8:00 + 1 minute,
 * "value1")
 *
 * case ((8:01, _) , (2.minutes, "value2")) =>
 * (8:01 + 2.minutes, "value2")
 *
 * Output: ( ("8:01", "value1"), ("8:03",
 * "value2") )
 */
private def accessX[A](
                              timeTable: Seq[ExpiringValue[A]]
                            ): ZIO[Any, TimeoutException, A] =
  defer {
    val now = Clock.instant.run
    ZIO
      .getOrFailWith(
        new TimeoutException("TOO LATE")
      ) {
        timeTable
          .find(_.expirationTime.isAfter(now))
          .map(_.value)
      }
      .run
  }

private case class ExpiringValue[A](
                                     expirationTime: Instant,
                                     value: A
                                   )
