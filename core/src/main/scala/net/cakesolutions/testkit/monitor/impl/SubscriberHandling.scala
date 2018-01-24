package net.cakesolutions.testkit.monitor.impl

import akka.receive.pattern.ReceivePipeline
import akka.receive.pattern.ReceivePipeline.HandledCompletely
import monix.reactive.observers.Subscriber

import net.cakesolutions.testkit.monitor.Notify

private[monitor] trait SubscriberHandling {
  this: ReceivePipeline =>

  import IOAutomata._

  private[this] var subscribers: Set[Subscriber[Notify]] = Set.empty

  def getSubscribers: Set[Subscriber[Notify]] = subscribers

  pipelineOuter {
    case Subscribe(sub: Subscriber[Notify]) =>
      subscribers = subscribers + sub
      HandledCompletely
    case Unsubscribe(sub: Subscriber[Notify]) =>
      subscribers = subscribers - sub
      HandledCompletely
  }
}
