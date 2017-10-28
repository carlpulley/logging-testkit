// Copyright 2016 Carl Pulley

package net.cakesolutions.testkit

import monix.reactive.Observable
import org.atnos.eff.Eff

package object monitor {
  type Behaviour[IOState, Event] = PartialFunction[IOState, PartialFunction[ObservedEvent[Event], Action[IOState]]]

  implicit class PropositionalLogic[Model](left: Eff[Model, Observable[Notify]]) {
    def &&(right: Eff[Model, Observable[Notify]]): Eff[Model, Observable[Notify]] = {
      for {
        l <- left
        r <- right
      } yield l.zip(r).map {
        case (Accept(leftFailures@_*), Accept(rightFailures@_*)) =>
          Accept(leftFailures ++ rightFailures: _*)
        case (Fail(leftFailures@_*), Accept(rightFailures@_*)) =>
          Fail(leftFailures ++ rightFailures: _*)
        case (Accept(leftFailures@_*), Fail(rightFailures@_*)) =>
          Fail(leftFailures ++ rightFailures: _*)
        case (Fail(leftFailures@_*), Fail(rightFailures@_*)) =>
          Fail(leftFailures ++ rightFailures: _*)
      }
    }

    def ||(right: Eff[Model, Observable[Notify]]): Eff[Model, Observable[Notify]] = {
      for {
        l <- left
        r <- right
      } yield l.zip(r).map {
        case (Accept(leftFailures@_*), Accept(rightFailures@_*)) =>
          Accept(leftFailures ++ rightFailures: _*)
        case (Fail(leftFailures@_*), Accept(rightFailures@_*)) =>
          Accept(leftFailures ++ rightFailures: _*)
        case (Accept(leftFailures@_*), Fail(rightFailures@_*)) =>
          Accept(leftFailures ++ rightFailures: _*)
        case (Fail(leftFailures@_*), Fail(rightFailures@_*)) =>
          Fail(leftFailures ++ rightFailures: _*)
      }
    }

    def negate: Eff[Model, Observable[Notify]] = {
      left.map(_.map(_.invert))
    }
  }
}
