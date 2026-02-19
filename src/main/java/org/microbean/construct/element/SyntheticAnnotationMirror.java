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

import java.lang.constant.ClassDesc;
import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DynamicConstantDesc;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;

import org.microbean.construct.constant.Constables;

import static java.lang.constant.ConstantDescs.BSM_INVOKE;
import static java.lang.constant.ConstantDescs.CD_Map;

import static java.lang.constant.MethodHandleDesc.ofConstructor;

import static java.util.Collections.unmodifiableMap;

import static java.util.LinkedHashMap.newLinkedHashMap;

import static javax.lang.model.element.ElementKind.ANNOTATION_TYPE;

import static javax.lang.model.util.ElementFilter.methodsIn;

/**
 * An <strong>experimental</strong> {@link AnnotationMirror} implementation that is partially or wholly synthetic.
 *
 * <p>It is possible to create {@link SyntheticAnnotationMirror} instances representing annotations that a Java compiler
 * will never produce. For example, <a
 * href="https://docs.oracle.com/javase/specs/jls/se25/html/jls-9.html#jls-9.6.1">annotations cannot refer to each
 * other, directly or indirectly</a>, but two {@link SyntheticAnnotationMirror}s may do so.</p>
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @spec https://docs.oracle.com/javase/specs/jls/se25/html/jls-9.html#jls-9.6.1 Java Language Specification, section
 * 9.6.1
 */
public final class SyntheticAnnotationMirror implements AnnotationMirror, Constable {


  /*
   * Instance fields.
   */


  private final TypeElement annotationTypeElement;

