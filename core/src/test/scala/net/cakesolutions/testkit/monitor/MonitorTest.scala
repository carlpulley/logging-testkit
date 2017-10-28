package net.cakesolutions.testkit.monitor

import scala.concurrent.duration._

import org.scalatest.{FreeSpec, Matchers}
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class MonitorTest extends FreeSpec with Matchers with GeneratorDrivenPropertyChecks {

  "Simple finite state machine" - {
    "with no notifications" - {
      val simple = Monitor[Int, String](0) {
        case 0 => {
          case Observe("1") =>
            Goto(1)
          case Observe("2") =>
            Stop(Accept())
          case Observe("") =>
            Stay()
        }
      }

      simple.run(???)
    }

    "with notifications" - {
      val simple = Monitor[Int, String](0) {
        case 0 => {
          case Observe("1") =>
            Goto(1, ???)
          case Observe("2") =>
            Stop(Accept())
          case Observe("") =>
            Stay(???)
        }
      }

      simple.run(???)
    }
  }

  "Timed state machine" - {
    "with no notifications" - {
      val timed = Monitor[Int, String](0, 1.second) {
        case 0 => {
          case Observe("1") =>
            Goto(1, 1.second)
          case Observe("2") =>
            Stop(Accept())
          case Observe("") =>
            Stay()
        }
      }

      timed.run(???)
    }

    "with notifications" - {
      val timed = Monitor[Int, String](0, 1.second) {
        case 0 => {
          case Observe("1") =>
            Goto(1, 1.second, ???)
          case Observe("2") =>
            Stop(Accept())
          case Observe("") =>
            Stay(???)
        }
      }

      timed.run(???)
    }
  }
}
