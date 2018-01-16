// Copyright 2018 Carl Pulley

package net.cakesolutions.testkit.monitor

import scala.concurrent.duration._

import akka.actor.ActorSystem
import monix.execution.Scheduler
import monix.reactive.Observable
import org.scalatest.{FreeSpec, Matchers}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.slf4j.{Logger, LoggerFactory}

import net.cakesolutions.testkit.matchers.ObservableMatcher._

class MonitorTest extends FreeSpec with Matchers with GeneratorDrivenPropertyChecks {

  implicit val system: ActorSystem = ActorSystem("MonitorTest")
  implicit val scheduler: Scheduler = Scheduler(system.dispatcher)
  implicit val logger: Logger = LoggerFactory.getLogger(getClass)
  implicit val timeout: FiniteDuration = 3.seconds

  "Simple finite state machine" - {
    "with no notifications" in {
      val simple = Monitor[Int, String](0) {
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

      simple.run(events) should observe(Accept("stop"))
    }

    "with notifications" in {
      val simple = Monitor[Int, String](0) {
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

      simple.run(events) should observe(Accept("goto-1"), Accept("stay"), Accept("stop"))
    }
  }

  "Timed state machine" - {
    "with no notifications" - {
      "and no timeouts" in {
        val timed = Monitor[Int, String](0, 1.second) {
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

        timed.run(events) should observe(Accept("stop"))
      }

      "and with timeouts (global timeout)" in {
        val timed = Monitor[Int, String](0, 1.second) {
          case 0 => {
            case StateTimeout =>
              Stop(Accept("timeout"))
          }
        }
        val events = Observable.never

        timed.run(events) should observe(Accept("timeout"))
      }

      "and with timeouts (state timeout)" in {
        val timed = Monitor[Int, String](0, 1.second) {
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

        timed.run(events) should observe(Accept("timeout"))
      }
    }

    "with notifications" - {
      "and no timeouts" in {
        val timed = Monitor[Int, String](0, 1.second) {
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

        timed.run(events) should observe(Accept("goto-1"), Accept("stop"))
      }

      "and with timeouts" in {
        val timed = Monitor[Int, String](0, 1.second) {
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

        timed.run(events) should observe(Accept("timeout"))
      }
    }
  }

  "Event observable closes early" - {
    val upstreamClosure = "FSM upstreams closed"

    "with no events flowing" in {
      val early = Monitor[Int, String](0) {
        case _ => {
          case _ =>
            Stop(Accept("empty"))
        }
      }
      val events = Observable.empty[String]

      early.run(events).map {
        case Fail(reasons@_*) =>
          Fail(reasons.head.take(upstreamClosure.length))
        case notification: Notify =>
          notification
      } should observe(Fail(upstreamClosure))
    }

    "with at least one event that flows" in {
      val early = Monitor[Int, String](0) {
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

      early.run(events).map {
        case Fail(reasons@_*) =>
          Fail(reasons.head.take(upstreamClosure.length))
        case notification: Notify =>
          notification
      } should observe(Fail(upstreamClosure))
    }
  }
}
