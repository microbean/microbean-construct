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
package org.microbean.construct;

import java.lang.annotation.Annotation;

import java.lang.constant.ClassDesc;
import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import java.util.function.Supplier;

import javax.lang.model.AnnotatedConstruct;

import javax.lang.model.element.Element;

import javax.lang.model.type.TypeMirror;

import org.microbean.construct.constant.Constables;

import org.microbean.construct.element.AnnotationRecord;
import org.microbean.construct.element.UniversalElement;

import org.microbean.construct.type.UniversalType;

import static java.lang.constant.ConstantDescs.BSM_INVOKE;

/**
 * An abstract implementation of {@link AnnotatedConstruct} from which only {@link UniversalElement} and {@link
 * UniversalType} descend.
 *
 * @param <T> a type of {@link AnnotatedConstruct}, which may be only either {@link Element} or {@link TypeMirror}
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see AnnotatedConstruct
 *
 * @see UniversalElement
 *
 * @see UniversalType
 */
public abstract sealed class UniversalConstruct<T extends AnnotatedConstruct> implements AnnotatedConstruct, Constable
  permits UniversalElement, UniversalType {


  /*
   * Instance fields.
   */


  private final Domain domain;

  // volatile not needed
  private Supplier<? extends T> delegateSupplier;

  private volatile String s;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link AnnotatedConstruct}.
   *
   * @param delegate a delegate to which operations are delegated; must not be {@code null}
   *
   * @param domain; a {@link Domain} from which the supplied {@code delegate} is presumed to have originated; must not
   * be {@code null}
   *
   * @exception NullPointerException if either argument is {@code null}
   */
  @SuppressWarnings("try")
  protected UniversalConstruct(final T delegate, final Domain domain) {
    super();
    this.domain = Objects.requireNonNull(domain, "domain");
    final T unwrappedDelegate = unwrap(Objects.requireNonNull(delegate, "delegate"));
    final Runnable symbolCompleter = switch (unwrappedDelegate) {
    case null -> throw new IllegalArgumentException("delegate: " + delegate);
    case Element e -> e::getKind;
    case TypeMirror t -> t::getKind;
    default -> UniversalConstruct::doNothing;
    };
    this.delegateSupplier = () -> {
      try (var lock = domain.lock()) {
        symbolCompleter.run();
        this.delegateSupplier = () -> unwrappedDelegate;
      }
      return unwrappedDelegate;
    };
  }


  /*
   * Instance methods.
   */


  /**
   * Returns the delegate to which operations are delegated.
   *
   * @return a non-{@code null} delegate
   *
   * @see Element
   *
   * @see TypeMirror
   */
  public final T delegate() {
    return this.delegateSupplier.get();
  }

  @Override // Constable
  public final Optional<? extends ConstantDesc> describeConstable() {
    final T delegate = this.delegate();
    final Domain domain = this.domain();
    return Constables.describe(delegate, domain)
      .map(delegateDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                  MethodHandleDesc.ofConstructor(ClassDesc.of(this.getClass().getName()),
                                                                                 ClassDesc.of(delegate instanceof TypeMirror ? TypeMirror.class.getName() : Element.class.getName()),
                                                                                 ClassDesc.of(Domain.class.getName())),
                                                   delegateDesc,
                                                   ((Constable)domain).describeConstable().orElseThrow()));
  }

  /**
   * Returns the {@link Domain} supplied at construction time.
   *
   * @return the non-{@code null} {@link Domain} supplied at construction time
   */
  public final Domain domain() {
    return this.domain;
  }

  @Override // AnnotatedConstruct
  public final List<? extends AnnotationRecord> getAnnotationMirrors() {
    return AnnotationRecord.of(this.delegate().getAnnotationMirrors(), this.domain());
  }

  @Override // AnnotatedConstruct
  @SuppressWarnings("try")
  public final <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
    try (var lock = this.domain().lock()) {
      return this.delegate().getAnnotation(annotationType);
    }
  }

  @Override // AnnotatedConstruct
  @SuppressWarnings("try")
  public final <A extends Annotation> A[] getAnnotationsByType(final Class<A> annotationType) {
    try (var lock = this.domain().lock()) {
      return this.delegate().getAnnotationsByType(annotationType);
    }
  }

  @Override // Object
  @SuppressWarnings("try")
  public final String toString() {
    String s = this.s; // volatile read
    if (s == null) {
      try (var lock = this.domain().lock()) {
        s = this.s = this.delegate().toString(); // volatile write, read
      }
      assert s != null;
    }
    return s;
  }


  /*
   * Static methods.
   */


  /**
   * <dfn>Unwraps</dfn> the supplied {@link AnnotatedConstruct} implementation such that the returned value is not an
   * instance of {@link UniversalConstruct}.
   *
   * @param <T> an {@link AnnotatedConstruct} subtype (possibly {@link UniversalElement} or {@link UniversalType})
   *
   * @param t an {@link AnnotatedConstruct}; may be {@code null}
   *
   * @return an object of the appropriate type that is guaranteed not to be an instance of {@link UniversalConstruct}
   *
   * @see #delegate()
   */
  @SuppressWarnings("unchecked")
  public static final <T extends AnnotatedConstruct> T unwrap(T t) {
    while (t instanceof UniversalConstruct<?> uc) {
      t = (T)uc.delegate();
    }
    return t;
  }

  private static final void doNothing() {}

}
