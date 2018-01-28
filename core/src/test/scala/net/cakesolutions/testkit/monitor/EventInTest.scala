package net.cakesolutions.testkit.monitor

import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FreeSpec, Matchers}

class EventInTest extends FreeSpec with Matchers with GeneratorDrivenPropertyChecks {

  import net.cakesolutions.testkit.generators.TestGenerators._

  "StateTimeout is a Throwable instance" in {
    forAll(stateTimeoutGen) { timeout =>
      timeout shouldBe a[Throwable]
    }
  }
}
