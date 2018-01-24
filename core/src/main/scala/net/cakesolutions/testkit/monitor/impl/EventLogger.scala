package net.cakesolutions.testkit.monitor.impl

import scala.reflect.ClassTag

import akka.receive.pattern.ReceivePipeline
import akka.receive.pattern.ReceivePipeline.Inner

import net.cakesolutions.testkit.monitor.ObservedEvent

private[monitor] abstract class EventLogger[IOState: ClassTag, Event] {
  this: ReceivePipeline =>

  import IOAutomata._

  // FIXME: set via configuration!
  protected val traceSize: Int = 100

  private[this] var trace = Vector.empty[(State[IOState], ObservedEvent[Event])]

  protected def getState: State[IOState]

  protected def getLoggedTrace: Vector[(State[IOState], ObservedEvent[Event])] = trace

  private def addEvent(state: State[IOState], event: ObservedEvent[Event]): Unit = {
    if (trace.length < traceSize) {
      trace = trace :+((state, event))
    } else {
      trace = trace.drop(1) :+((state, event))
    }
  }

  pipelineOuter {
    case event: ObservedEvent[Event] =>
      addEvent(getState, event)
      Inner(event)
  }
}
