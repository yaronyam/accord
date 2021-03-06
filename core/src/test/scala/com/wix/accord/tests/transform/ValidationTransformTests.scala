/*
  Copyright 2013-2015 Wix.com

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

package com.wix.accord.tests.transform

import org.scalatest.{Matchers, WordSpec}
import com.wix.accord._
import com.wix.accord.scalatest.ResultMatchers

class ValidationTransformTests extends WordSpec with Matchers with ResultMatchers {
  "Validator description" should {
    import ValidationTransformTests._

    "be generated for a fully-qualified field selector" in {
      validate( FlatTest( null ) )( implicitlyDescribedNamedValidator ) should failWith( "field" -> "is a null" )
    }
    "be generated for an anonymously-qualified field selector" in {
      validate( FlatTest( null ) )( implicitlyDescribedAnonymousValidator ) should failWith( "field" -> "is a null" )
    }
    "be generated for an anonymous value reference" in {
      validate( null )( implicitlyDescribedValueValidator ) should failWith( "value" -> "is a null" )
    }
    "be generated for a fully-qualified selector with multiple indirections" in {
      val obj = CompositeTest( FlatTest( null ) )
      validate( obj )( namedIndirectValidator ) should failWith( "member.field" -> "is a null" )
    }
    "be generated for an anonymously-qualified selector with multiple indirections" in {
      val obj = CompositeTest( FlatTest( null ) )
      validate( obj )( anonymousIndirectValidator ) should failWith( "member.field" -> "is a null" )
    }
    "be generated for a multiple-clause boolean expression" in {
      val obj = FlatTest( "123" )
      validate( obj )( booleanExpressionValidator ) should failWith(
        group( null, "doesn't meet any of the requirements",
          "field" -> "is not a null",
          "field" -> "has size 3, expected more than 5"
        ) )
    }
    "be propagated for an explicitly-described expression" in {
      validate( FlatTest( null ) )( explicitlyDescribedValidator ) should failWith( "described" -> "is a null" )
    }
    "be propagated for a composite validator" in {
      val obj = CompositeTest( FlatTest( null ) )
      validate( obj )( compositeValidator ) should failWith( group( "member", "is invalid", "field" -> "is a null" ) )
    }
    "be propagated for an adapted validator" in {
      validate( FlatTest( null ) )( adaptedValidator ) should failWith( "field" -> "is a null" )
    }
  }

  "Validation block transformation" should {

    "safely ignore statements of type Nothing" in {
      """
      import com.wix.accord.dsl._

      val v = validator[ String ] { s => throw new Exception() }
      """ should compile
    }

    "safely ignore statements of type Null" in {
      """
      import com.wix.accord.dsl._

      def _null: Null = null
      val v = validator[ String ] { s => _null }
      """ should compile
    }
  }
}

object ValidationTransformTests {
  import dsl._
  
  case class FlatTest( field: String )
  val implicitlyDescribedNamedValidator = validator[ FlatTest ] { t => t.field is notNull }
  val implicitlyDescribedAnonymousValidator = validator[ FlatTest ] { _.field is notNull }
  val explicitlyDescribedValidator = validator[ FlatTest ] { t => t.field as "described" is notNull }
  val implicitlyDescribedValueValidator = validator[ String ] { _ is notNull }
  val adaptedValidator = implicitlyDescribedValueValidator compose { ( f: FlatTest ) => f.field }
  val booleanExpressionValidator = validator[ FlatTest ] { t => ( t.field is aNull ) or ( t.field has size > 5 ) }

  case class CompositeTest( member: FlatTest )
  val compositeValidator = {
    implicit val flatValidator = implicitlyDescribedAnonymousValidator
    validator[ CompositeTest ] { _.member is valid }
  }
  val namedIndirectValidator = validator[ CompositeTest ] { c => c.member.field is notNull }
  val anonymousIndirectValidator = validator[ CompositeTest ] { _.member.field is notNull }
}