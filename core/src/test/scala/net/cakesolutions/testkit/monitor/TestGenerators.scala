// Copyright 2018 Carl Pulley

package net.cakesolutions.testkit.monitor

import scala.concurrent.duration.Duration

import org.scalacheck.Gen

object TestGenerators {

  val acceptGen: Gen[Notify] = for {
    failures <- Gen.listOf(Gen.alphaNumStr)
  } yield Accept(failures: _*)

  val failGen: Gen[Fail] = for {
    failures <- Gen.listOf(Gen.alphaNumStr)
  } yield Fail(failures: _*)

  val notifyGen: Gen[Notify] = Gen.frequency(
    1 -> acceptGen,
    1 -> failGen
  )

  def gotoGen[S](stateGen: Gen[S]): Gen[Goto[S]] = for {
    state <- stateGen
    forMax <- Gen.option(Gen.choose(Long.MinValue + 1, Long.MaxValue).map(Duration.fromNanos))
    emit <- Gen.option(notifyGen)
  } yield new Goto[S](state, forMax, emit)

  def stayGen[S]: Gen[Stay[S]] = for {
    emit <- Gen.option(notifyGen)
  } yield Stay[S](emit)

  def stopGen[S]: Gen[Stop[S]] = for {
    toEmit <- notifyGen
  } yield Stop[S](toEmit)

  def actionGen[S](stateGen: Gen[S]): Gen[Action[S]] = Gen.frequency(
    1 -> stopGen[S],
    4 -> stayGen[S],
    5 -> gotoGen[S](stateGen)
  )

  val stateTimeoutGen: Gen[StateTimeout.type] = Gen.const(StateTimeout)

  def observeGen[E](eventGen: Gen[E]): Gen[Observe[E]] = for {
    event <- eventGen
  } yield Observe[E](event)

  def observedEventGen[E](eventGen: Gen[E]): Gen[ObservedEvent[E]] = Gen.frequency(
    1 -> stateTimeoutGen,
    9 -> observeGen[E](eventGen)
  )
}
