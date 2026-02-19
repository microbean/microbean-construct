/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2026 microBean™.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.microbean.construct.element;

import java.util.List;
import java.util.Map;

import java.util.function.Predicate;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.TypeMirror;

import javax.lang.model.util.AbstractAnnotationValueVisitor14;

import static javax.lang.model.element.ElementKind.METHOD;

/**
 * An {@link AbstractAnnotationValueVisitor14} that computes a hashcode for an {@link AnnotationValue}, emulating as
 * closely as possible the rules described by the {@link java.lang.annotation.Annotation#hashCode()} contract.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see SameAnnotationValueVisitor
 */
// Goal is to emulate as much as possible the rules from java.lang.annotation.Annotation which capture what it means for
// two annotations to be "the same", and read in part as follows:
//
//   The hash code of an annotation is the sum of the hash codes of its members (including those with default values). The
//   hash code of an annotation member is (127 times the hash code of the member-name as computed by String.hashCode())
//   XOR the hash code of the member-value. The hash code of a member-value depends on its type as defined below:
//
//     * The hash code of a primitive value v is equal to WrapperType.valueOf(v).hashCode() [or, equivalently,
//       WrapperType.hashCode(v)], where WrapperType is the wrapper type corresponding to the primitive type of v (Byte,
//       Character, Double, Float, Integer, Long, Short, or Boolean).
//
//     * The hash code of a string, enum, class, or annotation member-value v is computed as by calling v.hashCode(). (In
//       the case of annotation member values, this is a recursive definition.)
//
//     * The hash code of an array member-value is computed by calling the appropriate overloading of Arrays.hashCode on
//       the value. (There is one overloading for each primitive type, and one for object reference types.)
//
public final class AnnotationValueHashcodeVisitor extends AbstractAnnotationValueVisitor14<Integer, Predicate<? super ExecutableElement>> {

  /**
   * Creates a new {@link AnnotationValueHashcodeVisitor}.
   */
  public AnnotationValueHashcodeVisitor() {
    super();
  }

  @Override // AbstractAnnotationValueVisitor14q
  public final Integer visitAnnotation(final AnnotationMirror am0, Predicate<? super ExecutableElement> p) {
    if (am0 == null) {
      return 0;
    }
    if (p == null) {
      p = ee -> true;
    }
    int hashCode = 0;
    final Map<? extends ExecutableElement, ? extends AnnotationValue> explicitValues = am0.getElementValues();
    for (final Element e : am0.getAnnotationType().asElement().getEnclosedElements()) {
      if (e.getKind() == METHOD && e instanceof ExecutableElement ee && p.test(ee)) {
        final AnnotationValue v = explicitValues.containsKey(ee) ? explicitValues.get(ee) : ee.getDefaultValue();
        // An annotation member value is either explicit or default but cannot be null.
        assert v != null : "v == null; ee: " + ee;
        // "The hash code of an annotation is the sum of the hash codes of its members (including those with default values). The
        // hash code of an annotation member is (127 times the hash code of the member-name as computed by String.hashCode())
        // XOR the hash code of the member-value."
        hashCode += ((127 * ee.getSimpleName().toString().hashCode()) ^ this.visit(v, p).intValue());
      }
    }
    return hashCode;
  }

  @Override // AbstractAnnotationValueVisitor14
  public final Integer visitArray(final List<? extends AnnotationValue> l0, final Predicate<? super ExecutableElement> ignored) {
    // "The hash code of an array[-typed annotation] member-value is computed by calling the appropriate overloading of
    // Arrays.hashCode on the value. (There is one overloading for each primitive type, and one for object reference
    // types.)"
    //
    // More cumbersome than you might think. Somewhat conveniently, in general Arrays.hashCode(something) will perform
    // the same calculation as an equivalent List. So perform the calculation on a "de-AnnotationValueized" List.
    //
    // (Implementation note: in the JDK as of this writing, ArraySupport will do fancy stuff with vectors, which
    // presumably List's hashCode calculations do not. If this ends up being some kind of hot spot, we could turn the
    // List into an array appropriately.)
    return l0.stream().map(AnnotationValue::getValue).toList().hashCode();
  }

  @Override // AbstractAnnotationValueVisitor14
  public final Integer visitBoolean(final boolean b0, final Predicate<? super ExecutableElement> ignored) {
    // "The hash code of a primitive value v is equal to WrapperType.valueOf(v).hashCode(), where WrapperType is the
    // wrapper type corresponding to the primitive type of v (Byte, Character, Double, Float, Integer, Long, Short, or
    // Boolean)."
    return Boolean.hashCode(b0);
  }

