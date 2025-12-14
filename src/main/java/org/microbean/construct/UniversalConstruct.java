/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2024–2025 microBean™.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import java.util.function.Supplier;

import javax.lang.model.AnnotatedConstruct;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

import javax.lang.model.type.TypeMirror;

import org.microbean.construct.constant.Constables;

import org.microbean.construct.element.SyntheticAnnotationMirror;
import org.microbean.construct.element.UniversalAnnotation;
import org.microbean.construct.element.UniversalElement;

import org.microbean.construct.type.UniversalType;

import static java.lang.constant.ConstantDescs.BSM_INVOKE;
import static java.lang.constant.ConstantDescs.CD_List;

import static java.lang.constant.MethodHandleDesc.ofConstructor;

import static java.util.Collections.unmodifiableList;

import static java.util.Objects.requireNonNull;

/**
 * An abstract implementation of {@link AnnotatedConstruct} from which only {@link UniversalElement} and {@link
 * UniversalType} descend.
 *
 * @param <T> a type of {@link AnnotatedConstruct}, which may be only either {@link Element} or {@link TypeMirror}
 *
 * @param <U> a type representing one of the permitted subclasses
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see AnnotatedConstruct
 *
 * @see UniversalElement
 *
 * @see UniversalType
 */
public abstract sealed class UniversalConstruct<T extends AnnotatedConstruct, U extends UniversalConstruct<T, U>>
  implements AnnotatedConstruct, Constable
  permits UniversalElement, UniversalType {


  /*
   * Instance fields.
   */


  // Retained only for Constable implementation
  private final PrimordialDomain domain;

  // Eventually this should become a lazy constant/stable value
  // volatile not needed
  private Supplier<? extends T> delegateSupplier;

  // Eventually this should become a lazy constant/stable value
  private volatile String s;

  // Eventually this should become a lazy constant/stable value
  private volatile List<? extends UniversalAnnotation> annotations;

  private final List<? extends UniversalAnnotation> syntheticAnnotations;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link AnnotatedConstruct}.
   *
   * @param delegate a delegate to which operations are delegated; must not be {@code null}
   *
   * @param domain a {@link PrimordialDomain} representing the construct domain from which the supplied {@code
   * delegate} is presumed to have originated; must not be {@code null}
   *
   * @exception NullPointerException if either argument is {@code null}
   *
   * @see #UniversalConstruct(AnnotatedConstruct, List, PrimordialDomain)
   */
  protected UniversalConstruct(final T delegate, final PrimordialDomain domain) {
    this(delegate, null, domain);
  }

  /**
   * Creates a new {@link AnnotatedConstruct}.
   *
   * @param delegate a delegate to which operations are delegated; must not be {@code null}
   *
   * @param annotations a {@link List} of {@link AnnotationMirror} instances representing annotations, often
   * synthetic, that this {@link UniversalConstruct} should bear; may be {@code null} in which case only the annotations
   * from the supplied {@code delegate} will be used
   *
   * @param domain a {@link PrimordialDomain} representing the construct domain from which the supplied {@code
   * delegate} is presumed to have originated; must not be {@code null}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @see #annotate(List)
   */
  @SuppressWarnings("try")
  protected UniversalConstruct(final T delegate,
                               final List<? extends AnnotationMirror> annotations,
                               final PrimordialDomain domain) {
    super();
    this.domain = requireNonNull(domain, "domain");
    if (annotations == null) {
      this.syntheticAnnotations = List.of();
    } else if (annotations.isEmpty()) {
      this.syntheticAnnotations = List.of();
      this.annotations = List.of(); // volatile write
    } else {
      final List<UniversalAnnotation> delegateAnnotations = new ArrayList<>(annotations.size());
      final List<UniversalAnnotation> syntheticAnnotations = new ArrayList<>(annotations.size());
      for (final AnnotationMirror annotation : annotations) {
        switch (annotation) {
        case null -> throw new IllegalArgumentException("annotations: " + annotations);
        case SyntheticAnnotationMirror sam -> syntheticAnnotations.add(UniversalAnnotation.of(sam, domain));
        case UniversalAnnotation ua -> {
          switch (ua.delegate()) {
          case SyntheticAnnotationMirror sam -> syntheticAnnotations.add(ua);
          case UniversalAnnotation x -> throw new AssertionError();
          default -> delegateAnnotations.add(UniversalAnnotation.of(annotation, domain));
          }
        }
        default -> delegateAnnotations.add(UniversalAnnotation.of(annotation, domain));
        }
      }
      this.annotations = delegateAnnotations.isEmpty() ? List.of() : unmodifiableList(delegateAnnotations); // volatile write
      this.syntheticAnnotations = syntheticAnnotations.isEmpty() ? List.of() : unmodifiableList(syntheticAnnotations);
    }
    final T unwrappedDelegate = unwrap(requireNonNull(delegate, "delegate"));
    if (unwrappedDelegate == delegate) {
      this.delegateSupplier = () -> {
        try (var lock = domain.lock()) {
          // No unwrapping happened so do symbol completion early; most common case.
          if (unwrappedDelegate instanceof Element) {
            ((Element)unwrappedDelegate).getModifiers();
          } else {
            ((TypeMirror)unwrappedDelegate).getKind();
          }
          this.delegateSupplier = () -> unwrappedDelegate;
        }
        return unwrappedDelegate;
      };
    } else {
      assert delegate instanceof UniversalConstruct<?, ?>;
      // Symbol completion already happened because unwrapping actually happened
      this.delegateSupplier = () -> unwrappedDelegate;
    }
  }


  /*
   * Instance methods.
   */


  /**
   * <strong>Experimental</strong>; returns a new {@link UniversalConstruct} instance annotated with only the supplied
   * annotations, which are often synthetic.
   *
   * @param replacementAnnotations a {@link List} of {@link AnnotationMirror}s; must not be {@code null}
   *
   * @return a new {@link UniversalConstruct} instance; never {@code null}
   *
   * @exception NullPointerException if {@code replacementAnnotations} is {@code null}
   */
  // Experimental. Unsupported.
  protected abstract U annotate(final List<? extends AnnotationMirror> replacementAnnotations);

  /**
   * Returns the delegate to which operations are delegated.
   *
   * <p>The delegate is guaranteed not to be an instance of {@link UniversalConstruct}.</p>
   *
   * @return a non-{@code null} delegate
   *
   * @see Element
   *
   * @see TypeMirror
   */
  public final T delegate() {
    final T delegate = this.delegateSupplier.get();
    assert !(delegate instanceof UniversalConstruct);
    return delegate;
  }

  @Override // Constable
  public final Optional<? extends ConstantDesc> describeConstable() {
    final T delegate = this.delegate();
    final List<? extends UniversalAnnotation> annotations = this.annotations; // volatile read; may be null and that's OK
    return this.domain() instanceof Domain d ? Constables.describe(delegate, d)
      .flatMap(delegateDesc -> Constables.describe(annotations, Constable::describeConstable)
               .map(annosDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                        ofConstructor(ClassDesc.of(this.getClass().getName()),
                                                                      ClassDesc.of(delegate instanceof TypeMirror ? TypeMirror.class.getName() : Element.class.getName()),
                                                                      CD_List,
                                                                      ClassDesc.of(PrimordialDomain.class.getName())),
                                                        delegateDesc,
                                                        annosDesc,
                                                        ((Constable)d).describeConstable().orElseThrow()))) :
      Optional.empty();
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
  public final boolean equals(final Object other) {
    // Interesting; equality does not cause symbol completion. See:
    //
    // https://github.com/openjdk/jdk/blob/jdk-26%2B25/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Type.java#L553-L559
    // (the only type that overrides this is ArrayType; see
    // https://github.com/openjdk/jdk/blob/jdk-26%2B25/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Type.java#L1402-L1406)
    //
    // https://github.com/openjdk/jdk/blob/jdk-26%2B25/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Symbol.java
    // (Symbol (Element) doesn't override it at all.)
    return this == other || switch (other) {
    case null -> false;
    case UniversalConstruct<?, ?> uc when this.getClass() == uc.getClass() -> this.delegate().equals(uc.delegate());
    default -> false;
    };
  }

  @Override // AnnotatedConstruct
  @SuppressWarnings("try")
  public final List<? extends UniversalAnnotation> getAnnotationMirrors() {
    List<? extends UniversalAnnotation> annotations = this.annotations; // volatile read
    if (annotations == null) {
      try (var lock = this.domain().lock()) {
        annotations =
          this.annotations = UniversalAnnotation.of(this.delegate().getAnnotationMirrors(), this.domain()); // volatile read/write
      }
      assert annotations != null;
    }
    if (annotations.isEmpty()) {
      return this.syntheticAnnotations;
    } else if (this.syntheticAnnotations.isEmpty()) {
      return annotations;
    } else {
      final List<UniversalAnnotation> rv = new ArrayList<>(annotations);
      rv.addAll(this.syntheticAnnotations);
      return unmodifiableList(rv);
    }
  }

  @Override // AnnotatedConstruct
  @SuppressWarnings("try")
  public final <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
    // TODO: is this lock actually needed, given how delegateSupplier works?
    try (var lock = this.domain().lock()) {
      return this.delegate().getAnnotation(annotationType);
    }
  }

  @Override // AnnotatedConstruct
  @SuppressWarnings("try")
  public final <A extends Annotation> A[] getAnnotationsByType(final Class<A> annotationType) {
    // TODO: is this lock actually needed, given how delegateSupplier works?
    try (var lock = this.domain().lock()) {
      return this.delegate().getAnnotationsByType(annotationType);
    }
  }

  @Override // Object
  public final int hashCode() {
    // Interesting; hashCode doesn't cause symbol completion. See:
    //
    // https://github.com/openjdk/jdk/blob/jdk-26%2B25/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Type.java#L565-L568
    // (AnnoConstruct doesn't define it so super.hashCode() is Object.hashCode())
    //
    // https://github.com/openjdk/jdk/blob/jdk-26%2B25/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Symbol.java
    // (Symbol (Element) doesn't override it at all.)
    return this.delegate().hashCode();
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
    while (t instanceof UniversalConstruct<?, ?> uc) {
      t = (T)uc.delegate();
    }
    return t;
  }

}
