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
package org.microbean.construct.type;

import java.lang.annotation.Annotation;

import java.util.List;

import javax.lang.model.element.AnnotationMirror;

import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;

/**
 * A useful implementation of {@link NoType} with a {@link TypeKind} of {@link TypeKind#NONE NONE}.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 */
public final class NoneType implements NoType {

  private static final NoneType INSTANCE = new NoneType();
  
  private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];

  private NoneType() {
    super();
  }

  @Override // TypeMirror
  public final <R, P> R accept(final TypeVisitor<R, P> v, final P p) {
    return v.visitNoType(this, p);
  }
  
  @Override // NoType (AnnotatedConstruct)
  public final <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
    return null;
  }

  @Override // NoType (AnnotatedConstruct)
  public final List<? extends AnnotationMirror> getAnnotationMirrors() {
    return List.of();
  }

  @Override // NoType (AnnotatedConstruct)
  @SuppressWarnings("unchecked")
  public final <A extends Annotation> A[] getAnnotationsByType(final Class<A> annotationType) {
    return (A[])EMPTY_ANNOTATION_ARRAY;
  }
  
  @Override // NoType (TypeMirror)
  public final TypeKind getKind() {
    return TypeKind.NONE;
  }

  /**
   * Returns a {@link NoneType}.
   *
   * @return a non-{@code null} {@link NoneType}
   *
   * @see NoType
   *
   * @see TypeKind#NONE
   */
  public static final NoneType of() {
    return INSTANCE;
  }
  
}