  private final Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link SyntheticAnnotationMirror}.
   *
   * @param annotationTypeElement a {@link TypeElement} representing an annotation type; must not be {@code null}; must
   * return {@link javax.lang.model.element.ElementKind#ANNOTATION_TYPE ANNOTATION_TYPE} from its {@link
   * Element#getKind() getKind()} method; {@link SyntheticAnnotationTypeElement} implementations are strongly preferred
   *
   * @exception NullPointerException if {@code annotationTypeElement} is {@code null}
   *
   * @exception IllegalArgumentException if {@code annotationTypeElement} does not return {@link
   * javax.lang.model.element.ElementKind#ANNOTATION_TYPE ANNOTATION_TYPE} from an invocation of its {@link
   * Element#getKind() getKind()} method, or if {@code values} has more entries in it than {@code annotationTypeElement}
   * has {@linkplain Element#getEnclosedElements() anotation elements}
   *
   * @see #SyntheticAnnotationMirror(TypeElement, Map)
   */
  public SyntheticAnnotationMirror(final TypeElement annotationTypeElement) {
    this(annotationTypeElement, Map.of());
  }

  /**
   * Creates a new {@link SyntheticAnnotationMirror}.
   *
   * @param annotationTypeElement a {@link TypeElement} representing an annotation type; must not be {@code null}; must
   * return {@link javax.lang.model.element.ElementKind#ANNOTATION_TYPE ANNOTATION_TYPE} from its {@link
   * Element#getKind() getKind()} method; {@link SyntheticAnnotationTypeElement} implementations are strongly preferred
   *
   * @param values a {@link Map} of annotation values indexed by annotation element name; must not be {@code null}; must
   * contain only values that are permissible for annotation elements
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @exception IllegalArgumentException if {@code annotationTypeElement} does not return {@link
   * javax.lang.model.element.ElementKind#ANNOTATION_TYPE ANNOTATION_TYPE} from an invocation of its {@link
   * Element#getKind() getKind()} method, or if {@code values} has more entries in it than {@code annotationTypeElement}
   * has {@linkplain Element#getEnclosedElements() anotation elements}
   */
  public SyntheticAnnotationMirror(final TypeElement annotationTypeElement,
                                   final Map<? extends String, ?> values) {
    super();
    if (annotationTypeElement.getKind() != ANNOTATION_TYPE) {
      throw new IllegalArgumentException("annotationTypeElement: " + annotationTypeElement);
    }
    this.annotationTypeElement = annotationTypeElement;
    final LinkedHashMap<ExecutableElement, AnnotationValue> m = newLinkedHashMap(values.size());
    final List<? extends ExecutableElement> methods = methodsIn(annotationTypeElement.getEnclosedElements());
    for (final ExecutableElement e : methods) {
      final Object value = values.get(e.getSimpleName().toString()); // default value deliberately not included
      if (value == null) {
        if (e.getDefaultValue() == null) {
          // There has to be a value somewhere, or annotationTypeElement or values is illegal
          throw new IllegalArgumentException("annotationTypeElement: " + annotationTypeElement + "; values: " + values);
        }
        // Default values are excluded from the map on purpose, following the contract of
        // AnnotationValue#getElementValues().
      } else {
        m.put(e, value instanceof AnnotationValue av ? av : new SyntheticAnnotationValue(value));
      }
    }
    if (values.size() > m.size()) {
      throw new IllegalArgumentException("values: " + values);
    }
    this.elementValues = m.isEmpty() ? Map.of() : unmodifiableMap(m);
  }

  /**
   * Creates a new {@link SyntheticAnnotationMirror} that is an effective <dfn>copy</dfn> of the supplied {@link
   * AnnotationMirror}.
   *
   * @param a a non-{@code null} {@link AnnotationMirror} to semantically copy
   *
   * @exception NullPointerException if {@code a} is {@code null}
   */
  public SyntheticAnnotationMirror(final AnnotationMirror a) {
    super();
    this.annotationTypeElement = new SyntheticAnnotationTypeElement((TypeElement)a.getAnnotationType().asElement());
    final Map<? extends ExecutableElement, ? extends AnnotationValue> originalElementValues = a.getElementValues();
    if (originalElementValues.isEmpty()) {
      // If there are no explicit values, then...there are no explicit values whether the annotation interface type
      // contains/encloses any elements at all.
      this.elementValues = Map.of();
    } else {
      // There are explicit values. That also means that the annotation interface type contains/encloses at least one
      // element.
      final List<ExecutableElement> syntheticElements = methodsIn(this.annotationTypeElement.getEnclosedElements());
      assert !syntheticElements.isEmpty();
      final Map<ExecutableElement, AnnotationValue> newElementValues = newLinkedHashMap(originalElementValues.size());
      for (final Entry<? extends ExecutableElement, ? extends AnnotationValue> originalEntry : originalElementValues.entrySet()) {
        final ExecutableElement originalElement = originalEntry.getKey();
        final ExecutableElement syntheticElement = element(syntheticElements, originalElement.getSimpleName());
        if (syntheticElement != null) {
          newElementValues.put(syntheticElement, originalEntry.getValue());
        }
      }
      this.elementValues = unmodifiableMap(newElementValues);
    }
  }


  /*
   * Instance methods.
   */


  @Override // Constable
  public final Optional<? extends ConstantDesc> describeConstable() {
    return this.annotationTypeElement instanceof Constable c ? c.describeConstable() : Optional.<ConstantDesc>empty()
      .flatMap(elementDesc -> Constables.describe(this.elementValues,
                                                  SyntheticAnnotationMirror::describeExecutableElement,
                                                  SyntheticAnnotationMirror::describeAnnotationValue)
               .map(valuesDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                         ofConstructor(ClassDesc.of(this.getClass().getName()),
                                                                       ClassDesc.of(TypeElement.class.getName()),
                                                                       CD_Map),
                                                         elementDesc,
                                                         valuesDesc)));
  }

  @Override // AnnotationMirror
  public final DeclaredType getAnnotationType() {
    return (DeclaredType)this.annotationTypeElement.asType();
  }

  @Override // AnnotationMirror
  public final Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValues() {
    return this.elementValues;
  }

  @Override
  public final String toString() {
    return "@" + this.annotationTypeElement.toString(); // TODO: not anywhere near good enough
  }


  /*
   * Static methods.
   */


  private static final Optional<? extends ConstantDesc> describeAnnotationValue(final Object v) {
    return switch (v) {
    case null -> Optional.empty(); // deliberately not Optional.of(NULL); annotation values cannot be null
    case Constable c -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd);
    case List<?> l -> Constables.describe(l, e -> e instanceof Constable c ? c.describeConstable() : Optional.empty());
    default -> Optional.empty();
    };
  }

  private static final Optional<? extends ConstantDesc> describeExecutableElement(final ExecutableElement e) {
    return switch (e) {
    case null -> throw new IllegalStateException();
    case Constable c -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd);
    default -> Optional.empty();
    };
  }

  private static final <E extends Element> E element(final Iterable<? extends E> elements, final CharSequence simpleName) {
    for (final E e : elements) {
      if (e.getSimpleName().contentEquals(simpleName)) {
        return e;
      }
    }
    return null;
  }

}
