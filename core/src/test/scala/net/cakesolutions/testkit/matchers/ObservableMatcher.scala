// Copyright 2016-2018 Carl Pulley

package net.cakesolutions.testkit.matchers

import java.util.concurrent.TimeoutException

import scala.concurrent._
import scala.concurrent.duration._

import monix.execution.Scheduler
import monix.execution.exceptions.UpstreamTimeoutException
import monix.reactive.{Notification, Observable}
import monix.reactive.Notification.{OnComplete, OnError, OnNext}
import org.scalatest.matchers.{Matcher, MatchResult}

private final case class TestFailed[Action](n: Int, action: Action) extends Exception
private final case class ObservableError(n: Int, cause: Throwable) extends Exception
private final case class ObservableClosed(n: Int) extends Exception

object ObservableMatcher {

  def observe[Action](actions: Action*)(implicit scheduler: Scheduler, timeout: FiniteDuration) = new Matcher[Observable[Action]] {
    def apply(obs: Observable[Action]): MatchResult = {
      val result = Promise[Unit]

      if (actions.isEmpty) {
        result.success(())
      } else {
        obs
          .timeoutOnSlowUpstream(timeout + 1.second)
          .materialize
          .scan[Option[Int]](Some(0)) {
            case _: (Option[Int], Notification[Action]) if result.isCompleted =>
              None
            case (Some(n), OnNext(_)) if n < 0 || n >= actions.length =>
              result.failure(ObservableError(n, new IndexOutOfBoundsException))
              None
            case (Some(n), OnNext(act)) if actions(n) == act =>
              Some(n + 1)
            case (Some(n), OnNext(act)) =>
              result.failure(TestFailed[Action](n, act))
              None
            case (Some(n), OnError(exn)) =>
              result.failure(ObservableError(n, exn))
              None
            case (Some(n), OnComplete) if actions.length == n =>
              result.success(())
              None
            case (Some(n), OnComplete) =>
              result.failure(ObservableClosed(n))
              None
            case _ =>
              None
          }
          .subscribe()
      }

      try {
        Await.result(result.future, timeout + 1.second)
        MatchResult(matches = true, "", "")
      } catch {
        case _: TimeoutException =>
          val errMsg = s"After $timeout test timed out"
          MatchResult(matches = false, errMsg, errMsg)
        case ObservableError(n, cause: IndexOutOfBoundsException) if n < 0 =>
          val errMsg = s"Observable emitted an unexpected exception $cause with negative index $n"
          MatchResult(matches = false, errMsg, errMsg)
        case ObservableError(n, cause: IndexOutOfBoundsException) if actions.length >= n =>
          val errMsg = s"Observable emitted an unexpected exception $cause at list index $n (list contained ${actions.length} members!)"
          MatchResult(matches = false, errMsg, errMsg)
        case ObservableError(n, _: UpstreamTimeoutException) =>
          val errMsg = s"After ${timeout + 1.second} failed to observe any events flowing for matching"
          MatchResult(matches = false, errMsg, errMsg)
        case ObservableError(0, cause) =>
          val errMsg = s"Observable emitted an unexpected exception $cause - expected ${actions(0)}"
          MatchResult(matches = false, errMsg, errMsg)
        case ObservableError(position, cause) =>
          val errMsg = s"Observable emitted an unexpected exception $cause - matched ${actions.take(position)} and expected ${actions(position)}"
          MatchResult(matches = false, errMsg, errMsg)
        case ObservableClosed(0) =>
          val errMsg = s"Observable closed prematurely - expected ${actions(0)}"
          MatchResult(matches = false, errMsg, errMsg)
        case ObservableClosed(position) =>
          val errMsg = s"Observable closed prematurely - matched ${actions.take(position)} and expected ${actions(position)}"
          MatchResult(matches = false, errMsg, errMsg)
        case TestFailed(0, action) =>
          val errMsg = s"Test failed to match first event - received $action expected ${actions(0)}"
          MatchResult(matches = false, errMsg, errMsg)
        case TestFailed(position, action) =>
          val errMsg = s"Test matched ${actions.take(position)} and then matching failed - received $action expected ${actions(position)}"
          MatchResult(matches = false, errMsg, errMsg)
      }
    }
  }
}
