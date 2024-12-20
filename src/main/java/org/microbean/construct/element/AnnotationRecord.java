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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;

import org.microbean.construct.Domain;

import org.microbean.construct.type.UniversalType;

import static java.util.HashMap.newHashMap;

/**
 * An {@link AnnotationMirror} implementation.
 *
 * @param delegate an {@link AnnotationMirror} to which operations will be delegated; must not be {@code null}
 *
 * @param domain a {@link Domain}; must not be {@code null}
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see AnnotationMirror
 */
public final record AnnotationRecord(AnnotationMirror delegate, Domain domain) implements AnnotationMirror {


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link AnnotationRecord}.
   *
   * @param delegate an {@link AnnotationMirror} to which operations will be delegated; must not be {@code null}
   *
   * @param domain a {@link Domain}; must not be {@code null}
   *
   * @exception NullPointerException if either argument is {@code null}
   */
  public AnnotationRecord {
    Objects.requireNonNull(delegate, "delegate");
    Objects.requireNonNull(domain, "domain");
  }


  /*
   * Instance methods.
   */


  @Override // Object
  @SuppressWarnings("try")
  public final boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other instanceof AnnotationMirror a) { // instanceof on purpose
      try (var lock = this.domain().lock()) {
        // We need the explicit domain lock because a might not be an AnnotationRecord and so its annotation type might
        // not be a UniversalType.
        //
        // Type equality anywhere in here will be via domain.sameType(TypeMirror, TypeMirror).
        //
        // Because the keys in getElementValues() are UniversalElements, equality of elements will be OK.
        return
          this.getAnnotationType().equals(a.getAnnotationType()) &&
          this.getElementValues().equals(a.getElementValues());
      }
    } else {
      return false;
    }
  }

  @Override // AnnotationMirror
  @SuppressWarnings("try")
  public final UniversalType getAnnotationType() {
    final Domain d = this.domain();
    try (var lock = d.lock()) {
      return UniversalType.of(this.delegate().getAnnotationType(), d);
    }
  }

  @Override // AnnotationMirror
  @SuppressWarnings("try")
  public final Map<? extends UniversalElement, ? extends AnnotationValueRecord> getElementValues() {
    final Map<UniversalElement, AnnotationValueRecord> map = newHashMap(17);
    final Domain d = this.domain();
    try (var lock = d.lock()) {
      for (final Entry<? extends ExecutableElement, ? extends AnnotationValue> e : this.delegate().getElementValues().entrySet()) {
        map.put(UniversalElement.of(e.getKey(), d), AnnotationValueRecord.of(e.getValue(), d));
      }
    }
    return Collections.unmodifiableMap(map);
  }

  @Override // AnnotationMirror
  public final int hashCode() {
    int hashCode = 37 * 17 + this.getAnnotationType().hashCode();
    return 37 * hashCode + this.getElementValues().hashCode();
  }


  /*
   * Static methods.
   */


  /**
   * Returns a non-{@code null} {@link AnnotationRecord} that is either the supplied {@link AnnotationMirror} (if it
   * itself is an {@link AnnotationRecord}) or one that wraps it.
   *
   * @param a an {@link AnnotationMirror}; must not be {@code null}
   *
   * @param d a {@link Domain}; must not be {@code null}
   *
   * @return a non-{@code null} {@link AnnotationRecord}
   *
   * @exception NullPointerException if either argument is {@code null}
   *
   * @see #AnnotationRecord(AnnotationMirror, Domain)
   */
  public static final AnnotationRecord of(final AnnotationMirror a, final Domain d) {
    return a instanceof AnnotationRecord ar ? ar : new AnnotationRecord(a, d);
  }

  /**
   * Returns a non-{@code null}, immutable {@link List} of {@link AnnotationRecord}s whose elements wrap the supplied
   * {@link List}'s elements.
   *
   * @param as a {@link Collection} of {@link AnnotationMirror}s; must not be {@code null}
   *
   * @param domain a {@link Domain}; must not be {@code null}
   *
   * @return a non-{@code null}, immutable {@link List} of {@link AnnotationRecord}s
   *
   * @exception NullPointerException if either argument is {@code null}
   */
  public static final List<? extends AnnotationRecord> of(final Collection<? extends AnnotationMirror> as,
                                                          final Domain domain) {
    final List<AnnotationRecord> newAs = new ArrayList<>(as.size());
    for (final AnnotationMirror a : as) {
      newAs.add(AnnotationRecord.of(a, domain));
    }
    return Collections.unmodifiableList(newAs);
  }
}
