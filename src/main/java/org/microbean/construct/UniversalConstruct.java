/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2024–2026 microBean™.
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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import java.util.concurrent.CopyOnWriteArrayList;

import java.util.function.Supplier;

import javax.lang.model.AnnotatedConstruct;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.QualifiedNameable;

import javax.lang.model.type.TypeMirror;

import org.microbean.construct.constant.Constables;

import org.microbean.construct.element.UniversalAnnotation;
import org.microbean.construct.element.UniversalElement;

import org.microbean.construct.type.UniversalType;

import static java.lang.constant.ConstantDescs.BSM_INVOKE;
import static java.lang.constant.ConstantDescs.CD_List;

import static java.lang.constant.MethodHandleDesc.ofConstructor;

import static java.util.Objects.requireNonNull;

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
public abstract sealed class UniversalConstruct<T extends AnnotatedConstruct>
  implements AnnotatedConstruct, Cloneable, Constable
  permits UniversalElement, UniversalType {


  /*
   * Static fields.
   */


  private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];


  /*
   * Instance fields.
   */


  private final PrimordialDomain domain;

  // Eventually this should become a lazy constant/stable value
  // volatile not needed
  private Supplier<? extends T> delegateSupplier;

  // Eventually this should become a lazy constant/stable value
  private volatile String s;

  // Eventually this should become a lazy constant/stable value
  private volatile CopyOnWriteArrayList<AnnotationMirror> annotations;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link UniversalConstruct} that is a copy of the supplied {@link UniversalConstruct}.
   *
   * @param uc a non-{@code null} {@link UniversalConstruct}
   *
   * @exception NullPointerException if {@code uc} is {@code null}
   *
   * @see #clone()
   */
  protected UniversalConstruct(final UniversalConstruct<T> uc) {
    super();
    this.domain = uc.domain;
    this.delegateSupplier = uc.delegateSupplier;
    this.s = uc.s;
    final Collection<? extends AnnotationMirror> annotations = uc.annotations; // volatile read
    if (annotations != null) {
      this.annotations = new CopyOnWriteArrayList<>(annotations);
    }
  }

  /**
   * Creates a new {@link UniversalConstruct}.
   *
   * @param delegate a delegate to which operations are delegated; must not be {@code null}
   *
   * @param domain a {@link PrimordialDomain} representing the construct domain from which the supplied {@code
   * delegate} is presumed to have originated; must not be {@code null}
   *
   * @exception NullPointerException if either argument is {@code null}
   *
   * @see #UniversalConstruct(List, AnnotatedConstruct, PrimordialDomain)
   */
  protected UniversalConstruct(final T delegate, final PrimordialDomain domain) {
    this(null, delegate, domain);
  }

  /**
   * Creates a new {@link UniversalConstruct}.
   *
   * @param annotations a {@link List} of {@link AnnotationMirror} instances representing annotations, often
   * synthetic, that this {@link UniversalConstruct} should bear; may be {@code null} in which case only the annotations
   * from the supplied {@code delegate} will be used
   * @param delegate a delegate to which operations are delegated; must not be {@code null}
   *
   * @param domain a {@link PrimordialDomain} representing the construct domain from which the supplied {@code
   * delegate} is presumed to have originated; must not be {@code null}
   *
   * @exception NullPointerException if any argument is {@code null}
   */
  @SuppressWarnings("try")
  protected UniversalConstruct(final List<? extends AnnotationMirror> annotations,
                               final T delegate,
                               final PrimordialDomain domain) {
    super();
    this.domain = requireNonNull(domain, "domain");
    if (annotations != null) {
      this.annotations = new CopyOnWriteArrayList<>(annotations);
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
      assert delegate instanceof UniversalConstruct;
      // Symbol completion already happened because unwrapping actually happened
      this.delegateSupplier = () -> unwrappedDelegate;
    }
  }


  /*
   * Instance methods.
   */


  @Override // Cloneable
  @SuppressWarnings("unchecked")
  protected UniversalConstruct<T> clone() {
    final UniversalConstruct<T> clone;
    try {
      clone = (UniversalConstruct<T>)super.clone();
    } catch (final CloneNotSupportedException e) {
      throw new AssertionError(e.getMessage(), e);
    }
    if (clone.annotations != null) {
      clone.annotations = new CopyOnWriteArrayList<>(this.getAnnotationMirrors());
    }
    return clone;
  }

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
  public final Optional<DynamicConstantDesc<T>> describeConstable() {
    final PrimordialDomain primordialDomain = this.domain();
    if (domain instanceof Domain d && d instanceof Constable dc) {
      final T delegate = this.delegate();
      final List<AnnotationMirror> annotations = this.annotations; // volatile read; may be null and that's OK
      return Constables.describe(delegate, d)
        .flatMap(delegateDesc -> Constables.describe(annotations)
                 .map(annosDesc -> DynamicConstantDesc.ofNamed(BSM_INVOKE,
                                                               this.getClass().getSimpleName(),
                                                               this.getClass().describeConstable().orElseThrow(),
                                                               ofConstructor(this.getClass().describeConstable().orElseThrow(),
                                                                             CD_List,
                                                                             (delegate instanceof TypeMirror ? TypeMirror.class : Element.class).describeConstable().orElseThrow(),
                                                                             PrimordialDomain.class.describeConstable().orElseThrow()),
                                                               annosDesc,
                                                               delegateDesc,
                                                               dc.describeConstable().orElseThrow())));
    }
    return Optional.empty();
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
    case UniversalConstruct<?> uc when this.getClass() == uc.getClass() -> this.delegate().equals(uc.delegate());
    default -> false;
    };
  }

  /**
   * Returns a non-{@code null}, determinate, <strong>mutable</strong>, thread-safe {@link List} of {@link
   * AnnotationMirror} instances representing the annotations to be considered <dfn>directly present</dfn> on this
   * {@link UniversalConstruct} implementation.
   *
   * @return a non-{@code null}, determinate, <strong>mutable</strong> thread-safe {@link List} of {@link
   * AnnotationMirror}s
   *
   * @see AnnotatedConstruct#getAnnotationMirrors()
   */
  @Override // AnnotatedConstruct
  @SuppressWarnings("try")
  public final List<AnnotationMirror> getAnnotationMirrors() {
    CopyOnWriteArrayList<AnnotationMirror> annotations = this.annotations; // volatile read
    if (annotations == null) {
      try (var lock = this.domain().lock()) {
        this.annotations = annotations = // volatile write
          new CopyOnWriteArrayList<>(UniversalAnnotation.of(this.delegate().getAnnotationMirrors(), this.domain()));
      }
    }
    return annotations;
  }

  /**
   * Makes a <strong>best effort</strong> to return an {@link Annotation} of the appropriate type <dfn>present</dfn> on
   * this {@link UniversalConstruct} implementation.
   *
   * <p>See the specification for the {@link AnnotatedConstruct#getAnnotation(Class)} method for important details.</p>
   *
   * <p>{@link UniversalConstruct} implementations deliberately permit modification of their {@linkplain
   * #getAnnotationMirrors() annotations}. Consequently, this override first checks to see if there is at least one
   * {@link AnnotationMirror} whose {@linkplain AnnotationMirror#getAnnotationType() annotation type} is declared by a
   * {@link javax.lang.model.element.TypeElement} whose {@linkplain
   * javax.lang.model.element.TypeElement#getQualifiedName() qualified name} is {@linkplain
   * javax.lang.model.element.Name#contentEquals(CharSequence) equal to} the {@linkplain Class#getCanonicalName()
   * canonical name} of the supplied {@link Class}. If there is, then the {@link AnnotatedConstruct#getAnnotation(Class)
   * getAnnotation(Class)} method is invoked on the {@linkplain #delegate() delegate} and its result is
   * returned. Otherwise, {@code null} is returned.</p>
   *
   * <p>There are circumstances where the {@link Annotation} returned by this method may not accurately reflect a
   * synthetic annotation added to this {@link AnnotatedConstruct} implementation's {@linkplain #getAnnotationMirrors()
   * annotations}.</p>
   *
   * <p>In general, the use of this method is discouraged.</p>
   *
   * @param annotationType a {@link Class} that is an annotation interface; must not be {@code null}
   *
   * @return an appropriate {@link Annotation}, or {@code null}
   *
   * @exception NullPointerException if {@code annotationType} is {@code null}
   *
   * @see AnnotatedConstruct#getAnnotation(Class)
   *
   * @deprecated The use of this method is discouraged.
   */
  @Deprecated
  @Override // AnnotatedConstruct
  @SuppressWarnings("try")
  public final <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
    if (!annotationType.isAnnotation()) {
      return null;
    }
    final String canonicalName = annotationType.getCanonicalName();
    for (final AnnotationMirror a : this.getAnnotationMirrors()) {
      if (((QualifiedNameable)a.getAnnotationType().asElement()).getQualifiedName().contentEquals(canonicalName)) {
        // TODO: is this lock actually needed, given how delegateSupplier works?
        try (var lock = this.domain().lock()) {
          return this.delegate().getAnnotation(annotationType);
        }
      }
    }
    return null;
  }

  /**
   * Makes a <strong>best effort</strong> to return an array of {@link Annotation}s of the appropriate type
   * <dfn>associated</dfn> with this {@link UniversalConstruct} implementation.
   *
   * <p>See the specification for the {@link AnnotatedConstruct#getAnnotationsByType(Class)} method for important
   * details.</p>
   *
   * <p>{@link UniversalConstruct} implementations deliberately permit modification of their {@linkplain
   * #getAnnotationMirrors() annotations}. Consequently, this override first checks to see if there is at least one
   * {@link AnnotationMirror} whose {@linkplain AnnotationMirror#getAnnotationType() annotation type} is declared by a
   * {@link javax.lang.model.element.TypeElement} whose {@linkplain
   * javax.lang.model.element.TypeElement#getQualifiedName() qualified name} is {@linkplain
   * javax.lang.model.element.Name#contentEquals(CharSequence) equal to} the {@linkplain Class#getCanonicalName()
   * canonical name} of the supplied {@link Class}. If there is, then the {@link
   * AnnotatedConstruct#getAnnotationsByType(Class) getAnnotationsByType(Class)} method is invoked on the {@linkplain
   * #delegate() delegate} and its result is returned. Otherwise, an empty array is returned.</p>
   *
   * <p>There are circumstances where the {@link Annotation} array returned by this method may not accurately reflect
   * synthetic annotations added to this {@link AnnotatedConstruct} implementation's {@linkplain #getAnnotationMirrors()
   * annotations}.</p>
   *
   * <p>In general, the use of this method is discouraged.</p>
   *
   * @param annotationType a {@link Class} that is an annotation interface; must not be {@code null}
   *
   * @return an appropriate {@link Annotation}, or {@code null}
   *
   * @exception NullPointerException if {@code annotationType} is {@code null}
   *
   * @see AnnotatedConstruct#getAnnotation(Class)
   *
   * @deprecated The use of this method is discouraged.
   */
  @Deprecated
  @Override // AnnotatedConstruct
  @SuppressWarnings({"try", "unchecked"})
  public final <A extends Annotation> A[] getAnnotationsByType(final Class<A> annotationType) {
    if (!annotationType.isAnnotation()) {
      return (A[])EMPTY_ANNOTATION_ARRAY;
    }
    final String canonicalName = annotationType.getCanonicalName();
    for (final AnnotationMirror a : this.getAnnotationMirrors()) {
      if (((QualifiedNameable)a.getAnnotationType().asElement()).getQualifiedName().contentEquals(canonicalName)) {
        // TODO: is this lock actually needed, given how delegateSupplier works?
        try (var lock = this.domain().lock()) {
          return this.delegate().getAnnotationsByType(annotationType);
        }
      }
    }
    return (A[])EMPTY_ANNOTATION_ARRAY;
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

}
