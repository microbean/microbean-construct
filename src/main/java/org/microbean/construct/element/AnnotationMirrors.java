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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SequencedMap;

import java.util.function.Predicate;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.QualifiedNameable;

import static java.util.Collections.unmodifiableSequencedMap;

import static java.util.LinkedHashMap.newLinkedHashMap;

import static javax.lang.model.util.ElementFilter.methodsIn;

/**
 * A utility class for working with annotations as represented by {@link AnnotationMirror}s, {@link ExecutableElement}s,
 * and {@link AnnotationValue}s.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 */
public final class AnnotationMirrors {

  private static final SequencedMap<?, ?> EMPTY_MAP = unmodifiableSequencedMap(newLinkedHashMap(0));

  private static final SameAnnotationValueVisitor sameAnnotationValueVisitor = new SameAnnotationValueVisitor();

  private AnnotationMirrors() {
    super();
  }

  /**
   * For the supplied {@link AnnotationMirror}, returns an immutable, determinate {@link Map} of {@link AnnotationValue}
   * instances indexed by its {@link ExecutableElement}s to which they apply.
   *
   * <p>Each {@link ExecutableElement} represents an <a
   * href="https://docs.oracle.com/javase/specs/jls/se25/html/jls-9.html#jls-9.6.1">annotation element</a> and meets the
   * requirements of such an element.</p>
   *
   * <p>Each {@link AnnotationValue} represents the value of an annotation element and meets the requirements for
   * annotation values.</p>
   *
   * <p>This method is a more capable, better-typed replacement of the {@link
   * javax.lang.model.util.Elements#getElementValuesWithDefaults(AnnotationMirror)} method, and should be preferred.</p>
   *
   * @param a an {@link AnnotationMirror}; may be {@code null} in which case an empty, immutable, determinate {@link
   * Map} will be returned
   *
   * @return an immutable, determinate {@link Map} of {@link AnnotationValue} instances indexed by {@link
   * ExecutableElement}s
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se25/html/jls-9.html#jls-9.6.1 Java Language Specification, section
   * 9.6.1
   *
   * @see javax.lang.model.util.Elements#getElementValuesWithDefaults(AnnotationMirror)
   */
  public static final SequencedMap<ExecutableElement, AnnotationValue> allAnnotationValues(final AnnotationMirror a) {
    if (a == null) {
      return emptySequencedMap();
    }
    final Collection<? extends ExecutableElement> elements = methodsIn(a.getAnnotationType().asElement().getEnclosedElements());
    if (elements.isEmpty()) {
      return emptySequencedMap();
    }
    final SequencedMap<ExecutableElement, AnnotationValue> m = newLinkedHashMap(elements.size());
    final Map<? extends ExecutableElement, ? extends AnnotationValue> explicitValues = a.getElementValues();
    for (final ExecutableElement ee : elements) {
      // You're going to want to use getOrDefault() here. Go ahead and try but you'll run into typing issues.
      m.put(ee, explicitValues.containsKey(ee) ? explicitValues.get(ee) : ee.getDefaultValue());
    }
    return m.isEmpty() ? emptySequencedMap() : unmodifiableSequencedMap(m);
  }

  /**
   * Determines whether the two {@link AnnotationMirror}s represent the same (otherwise opaque) annotation.
   *
   * @param am0 an {@link AnnotationMirror}; may be {@code null}
   *
   * @param am1 an {@link AnnotationMirror}; may be {@code null}
   *
   * @return {@code true} if the supplied {@link AnnotationMirror}s represent the same (otherwise opaque) annotation;
   * {@code false} otherwise
   *
   * @see #sameAnnotation(AnnotationMirror, AnnotationMirror)
   */
  public static final boolean sameAnnotation(final AnnotationMirror am0, final AnnotationMirror am1) {
    return sameAnnotation(am0, am1, x -> true);
  }

  /**
   * Determines whether the two {@link AnnotationMirror}s represent the same (underlying, otherwise opaque) annotation.
   *
   * @param am0 an {@link AnnotationMirror}; may be {@code null}
   *
   * @param am1 an {@link AnnotationMirror}; may be {@code null}
   *
   * @param p a {@link Predicate} that returns {@code true} if a given {@link ExecutableElement}, representing an
   * annotation element, is to be included in the computation; may be {@code null} in which case it is as if
   * {@code ()-> true} were supplied instead
   *
   * @return {@code true} if the supplied {@link AnnotationMirror}s represent the same (underlying, otherwise opaque)
   * annotation; {@code false} otherwise
   *
   * @see SameAnnotationValueVisitor
   *
   * @see #allAnnotationValues(AnnotationMirror)
   */
  public static final boolean sameAnnotation(final AnnotationMirror am0,
                                             final AnnotationMirror am1,
                                             Predicate<? super ExecutableElement> p) {
    if (am0 == am1) {
      return true;
    } else if (am0 == null || am1 == null) {
      return false;
    }
    if (p == null) {
      p = x -> true;
    }
    final QualifiedNameable qn0 = (QualifiedNameable)am0.getAnnotationType().asElement();
    final QualifiedNameable qn1 = (QualifiedNameable)am1.getAnnotationType().asElement();
    if (qn0 != qn1 && !qn0.getQualifiedName().contentEquals(qn1.getQualifiedName())) {
      return false;
    }
    final SequencedMap<ExecutableElement, AnnotationValue> m0 = allAnnotationValues(am0);
    final SequencedMap<ExecutableElement, AnnotationValue> m1 = allAnnotationValues(am1);
    if (m0.size() != m1.size()) {
      return false;
    }
    final Iterator<Entry<ExecutableElement, AnnotationValue>> i0 = m0.entrySet().iterator();
    final Iterator<Entry<ExecutableElement, AnnotationValue>> i1 = m1.entrySet().iterator();
    while (i0.hasNext()) {
      final Entry<ExecutableElement, AnnotationValue> e0 = i0.next();
      final Entry<ExecutableElement, AnnotationValue> e1 = i1.next();
      final ExecutableElement ee0 = e0.getKey();
      if (p.test(ee0) &&
          (!e0.getKey().getSimpleName().contentEquals(e1.getKey().getSimpleName()) ||
           !sameAnnotationValueVisitor.visit(e0.getValue(), e1.getValue().getValue()))) {
        return false;
      }
    }
    return !i1.hasNext();
  }

  /**
   * A convenience method that returns a value for an annotation element named by the supplied {@link CharSequence} and
   * logically owned by the supplied {@link AnnotationMirror}, or {@code null} if no such value exists.
   *
   * @param am an {@link AnnotationMirror}; must not be {@code null}
   *
   * @param name a {@link CharSequence}; may be {@code null} in which case {@code null} will be returned
   *
   * @return the result of invoking {@link AnnotationValue#getValue() getValue()} on an {@link AnnotationValue}, or
   * {@code null}
   *
   * @exception NullPointerException if {@code am} is {@code null}
   *
   * @see AnnotationValue
   *
   * @see #allAnnotationValues(AnnotationMirror)
   */
  public static final Object get(final AnnotationMirror am, final CharSequence name) {
    if (name == null) {
      return null;
    }
    for (final Entry<? extends ExecutableElement, ? extends AnnotationValue> e : allAnnotationValues(am).entrySet()) {
      if (e.getKey().getSimpleName().contentEquals(name)) {
        final AnnotationValue av = e.getValue();
        return av == null ? null : av.getValue();
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private static final <K, V> SequencedMap<K, V> emptySequencedMap() {
    return (SequencedMap<K, V>)EMPTY_MAP;
  }

}
