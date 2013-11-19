/*
  Copyright 2013 Tomer Gabel

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */


package com.tomergabel.accord.combinators

import com.tomergabel.accord.{Violation, Validator}

/**
 * A useful helper class when validating numerical properties (size, length, arity...). Provides the usual
 * arithmetic operators in a consistent manner across all numerical property combiners. Violation messages
 * are structured based on the provided snippet and the property/constraint values, for example:
 *
 * ```
 * class StringLength extends NumericPropertyWrapper[ String, Int, String ]( _.length, "has length" )
 *
 * val result = ( new StringLegnth < 16 ) apply "This text is too long"
 * // result is a Failure; violation text: "has length 21, expected 16 or less".
 * ```
 *
 * Additionally provides a representation type `Repr` to shortcut type inference; for an expression like
 * `c.students has size > 0`, the object under validation is `c.students` but its inferred type isn't resolved
 * until the leaf node of the expression tree (in this case, the method call `> 0`). That means all constraints,
 * view bounds and the like have to exist at the leaf node, or in other words on the method call itself; to
 * generalize this, each arithmetic method requires an implicit `T => Repr` conversion, and because `Repr`
 * is specified by whomever instantiates [[com.tomergabel.accord.combinators.NumericPropertyWrapper]] the
 * view bound is placed correctly at the call site. (*whew*, hope that made sense)
 *
 * @param extractor A function which extracts the property value from the representation of the object under
 *                  validation (e.g. `( p: String ) => p.length`)
 * @param snippet A textual snippet describing what the validator does (e.g. `has length`)
 * @param ev Evidence that the property is of a numeric type (normally filled in by the compiler automatically)
 * @tparam T The type of the object under validation
 * @tparam P The type of the property under validation
 * @tparam Repr The runtime representation of the object under validation. When it differs from `T`, an implicit
 *              conversion `T => Repr` is required ''at the call site''.
 */
abstract class NumericPropertyWrapper[ T, P, Repr ]( extractor: Repr => P, snippet: String )( implicit ev: Numeric[ P ] ) {
  /** Generates a validator that succeeds only if the property value is greater than the specified bound. */
  def >( other: P )( implicit repr: T => Repr ) = new Validator[ T ] {
    def apply( x: T ) = {
      val v = ( repr andThen extractor )( x )
      result( ev.gt( v, other ), Violation( s"$snippet $v, expected more than $other", x ) )
    }
  }
  /** Generates a validator that succeeds only if the property value is less than the specified bound. */
  def <( other: P )( implicit repr: T => Repr ) = new Validator[ T ] {
    def apply( x: T ) = {
      val v = ( repr andThen extractor )( x )
      result( ev.lt( v, other ), Violation( s"$snippet $v, expected less than $other", x ) )
    }
  }
  /** Generates a validator that succeeds if the property value is greater than or equal to the specified bound. */
  def >=( other: P )( implicit repr: T => Repr ) = new Validator[ T ] {
    def apply( x: T ) = {
      val v = ( repr andThen extractor )( x )
      result( ev.gteq( v, other ), Violation( s"$snippet $v, expected $other or more", x ) )
    }
  }
  /** Generates a validator that succeeds if the property value is less than or equal to the specified bound. */
  def <=( other: P )( implicit repr: T => Repr ) = new Validator[ T ] {
    def apply( x: T ) = {
      val v = ( repr andThen extractor )( x )
      result( ev.lteq( v, other ), Violation( s"$snippet $v, expected $other or less", x ) )
    }
  }
}

