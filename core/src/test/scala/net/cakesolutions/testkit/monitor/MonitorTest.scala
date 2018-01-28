// Copyright 2018 Carl Pulley

package net.cakesolutions.testkit.monitor

import scala.concurrent.duration._

import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import org.scalatest.{FreeSpec, Matchers}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.slf4j.{Logger, LoggerFactory}

import net.cakesolutions.testkit.matchers.ObservableMatcher._

class MonitorTest extends FreeSpec with Matchers with GeneratorDrivenPropertyChecks {

  import Interactions._

  implicit val logger: Logger = LoggerFactory.getLogger(getClass)
  implicit val timeout: FiniteDuration = 3.seconds

  "Simple finite state machine" - {
    "with no notifications" in {
      val simple: Behaviour[Int, String] = {
        case 0 => {
          case Observe("1") =>
            Goto(1)
        }
        case 1 => {
          case Observe("2") =>
            Stop(Accept("stop"))
        }
      }
      val events = Observable("1", "2")

      events.monitor(0)(simple) should observe[ActionOut[Notify]](Observe(Accept("stop")))
    }

    "with notifications" in {
      val simple: Behaviour[Int, String] = {
        case 0 => {
          case Observe("1") =>
            Goto(1, Accept("goto-1"))
        }
        case 1 => {
          case Observe("2") =>
            Stay(Accept("stay"))
          case Observe("3") =>
            Stop(Accept("stop"))
        }
      }
      val events = Observable("1", "2", "3")

      events.monitor(0)(simple) should observe[ActionOut[Notify]](Observe(Accept("goto-1")), Observe(Accept("stay")), Observe(Accept("stop")))
    }
  }

  "Timed state machine" - {
    "with no notifications" - {
      "and no timeouts" in {
        val timed: Behaviour[Int, String] = {
          case 0 => {
            case Observe("1") =>
              Goto(1, 1.second)
          }
          case 1 => {
            case Observe("2") =>
              Stop(Accept("stop"))
          }
        }
        val events = Observable("1", "2")

        events.monitor(0, initialTimeout = Some(1.second))(timed) should observe[ActionOut[Notify]](Observe(Accept("stop")))
      }

      "and with timeouts (global timeout)" in {
        val timed: Behaviour[Int, String] = {
          case 0 => {
            case StateTimeout =>
              Stop(Accept("timeout"))
          }
        }
        val events = Observable.never[String]

        events.monitor(0, initialTimeout = Some(1.second))(timed) should observe[ActionOut[Notify]](Observe(Accept("timeout")))
      }

      "and with timeouts (state timeout)" in {
        val timed: Behaviour[Int, String] = {
          case 0 => {
            case Observe("1") =>
              Goto(1, 1.second)
          }
          case 1 => {
            case StateTimeout =>
              Stop(Accept("timeout"))
          }
        }
        val events = Observable.cons("1", Observable.never)

        events.monitor(0, initialTimeout = Some(1.second))(timed) should observe[ActionOut[Notify]](Observe(Accept("timeout")))
      }
    }

    "with notifications" - {
      "and no timeouts" in {
        val timed: Behaviour[Int, String] = {
          case 0 => {
            case Observe("1") =>
              Goto(1, 1.second, Accept("goto-1"))
          }
          case 1 => {
            case Observe("2") =>
              Stop(Accept("stop"))
          }
        }
        val events = Observable("1", "2")

        events.monitor(0, initialTimeout = Some(1.second))(timed) should observe[ActionOut[Notify]](Observe(Accept("goto-1")), Observe(Accept("stop")))
      }

      "and with timeouts" in {
        val timed: Behaviour[Int, String] = {
          case 0 => {
            case Observe("1") =>
              Goto(1, 1.second)
          }
          case 1 => {
            case StateTimeout =>
              Stop(Accept("timeout"))
          }
        }
        val events = Observable.cons("1", Observable.never)

        events.monitor(0, initialTimeout = Some(1.second))(timed) should observe[ActionOut[Notify]](Observe(Accept("timeout")))
      }
    }
  }

  // FIXME: surely we should be able to classify failure modes here?
  "Event observable closes early" - {
    "with no events flowing" in {
      val early: Behaviour[Int, String] = {
        case _ => {
          case _ =>
            Stop(Accept("empty"))
        }
      }
      val events = Observable.empty[String]

      events.monitor(0)(early) should observe[ActionOut[Notify]]()
    }

    "with at least one event that flows" in {
      val early: Behaviour[Int, String] = {
        case 0 => {
          case Observe("1") =>
            Goto(1)
        }
        case 1 => {
          case _ =>
            Stop(Accept("empty"))
        }
      }
      val events = Observable("1")

      events.monitor(0)(early) should observe[ActionOut[Notify]]()
    }
  }
}
