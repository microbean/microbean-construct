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

import java.lang.constant.ClassDesc;
import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DynamicConstantDesc;

import java.util.List;
import java.util.Optional;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.TypeMirror;

import org.microbean.construct.constant.Constables;

import static java.lang.constant.ConstantDescs.BSM_INVOKE;
import static java.lang.constant.ConstantDescs.CD_Object;

import static java.lang.constant.MethodHandleDesc.ofConstructor;

/**
 * An <strong>experimental</strong> {@link AnnotationValue} implementation that is partially or wholly synthetic.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 */
public final class SyntheticAnnotationValue implements AnnotationValue, Constable {


  /*
   * Instance fields.
   */


  // Will be one of:
  //
  // * AnnotationMirror
  // * List<SyntheticAnnotationValue>
  // * TypeMirror (DeclaredType, PrimitiveType)
  // * VariableElement (ENUM_CONSTANT)
  // * Boolean
  // * Byte
  // * Character
  // * Double
  // * Float
  // * Integer
  // * Long
  // * Short
  // * String
  private final Object value;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link SyntheticAnnotationValue}.
   *
   * @param value a value legal for an {@link AnnotationValue}; must not be {@code null}
   *
   * @exception NullPointerException if {@code value} is {@code null}
   *
   * @exception IllegalArgumentException if {@code value} is not legal for an {@link AnnotationValue}
   *
   * @see AnnotationValue
   */
  public SyntheticAnnotationValue(final Object value) {
    super();
    this.value = value(value);
  }


  /*
   * Instance methods.
   */


  @Override // AnnotationValue
  @SuppressWarnings("unchecked")
  public final <R, P> R accept(final AnnotationValueVisitor<R, P> v, final P p) {
    return switch (this.value) {
    case null               -> v.visitUnknown(this, p); // ...or AssertionError?
    case AnnotationMirror a -> v.visitAnnotation(a, p);
    case List<?> l          -> v.visitArray((List<? extends AnnotationValue>)l, p);
    case TypeMirror t       -> v.visitType(t, p);
    case VariableElement e  -> v.visitEnumConstant(e, p);
    case Boolean b          -> v.visitBoolean(b, p);
    case Byte b             -> v.visitByte(b, p);
    case Character c        -> v.visitChar(c, p);
    case Double d           -> v.visitDouble(d, p);
    case Float f            -> v.visitFloat(f, p);
    case Integer i          -> v.visitInt(i, p);
    case Long l             -> v.visitLong(l, p);
    case Short s            -> v.visitShort(s, p);
    case String s           -> v.visitString(s, p);
    default                 -> v.visitUnknown(this, p);
    };
  }

  @Override // Constable
  public final Optional<DynamicConstantDesc<SyntheticAnnotationValue>> describeConstable() {
    final Optional<? extends ConstantDesc> valueDescOptional = switch (this.value) {
    case Constable c -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd);
    case List<?> l -> Constables.describe(l);
    default -> Optional.<ConstantDesc>empty();
    };
    return valueDescOptional.map(valueDesc -> DynamicConstantDesc.ofNamed(BSM_INVOKE,
                                                                          this.getClass().getSimpleName(),
                                                                          this.getClass().describeConstable().orElseThrow(),
                                                                          ofConstructor(this.getClass().describeConstable().orElseThrow(),
                                                                                        CD_Object),
                                                                          valueDesc));
  }

  @Override // Object
  public final boolean equals(final Object other) {
    return this == other || switch (other) {
    case null -> false;
    case SyntheticAnnotationValue sav when this.getClass() == sav.getClass() -> this.value.equals(sav.value);
    default -> false;
    };
  }

  @Override // AnnotationValue
  public final Object getValue() {
    return this.value;
  }

  @Override // Object
  public final int hashCode() {
    return this.value.hashCode();
  }

  @Override // Object
  public final String toString() {
    return this.value.toString();
  }


  /*
   * Static methods.
   */


  /**
   * Returns a non-{@code null}, determinate {@link SyntheticAnnotationValue} that represents the supplied {@code
   * value}.
   *
   * <p>If {@code value} is a {@link SyntheticAnnotationValue}, then it is returned unchanged.</p>
   *
   * @param value a value legal for an {@link AnnotationValue}; must not be {@code null}
   *
   * @return a non-{@code null}, determinate {@link SyntheticAnnotationValue}
   *
   * @exception NullPointerException if {@code value} is {@code null}
   *
   * @exception IllegalArgumentException if {@code value} is not legal for an {@link AnnotationValue}
   *
   * @see AnnotationValue
   */
  public static final SyntheticAnnotationValue of(final Object value) {
    return switch (value) {
    case null -> throw new NullPointerException("value");
    case SyntheticAnnotationValue sav -> sav;
    default -> new SyntheticAnnotationValue(value);
    };
  }

  private static final Object value(final Object value) {
    return switch (value) {
    case null               -> throw new NullPointerException("value");

    case AnnotationValue av -> av.getValue(); // not part of the spec; just good hygiene

    case List<?> l          -> l.stream().map(SyntheticAnnotationValue::new).toList();

    case TypeMirror t       -> switch (t.getKind()) {
    case ARRAY, BOOLEAN, BYTE, CHAR, DECLARED, DOUBLE, FLOAT, INT, LONG, SHORT, VOID -> t;
    default -> throw new IllegalArgumentException("value: " + value);
    };

    case VariableElement e  -> switch (e.getKind()) {
    case ENUM_CONSTANT -> e;
    default -> throw new IllegalArgumentException("value: " + value);
    };

    case AnnotationMirror a -> a;
    case Boolean b          -> b;
    case Byte b             -> b;
    case Character c        -> c;
    case Double d           -> d;
    case Float f            -> f;
    case Integer i          -> i;
    case Long l             -> l;
    case Short s            -> s;
    case String s           -> s;

    default                 -> throw new IllegalArgumentException("value: " + value);
    };
  }

}
