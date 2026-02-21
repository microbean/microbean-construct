/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2025–2026 microBean™.
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

import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodTypeDesc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import java.util.function.Supplier;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;

import org.microbean.construct.PrimordialDomain;

import org.microbean.construct.type.UniversalType;

import static java.lang.constant.ConstantDescs.BSM_INVOKE;

import static java.lang.constant.DirectMethodHandleDesc.Kind.STATIC;

import static java.lang.constant.MethodHandleDesc.ofMethod;

import static java.util.Objects.requireNonNull;

/**
 * An {@link AnnotationMirror} implementation.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see AnnotationMirror
 */
public final class UniversalAnnotation implements AnnotationMirror, Constable {


  /*
   * Instance fields.
   */


  // Eventually this should become a lazy constant/stable value
  // volatile not needed
  private Supplier<? extends AnnotationMirror> delegateSupplier;

  private final PrimordialDomain domain;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link UniversalAnnotation}.
   *
   * @param delegate an {@link AnnotationMirror} to which operations will be delegated; must not be {@code null}
   *
   * @param domain a {@link PrimordialDomain}; must not be {@code null}
   *
   * @exception NullPointerException if either argument is {@code null}
   */
  @SuppressWarnings("try")
  public UniversalAnnotation(final AnnotationMirror delegate, final PrimordialDomain domain) {
    super();
    this.domain = requireNonNull(domain, "domain");
    final AnnotationMirror unwrappedDelegate = unwrap(requireNonNull(delegate, "delegate"));
    if (unwrappedDelegate == delegate) {
      // No unwrapping happened so do symbol completion early; most common case
      this.delegateSupplier = () -> {
        try (var lock = domain.lock()) {
          unwrappedDelegate.getElementValues(); // should take care of any symbol completion
          this.delegateSupplier = () -> unwrappedDelegate;
        }
        return unwrappedDelegate;
      };
    } else {
      assert delegate instanceof UniversalAnnotation;
      // Symbol completion already happened
      this.delegateSupplier = () -> unwrappedDelegate;
    }
  }


  /*
   * Instance methods.
   */


  /**
   * Returns the delegate to which operations are delegated.
   *
   * @return a non-{@code null} delegate
   *
   * @see AnnotationMirror
   */
  public final AnnotationMirror delegate() {
    final AnnotationMirror delegate = this.delegateSupplier.get();
    assert !(delegate instanceof UniversalAnnotation);
    return delegate;
  }

  @Override // Constable
  public final Optional<DynamicConstantDesc<UniversalAnnotation>> describeConstable() {
    // TODO: this.delegate() is never going to be a Constable. It's debatable whether this class should implement
    // Constable at all.
    return (this.domain instanceof Constable c0 ? c0.describeConstable() : Optional.<ConstantDesc>empty())
      .flatMap(primordialDomainDesc -> (this.delegate() instanceof Constable c1 ? c1.describeConstable() : Optional.<ConstantDesc>empty())
               .map(delegateDesc -> DynamicConstantDesc.ofNamed(BSM_INVOKE,
                                                                this.getAnnotationType().asElement().getSimpleName().toString(),
                                                                UniversalAnnotation.class.describeConstable().orElseThrow(),
                                                                ofMethod(STATIC,
                                                                         UniversalAnnotation.class.describeConstable().orElseThrow(),
                                                                         "of",
                                                                         MethodTypeDesc.of(UniversalAnnotation.class.describeConstable().orElseThrow(),
                                                                                           AnnotationMirror.class.describeConstable().orElseThrow(),
                                                                                           PrimordialDomain.class.describeConstable().orElseThrow())),
                                                                delegateDesc,
                                                                primordialDomainDesc)));
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
    case UniversalAnnotation ar when this.getClass() == ar.getClass() -> this.delegate().equals(ar.delegate());
    default -> false;
    };
  }

  @Override // AnnotationMirror
  @SuppressWarnings("try")
  public final UniversalType getAnnotationType() {
    // No lock needed; see
    // https://github.com/openjdk/jdk/blob/jdk-26%2B25/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Attribute.java#L285-L288
    return UniversalType.of(this.delegate().getAnnotationType(), this.domain());
  }

  @Override // AnnotationMirror
  @SuppressWarnings("try")
  public final Map<? extends UniversalElement, ? extends UniversalAnnotationValue> getElementValues() {
    final Map<UniversalElement, UniversalAnnotationValue> map = new LinkedHashMap<>(17);
    final PrimordialDomain d = this.domain();
    // TODO: is this lock actually needed, given how delegateSupplier works?
    // try (var lock = d.lock()) {
      for (final Entry<? extends ExecutableElement, ? extends AnnotationValue> e : this.delegate().getElementValues().entrySet()) {
        map.put(UniversalElement.of(e.getKey(), d), UniversalAnnotationValue.of(e.getValue(), d));
      }
    // }
    return Collections.unmodifiableMap(map);
  }

  @Override // AnnotationMirror
  public final int hashCode() {
    // No lock needed; see
    // https://github.com/openjdk/jdk/blob/jdk-26%2B25/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Attribute.java#L45
    return this.delegate().hashCode();
  }


  /*
   * Static methods.
   */


  /**
   * Returns a non-{@code null} {@link UniversalAnnotation} that is either the supplied {@link AnnotationMirror} (if it
   * itself is an {@link UniversalAnnotation}) or one that wraps it.
   *
   * @param a an {@link AnnotationMirror}; must not be {@code null}
   *
   * @param d a {@link PrimordialDomain}; must not be {@code null}
   *
   * @return a non-{@code null} {@link UniversalAnnotation}
   *
   * @exception NullPointerException if either argument is {@code null}
   *
   * @see #UniversalAnnotation(AnnotationMirror, PrimordialDomain)
   */
  public static final UniversalAnnotation of(final AnnotationMirror a, final PrimordialDomain d) {
    return a instanceof UniversalAnnotation ar ? ar : new UniversalAnnotation(a, d);
  }

  /**
   * Returns a non-{@code null}, immutable {@link List} of {@link UniversalAnnotation}s whose elements wrap the supplied
   * {@link List}'s elements.
   *
   * @param as a {@link Collection} of {@link AnnotationMirror}s; must not be {@code null}
   *
   * @param domain a {@link PrimordialDomain}; must not be {@code null}
   *
   * @return a non-{@code null}, immutable {@link List} of {@link UniversalAnnotation}s
   *
   * @exception NullPointerException if either argument is {@code null}
   */
  public static final List<UniversalAnnotation> of(final Collection<? extends AnnotationMirror> as,
                                                   final PrimordialDomain domain) {
    if (as.isEmpty()) {
      return List.of();
    }
    final List<UniversalAnnotation> newAs = new ArrayList<>(as.size());
    for (final AnnotationMirror a : as) {
      newAs.add(UniversalAnnotation.of(a, domain));
    }
    return Collections.unmodifiableList(newAs);
  }

  /**
   * <dfn>Unwraps</dfn> the supplied {@link AnnotationMirror} implementation such that the returned value is not an
   * instance of {@link UniversalAnnotation}.
   *
   * @param a an {@link AnnotationMirror}; may be {@code null}
   *
   * @return an {@link AnnotationMirror} that is guaranteed not to be an instance of {@link UniversalAnnotation}
   *
   * @see #delegate()
   */
  public static final AnnotationMirror unwrap(AnnotationMirror a) {
    while (a instanceof UniversalAnnotation ua) {
      a = ua.delegate();
    }
    return a;
  }


}
