/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2025 microBean™.
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

import java.util.function.Supplier;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.TypeMirror;

import org.microbean.construct.PrimordialDomain;

import org.microbean.construct.type.UniversalType;

import static java.util.Objects.requireNonNull;

/**
 * An {@link AnnotationValue} implementation.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see AnnotationValue
 */
public final class UniversalAnnotationValue implements AnnotationValue {


  // Eventually this should become a lazy constant/stable value
  // volatile not needed
  private Supplier<? extends AnnotationValue> delegateSupplier;

  private final PrimordialDomain domain;

  private String s;

  private Object v;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link UniversalAnnotationValue}.
   *
   * @param delegate an {@link AnnotationValue} to which operations will be delegated; must not be {@code null}
   *
   * @param domain a {@link PrimordialDomain}; must not be {@code null}
   *
   * @exception NullPointerException if either argument is {@code null}
   */
  @SuppressWarnings("try")
  public UniversalAnnotationValue(final AnnotationValue delegate, final PrimordialDomain domain) {
    super();
    this.domain = requireNonNull(domain, "domain");
    final AnnotationValue unwrappedDelegate = unwrap(requireNonNull(delegate, "delegate"));
    if (unwrappedDelegate == delegate) {
      // No unwrapping happened so do symbol completion early; most common case
      this.delegateSupplier = () -> {
        try (var lock = domain.lock()) {
          // Should trigger symbol completion; see
          // https://github.com/openjdk/jdk/blob/jdk-25%2B3/src/jdk.compiler/share/classes/com/sun/tools/javac/util/Constants.java#L49;
          // any invocation of getTag() will do it.
          //
          // We bother to eagerly cache the value and the string representation because honestly you're going to call
          // those methods anyway, probably repeatedly.
          this.v = unwrappedDelegate.getValue();
          this.s = unwrappedDelegate.toString(); // names will do it too
          this.delegateSupplier = () -> unwrappedDelegate;
        }
        return unwrappedDelegate;
      };
    } else {
      assert delegate instanceof UniversalAnnotationValue;
      // Symbol completion already happened; no lock needed
      final UniversalAnnotationValue uav = (UniversalAnnotationValue)delegate;
      this.v = uav.getValue(); // already cached/computed
      this.s = uav.toString(); // already cached/computed
      this.delegateSupplier = () -> unwrappedDelegate;
    }
  }


  /*
   * Instance methods.
   */


  @Override // AnnotationValue
  @SuppressWarnings("unchecked")
  public final <R, P> R accept(final AnnotationValueVisitor<R, P> v, final P p) {
    return switch (this.getValue()) {
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

  /**
   * Returns the delegate to which operations are delegated.
   *
   * @return a non-{@code null} delegate
   *
   * @see AnnotationValue
   */
  public final AnnotationValue delegate() {
    final AnnotationValue delegate = this.delegateSupplier.get();
    assert !(delegate instanceof UniversalAnnotationValue);
    return delegate;
  }

  /**
   * Returns the {@link PrimordialDomain} supplied at construction time.
   *
   * @return the non-{@code null} {@link PrimordialDomain} supplied at construction time
   */
  public final PrimordialDomain domain() {
    return this.domain;
  }

  @Override // Object
  @SuppressWarnings("try")
  public final boolean equals(final Object other) {
    return this == other || switch (other) {
    case null -> false;
    // No lock needed; see
    // https://github.com/openjdk/jdk/blob/jdk-26%2B25/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Attribute.java#L45
    case UniversalAnnotationValue uav when this.getClass() == uav.getClass() -> this.delegate().equals(uav.delegate());
    default -> false;
    };
  }

  @Override // AnnotationValue
  @SuppressWarnings("unchecked")
  public final Object getValue() {
    final PrimordialDomain domain = this.domain();
    final Object value = this.v;
    return switch (value) {
    case null -> throw new AssertionError();
    case AnnotationMirror a -> UniversalAnnotation.of(a, domain);
    case List<?> l -> of((List<? extends AnnotationValue>)l, domain);
    case TypeMirror t -> UniversalType.of(t, domain);
    case VariableElement e -> UniversalElement.of(e, domain);
    default -> value;
    };
  }

  @Override // Object
  @SuppressWarnings({ "try", "unchecked" })
  public final int hashCode() {
    // No lock needed; see
    // https://github.com/openjdk/jdk/blob/jdk-26%2B25/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Attribute.java#L45
    return this.delegate().hashCode();
  }

  @Override // AnnotationValue
  @SuppressWarnings("try")
  public final String toString() {
    return this.s;
  }


  /*
   * Static methods.
   */


  /**
   * Returns a non-{@code null} {@link UniversalAnnotationValue} that is either the supplied {@link AnnotationValue} (if it
   * itself is {@code null} or is an {@link UniversalAnnotationValue}) or one that wraps it.
   *
   * @param av an {@link AnnotationValue}; may be {@code null}
   *
   * @param domain a {@link PrimordialDomain}; must not be {@code null}
   *
   * @return an {@link UniversalAnnotationValue}, or {@code null} (if {@code av} is {@code null})
   *
   * @exception NullPointerException if {@code domain} is {@code null}
   *
   * @see #UniversalAnnotationValue(AnnotationValue, PrimordialDomain)
   */
  public static final UniversalAnnotationValue of(final AnnotationValue av, final PrimordialDomain domain) {
    return switch (av) {
    case null -> null;
    case UniversalAnnotationValue uav -> uav;
    default -> new UniversalAnnotationValue(av, domain);
    };
  }

  /**
   * Returns a non-{@code null}, immutable {@link List} of {@link UniversalAnnotationValue}s whose elements wrap the
   * supplied {@link List}'s elements.
   *
   * @param avs a {@link Collection} of {@link AnnotationValue}s; must not be {@code null}
   *
   * @param domain a {@link PrimordialDomain}; must not be {@code null}
   *
   * @return a non-{@code null}, immutable {@link List} of {@link UniversalAnnotationValue}s
   *
   * @exception NullPointerException if either argument is {@code null}
   */
  public static final List<? extends UniversalAnnotationValue> of(final Collection<? extends AnnotationValue> avs,
                                                                  final PrimordialDomain domain) {
    if (avs.isEmpty()) {
      return List.of();
    }
    final List<UniversalAnnotationValue> newAvs = new ArrayList<>(avs.size());
    for (final AnnotationValue av : avs) {
      newAvs.add(UniversalAnnotationValue.of(av, domain));
    }
    return Collections.unmodifiableList(newAvs);
  }

  /**
   * <dfn>Unwraps</dfn> the supplied {@link AnnotationValue} implementation such that the returned value is not an
   * instance of {@link UniversalAnnotationValue}.
   *
   * @param a an {@link AnnotationValue}; may be {@code null}
   *
   * @return an {@link AnnotationValue} that is guaranteed not to be an instance of {@link UniversalAnnotationValue}
   *
   * @see #delegate()
   */
  public static final AnnotationValue unwrap(AnnotationValue a) {
    while (a instanceof UniversalAnnotationValue uav) {
      a = uav.delegate();
    }
    return a;
  }


}
