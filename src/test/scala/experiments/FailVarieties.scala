package experiments

import zio.*
import zio.test.*

case object ErrorObjectA:
  val msg = "Failed: ErrorObjectA"
case object ErrorObjectB
    extends Exception("Failed: ErrorObjectB")

def matchTo1(n: Int) =
  if n == 1 then
    defer:
      printLine("Failed at 1").run
      ZIO.fail("Failed at 1")
  else
    defer:
      printLine("Passed 1").run
      ZIO.succeed("Passed 1")

def matchTo2(n: Int) =
  if n == 2 then
    defer:
      printLine("Failed at 2").run
      ZIO.fail(ErrorObjectA)
  else
    defer:
      printLine("Passed 2").run
      ZIO.succeed("Passed 2")

def matchTo3(n: Int) =
  if n == 2 then
    defer:
      printLine("Failed at 3").run
      ZIO.fail(ErrorObjectB)
  else
    defer:
      printLine("Passed 3").run
      ZIO.succeed("Passed 3")

def completeTo(n: Int) =
  defer:
    val r1 =  matchTo1(n).catchAll:
        e => ZIO.debug(s"Caught: $e").as(e)
    printLine(r1).run
    val r2 =  matchTo2(n).catchAll:
        e => ZIO.debug(s"Caught: $e").as(e)
    printLine(r2).run
    val r3 = matchTo3(n).catchAll:
        e => ZIO.debug(s"Caught: $e").as(e)
    printLine(s"Success: $r1 $r2 $r3").run

object FailVarieties extends ZIOSpecDefault:
  def spec =
    suite("Suite of Tests")(
      test("one"):
        defer:
          completeTo(1).run
          assertCompletes
      ,
      test("two"):
        defer:
          completeTo(2).run
          assertCompletes
      ,
      test("three"):
        defer:
          completeTo(3).run
          assertCompletes
      ,
      test("four"):
        defer:
          completeTo(4).run
          assertCompletes,
    )
end FailVarieties