  @Override // AbstractAnnotationValueVisitor14
  public final Integer visitByte(final byte b0, final Predicate<? super ExecutableElement> ignored) {
    // "The hash code of a primitive value v is equal to WrapperType.valueOf(v).hashCode(), where WrapperType is the
    // wrapper type corresponding to the primitive type of v (Byte, Character, Double, Float, Integer, Long, Short, or
    // Boolean)."
    return Byte.hashCode(b0);
  }

  @Override // AbstractAnnotationValueVisitor14
  public final Integer visitChar(final char c0, final Predicate<? super ExecutableElement> ignored) {
    // "The hash code of a primitive value v is equal to WrapperType.valueOf(v).hashCode(), where WrapperType is the
    // wrapper type corresponding to the primitive type of v (Byte, Character, Double, Float, Integer, Long, Short, or
    // Boolean)."
    return Character.hashCode(c0);
  }

  @Override // AbstractAnnotationValueVisitor14
  public final Integer visitDouble(final double d0, final Predicate<? super ExecutableElement> ignored) {
    // "The hash code of a primitive value v is equal to WrapperType.valueOf(v).hashCode(), where WrapperType is the
    // wrapper type corresponding to the primitive type of v (Byte, Character, Double, Float, Integer, Long, Short, or
    // Boolean)."
    return Double.hashCode(d0);
  }

  @Override // AbstractAnnotationValueVisitor14
  public final Integer visitEnumConstant(final VariableElement ve0, final Predicate<? super ExecutableElement> ignored) {
    // "The hash code of a string, enum, class, or annotation member-value v is computed as by calling v.hashCode(). (In
    // the case of annotation member values, this is a recursive definition.)"
    return ve0 == null ? 0 : ve0.hashCode();
  }

  @Override // AbstractAnnotationValueVisitor14
  public final Integer visitFloat(final float f0, final Predicate<? super ExecutableElement> ignored) {
    // "The hash code of a primitive value v is equal to WrapperType.valueOf(v).hashCode(), where WrapperType is the
    // wrapper type corresponding to the primitive type of v (Byte, Character, Double, Float, Integer, Long, Short, or
    // Boolean)."
    return Float.hashCode(f0);
  }

  @Override // AbstractAnnotationValueVisitor14
  public final Integer visitInt(final int i0, final Predicate<? super ExecutableElement> ignored) {
    // "The hash code of a primitive value v is equal to WrapperType.valueOf(v).hashCode(), where WrapperType is the
    // wrapper type corresponding to the primitive type of v (Byte, Character, Double, Float, Integer, Long, Short, or
    // Boolean)."
    return Integer.hashCode(i0);
  }

  @Override // AbstractAnnotationValueVisitor14
  public final Integer visitLong(final long l0, final Predicate<? super ExecutableElement> ignored) {
    // "The hash code of a primitive value v is equal to WrapperType.valueOf(v).hashCode(), where WrapperType is the
    // wrapper type corresponding to the primitive type of v (Byte, Character, Double, Float, Integer, Long, Short, or
    // Boolean)."
    return Long.hashCode(l0);
  }

  @Override // AbstractAnnotationValueVisitor14
  public final Integer visitShort(final short s0, final Predicate<? super ExecutableElement> ignored) {
    // "The hash code of a primitive value v is equal to WrapperType.valueOf(v).hashCode(), where WrapperType is the
    // wrapper type corresponding to the primitive type of v (Byte, Character, Double, Float, Integer, Long, Short, or
    // Boolean)."
    return Short.hashCode(s0);
  }

  @Override // AbstractAnnotationValueVisitor14
  public final Integer visitString(final String s0, final Predicate<? super ExecutableElement> ignored) {
    // "The hash code of a string, enum, class, or annotation member-value v is computed as by calling v.hashCode(). (In
    // the case of annotation member values, this is a recursive definition.)"
    return s0 == null ? 0 : s0.hashCode();
  }

  @Override // AbstractAnnotationValueVisitor14
  public final Integer visitType(final TypeMirror t0, final Predicate<? super ExecutableElement> ignored) {
    // "The hash code of a string, enum, class, or annotation member-value v is computed as by calling v.hashCode(). (In
    // the case of annotation member values, this is a recursive definition.)"
    return t0 == null ? 0 : t0.hashCode();
  }

}
