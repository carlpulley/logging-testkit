// Copyright 2018 Carl Pulley

package net.cakesolutions.testkit.logging

import scala.concurrent.Promise

import io.circe.Json
import monix.execution.{Cancelable, Scheduler}
import monix.reactive.Observable
import monix.reactive.observers.Subscriber

import net.cakesolutions.utils.ValueDiscard

class TestLogSource(testData: Observable[LogEvent[Json]]) extends LoggingSource[Json] {

  /** @inheritdoc*/
  override protected def subscriberPolling(id: String, subscriber: Subscriber[LogEvent[Json]], cancelP: Promise[Unit])(implicit scheduler: Scheduler): Unit = {
    ValueDiscard[Cancelable] {
      testData
        .doOnComplete(() => cancelP.success(()))
        .subscribe(subscriber)
    }
  }
}
