package dev.insideyou
package zionutshell

import scala.language.adhocExtensions

import org.scalatest.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

export org.scalacheck.{ Arbitrary, Gen }
export org.scalatest.compatible.Assertion

trait TestSuite
    extends AnyFunSuite,
            should.Matchers,
            GivenWhenThen,
            BeforeAndAfterAll,
            BeforeAndAfterEach,
            ScalaCheckPropertyChecks
