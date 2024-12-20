/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2024 microBean™.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.TypeMirror;

import org.microbean.construct.Domain;

import org.microbean.construct.type.UniversalType;

/**
 * An {@link AnnotationValue} implementation.
 *
 * @param delegate an {@link AnnotationValue} to which operations will be delegated; must not be {@code null}
 *
 * @param domain a {@link Domain}; must not be {@code null}
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see AnnotationValue
 */
public final record AnnotationValueRecord(AnnotationValue delegate, Domain domain) implements AnnotationValue {


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link AnnotationValueRecord}.
   *
   * @param delegate an {@link AnnotationValue} to which operations will be delegated; must not be {@code null}
   *
   * @param domain a {@link Domain}; must not be {@code null}
   *
   * @exception NullPointerException if either argument is {@code null}
   */
  public AnnotationValueRecord {
    Objects.requireNonNull(delegate, "delegate");
    Objects.requireNonNull(domain, "domain");
  }


  /*
   * Instance methods.
   */


  @Override // AnnotationValue
  @SuppressWarnings({ "try", "unchecked" })
  public final <R, P> R accept(final AnnotationValueVisitor<R, P> v, final P p) {
    try (var lock = this.domain().lock()) {
      return switch (this.getValue()) {
      case null -> v.visitUnknown(this, p); // ...or AssertionError?
      case AnnotationMirror a -> v.visitAnnotation(a, p);
      case List<?> l -> v.visitArray((List<? extends AnnotationValue>)l, p);
      case TypeMirror t -> v.visitType(t, p);
      case VariableElement e -> v.visitEnumConstant(e, p);
      case Boolean b -> v.visitBoolean(b, p);
      case Byte b -> v.visitByte(b, p);
      case Character c -> v.visitChar(c, p);
      case Double d -> v.visitDouble(d, p);
      case Float f -> v.visitFloat(f, p);
      case Integer i -> v.visitInt(i, p);
      case Long l -> v.visitLong(l, p);
      case Short s -> v.visitShort(s, p);
      case String s -> v.visitString(s, p);
      default -> v.visitUnknown(this, p);
      };
    }
  }

  @Override // Object
  public final boolean equals(final Object other) {
    return this == other || switch (other) {
    case null -> false;
    case AnnotationValue av -> {
      try (var lock = this.domain().lock()) {
        // The mere act of getting a value (even of type String) can trigger symbol completion:
        // https://github.com/openjdk/jdk/blob/jdk-25%2B3/src/jdk.compiler/share/classes/com/sun/tools/javac/util/Constants.java#L49
        yield this.delegate().equals(av instanceof AnnotationValueRecord avr ? avr.delegate() : av);
      }
    }
    default -> false;
    };
  }

  @Override // AnnotationValue
  @SuppressWarnings({ "try", "unchecked" })
  public final Object getValue() {
    final Domain domain = this.domain();
    final Object value;
    try (var lock = domain.lock()) {
      // The mere act of getting a value (even of type String) can trigger symbol completion:
      // https://github.com/openjdk/jdk/blob/jdk-25%2B3/src/jdk.compiler/share/classes/com/sun/tools/javac/util/Constants.java#L49
      value = this.delegate().getValue();
    }
    return switch (value) {
    case null -> throw new AssertionError();
    case AnnotationMirror a -> AnnotationRecord.of(a, domain);
    case List<?> l -> of((List<? extends AnnotationValue>)l, domain);
    case TypeMirror t -> UniversalType.of(t, domain);
    case VariableElement e -> UniversalElement.of(e, domain);
    default -> value;
    };
  }

  @Override // Object
  public final int hashCode() {
    return this.getValue().hashCode();
  }

  @Override // AnnotationValue
  @SuppressWarnings("try")
  public final String toString() {
    try (var lock = this.domain().lock()) {
      // Can cause symbol completion and/or Name#toString() calls
      return this.delegate().toString();
    }
  }


  /*
   * Static methods.
   */


  /**
   * Returns a non-{@code null} {@link AnnotationValueRecord} that is either the supplied {@link AnnotationValue} (if it
   * itself is {@code null} or is an {@link AnnotationValueRecord}) or one that wraps it.
   *
   * @param av an {@link AnnotationValue}; may be {@code null}
   *
   * @param domain a {@link Domain}; must not be {@code null}
   *
   * @return an {@link AnnotationValueRecord}, or {@code null} (if {@code av} is {@code null})
   *
   * @exception NullPointerException if {@code domain} is {@code null}
   *
   * @see #AnnotationValueRecord(AnnotationValue, Domain)
   */
  public static final AnnotationValueRecord of(final AnnotationValue av, final Domain domain) {
    return switch (av) {
    case null -> null;
    case AnnotationValueRecord avr -> avr;
    default -> new AnnotationValueRecord(av, domain);
    };
  }

  /**
   * Returns a non-{@code null}, immutable {@link List} of {@link AnnotationValueRecord}s whose elements wrap the
   * supplied {@link List}'s elements.
   *
   * @param avs a {@link Collection} of {@link AnnotationValue}s; must not be {@code null}
   *
   * @param domain a {@link Domain}; must not be {@code null}
   *
   * @return a non-{@code null}, immutable {@link List} of {@link AnnotationValueRecord}s
   *
   * @exception NullPointerException if either argument is {@code null}
   */
  public static final List<? extends AnnotationValueRecord> of(final Collection<? extends AnnotationValue> avs,
                                                               final Domain domain) {
    if (avs.isEmpty()) {
      return List.of();
    }
    final List<AnnotationValueRecord> newAvs = new ArrayList<>(avs.size());
    for (final AnnotationValue av : avs) {
      newAvs.add(AnnotationValueRecord.of(av, domain));
    }
    return Collections.unmodifiableList(newAvs);
  }


}
