// Copyright 2018 Carl Pulley

package net.cakesolutions.testkit.monitor

import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FreeSpec, Matchers}

class NotifyTest extends FreeSpec with Matchers with GeneratorDrivenPropertyChecks {

  import TestGenerators._

  "Accept instances may be doubled inverted" in {
    forAll(acceptGen) { (accept: Notify) =>
      accept.invert.invert shouldEqual accept
    }
  }

  "Fail instances may be doubled inverted" in {
    forAll(failGen) { (fail: Notify) =>
      fail.invert.invert shouldEqual fail
    }
  }

  "Notify instances may be doubled inverted" in {
    forAll(notifyGen) { (notify: Notify) =>
      notify.invert.invert shouldEqual notify
    }
  }
}
