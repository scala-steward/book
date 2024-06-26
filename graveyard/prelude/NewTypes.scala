package prelude

import zio.Console.printLine
import zio.prelude.Assertion.*
import zio.prelude.{Assertion, Newtype}

/* Notes: Only works for primitive types. You
 * can't get compile-time guarantees for custom
 * classes */

type NewSpecialClass =
  NewSpecialClass.Type
object NewSpecialClass
    extends Newtype[OurPrimitiveClass]:
  override inline def assertion
      : Assertion[OurPrimitiveClass] =
    greaterThan(
      OurPrimitiveClass(
        "ignored_id",
        age =
          0
      )
    )

object SecurePassword extends Newtype[String]:
  extension (n: SecurePassword)
    def salt =
      SecurePassword.unwrap(n) +
        "random_salt_bits"

  override inline def assertion
      : Assertion[String] =
    hasLength(greaterThanOrEqualTo(10)) &&
      specialCharacters

  inline def specialCharacters
      : Assertion[String] =
    contains("$") || contains("!")

type SecurePassword =
  SecurePassword.Type

object NewTypeDemos extends ZIOAppDefault:

  def run =
    val badValue =
      "Special String #$"
    for
      primitiveClassResult <-
        ZIO.fromEither(
          OurPrimitiveClass
            .safeConstructor("idValue", -10)
        )
      specialClassResult <-
        ZIO.succeed(
          NewSpecialClass.make(
            OurPrimitiveClass("idValue", -10)
          )
        )
      accountNumber <-
        ZIO.succeed {
          SecurePassword("Special String #$")
        }
      accountNumbers <-
        ZIO.succeed {
          SecurePassword(
            "Special String $",
            "bad string!"
          )
        }
      accountNumberRuntime <-
        ZIO.succeed {
          SecurePassword.make(badValue)
        }
      _ <-
        printLine(
          s"Account Number: $accountNumber"
        )
      _ <-
        printLine(
          s"Account Number salted: ${accountNumber.salt}"
        )
    yield ()
    end for
  end run
end NewTypeDemos
