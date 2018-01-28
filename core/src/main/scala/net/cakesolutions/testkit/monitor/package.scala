// Copyright 2016-2018 Carl Pulley

package net.cakesolutions.testkit

import scala.concurrent.duration._

import monix.reactive.Observable
import monix.reactive.Notification.{OnComplete, OnError, OnNext}

package object monitor {

  import Interactions._

  /**
    * TODO:
    *
    * @tparam IOState
    * @tparam Event
    */
  type Behaviour[IOState, Event] =
    PartialFunction[IOState, PartialFunction[EventIn[Event], Action[IOState]]]

  /**
    * TODO:
    *
    * @param upstream
    * @tparam IOState
    * @tparam Event
    */
  implicit class ObservableMonitor[IOState, Event](upstream: Observable[Event]) {

    private val clock: Observable[EventInternal[Event]] =
      Observable.timerRepeated(0.seconds, 100.milliseconds, Tick)

    /**
      * TODO:
      *
      * @param initialState
      * @param initialTimeout
      * @param overallTimeout
      * @param transition
      * @return
      */
    def monitor(
      initialState: IOState,
      initialTimeout: Option[FiniteDuration] = None,
      overallTimeout: Option[FiniteDuration] = None
    )(
      transition: Behaviour[IOState, Event]
    ): Observable[ActionOut[Notify]] = {
      Observable.merge[EventInternal[Event]](upstream.asEventInternal, clock)
        .materialize
        .scan[MonitorState[IOState]](RunningState(initialState).timeout(initialTimeout).overall(overallTimeout)) {
          case (current: RunningState[IOState], OnNext(event: Observe[Event]))
            if transition.isDefinedAt(current.state) &&
              transition(current.state).isDefinedAt(event) =>
            next(transition)(current.state, event)
          case (current: RunningState[IOState], OnNext(Tick))
            if current.stateTimeout.exists(_.isOverdue()) &&
              transition.isDefinedAt(current.state) &&
              transition(current.state).isDefinedAt(StateTimeout) =>
            next(transition)(current.state, StateTimeout)
          case (current: RunningState[IOState], OnNext(Tick))
            if current.overallTimeout.exists(_.isOverdue()) &&
              transition.isDefinedAt(current.state) &&
              transition(current.state).isDefinedAt(MonitorTimeout) =>
            next(transition)(current.state, MonitorTimeout)
          case (current: RunningState[IOState], OnComplete) =>
            complete(current.state)
          case (current: RunningState[IOState], OnNext(event: Observe[Event])) =>
            error(current.state, TransitionFailure(event))
          case (current: RunningState[IOState], OnError(exn)) =>
            error(current.state, exn)
          case (current: RunningState[IOState], OnNext(Tick))
            if current.stateTimeout.exists(_.isOverdue()) =>
            error(current.state, StateTimeout)
          case (current: RunningState[IOState], OnNext(Tick))
            if current.overallTimeout.exists(_.isOverdue()) =>
            error(current.state, MonitorTimeout)
          case (current: RunningState[IOState], OnNext(Tick)) =>
            current
          case (current: ShutdownState[IOState], _) =>
            ShutdownState(current.state)
        }
        .flatMap {
          case state: RunningState[IOState] =>
            state.actions
          case state: ShutdownState[IOState] =>
            state.actions
        }
        .dematerialize
    }

    private def next(transition: Behaviour[IOState, Event])(state: IOState, event: EventIn[Event]): MonitorState[IOState] = {
      transition(state)(event) match {
        case Goto(nextState, duration, action) =>
          nextState.output(action).timeout(duration)
        case Stay(action) =>
          state.output(action)
        case Stop(action) =>
          ShutdownState(state, actions = Observable(OnNext(Observe(action)), OnComplete))
      }
    }

    private def error(state: IOState, exn: Throwable): MonitorState[IOState] = {
      ShutdownState(state, actions = Observable(OnError(exn)))
    }

    private def complete(state: IOState): MonitorState[IOState] = {
      ShutdownState(state, actions = Observable(OnComplete))
    }
  }

  private implicit class EventInMap[Event](upstream: Observable[Event]) {
    def asEventInternal: Observable[EventInternal[Event]] = {
      upstream.map { event: Event =>
          Observe(event)
      }
    }
  }

  private implicit class TransitionBehaviour[IOState](state: IOState) {
    def output(emit: Option[Notify]): MonitorState[IOState] = emit match {
      case Some(action) =>
        RunningState(state, actions = Observable(OnNext(Observe[Notify](action))))
      case None =>
        RunningState(state)
    }
  }

  private implicit class TimeoutBehaviour[IOState](state: MonitorState[IOState]) {
    def timeout(timeout: Option[FiniteDuration]): MonitorState[IOState] = state match {
      case current: RunningState[IOState] =>
        current.copy(stateTimeout = timeout.map(Deadline(_)))
      case current: ShutdownState[IOState] =>
        current
    }

    def overall(timeout: Option[FiniteDuration]): MonitorState[IOState] = state match {
      case current: RunningState[IOState] =>
        current.copy(overallTimeout = timeout.map(Deadline(_)))
      case current: ShutdownState[IOState] =>
        current
    }
  }
}
