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

import java.lang.annotation.Annotation;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import java.util.concurrent.CopyOnWriteArrayList;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.TypeMirror;

import static java.util.Objects.requireNonNull;

import static javax.lang.model.element.ElementKind.LOCAL_VARIABLE;

/**
 * An <strong>experimental</strong> {@link VariableElement} implementation that is a synthetic representation of a local
 * variable.
 *
 * <p>{@link SyntheticLocalVariableElement} instances may be useful for capturing declaration annotations that really
 * pertain to type usage. Such scenarios are often found in dependency injection systems.</p>
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 */
public final class SyntheticLocalVariableElement implements VariableElement {

  private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];
  
  private final List<AnnotationMirror> annotationMirrors;

  private final Name name;
  
  private final TypeMirror type;

  /**
   * Creates a new {@link SyntheticLocalVariableElement}.
   *
   * @param type a non-{@code null} {@link TypeMirror} that a hypothetical local variable may bear; as of this writing
   * no validation is performed on any argument supplied for this parameter
   *
   * @exception NullPointerException if {@code type} is {@code null}
   *
   * @see #SyntheticLocalVariableElement(Collection, TypeMirror, String)
   */
  public SyntheticLocalVariableElement(final TypeMirror type) {
    this(List.of(), type, null);
  }

  /**
   * Creates a new {@link SyntheticLocalVariableElement}.
   *
   * @param type a non-{@code null} {@link TypeMirror} that a hypothetical local variable may bear; as of this writing
   * no validation is performed on any argument supplied for this parameter
   *
   * @param name the name of this {@link SyntheticLocalVariableElement}; may be {@code null}
   *
   * @exception NullPointerException if {@code type} is {@code null}
   *
   * @see #SyntheticLocalVariableElement(Collection, TypeMirror, String)
   */
  public SyntheticLocalVariableElement(final TypeMirror type, final String name) {
    this(List.of(), type, name);
  }

  /**
   * Creates a new {@link SyntheticLocalVariableElement}.
   *
   * @param as a non-{@code null} {@link Collection} of {@link AnnotationMirror}s
   *
   * @param type a non-{@code null} {@link TypeMirror} that a hypothetical local variable may bear; as of this writing
   * no validation is performed on any argument supplied for this parameter
   *
   * @exception NullPointerException if {@code as} or {@code type} is {@code null}
   *
   * @see #SyntheticLocalVariableElement(Collection, TypeMirror, String)
   */
  public SyntheticLocalVariableElement(final Collection<? extends AnnotationMirror> as, final TypeMirror type) {
    this(as, type, null);
  }

  /**
   * Creates a new {@link SyntheticLocalVariableElement}.
   *
   * @param as a non-{@code null} {@link Collection} of {@link AnnotationMirror}s
   *
   * @param type a non-{@code null} {@link TypeMirror} that a hypothetical local variable may bear; as of this writing
   * no validation is performed on any argument supplied for this parameter
   *
   * @param name the name of this {@link SyntheticLocalVariableElement}; may be {@code null}
   *
   * @exception NullPointerException if {@code as} or {@code type} is {@code null}
   */
  public SyntheticLocalVariableElement(final Collection<? extends AnnotationMirror> as,
                                       final TypeMirror type,
                                       final String name) {
    super();
    this.annotationMirrors = new CopyOnWriteArrayList<>(as);
    this.name = new SyntheticName(name == null ? "" : name);
    this.type = requireNonNull(type, "type");
  }

  @Override // VariableElement (Element)
  public final <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
    return v.visitVariable(this, p);
  }

  @Override // VariableElement (Element)
  public final TypeMirror asType() {
    return this.type;
  }

  @Override // VariableElement (Object)
  public final boolean equals(final Object other) {
    return this == other || switch (other) {
    case null -> false;
    case SyntheticLocalVariableElement her when this.getClass() == her.getClass() -> this.type.equals(her.type) && this.name.contentEquals(her.name);
    default -> false;
    };
  }
  
  @Override // VariableElement (AnnotatedConstruct)
  public final List<AnnotationMirror> getAnnotationMirrors() {
    return this.annotationMirrors;
  }

  @Deprecated
  @Override // VariableElement (AnnotatedConstruct)
  public final <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
    return null; // deliberate
  }

  @Deprecated
  @Override // VariableElement (AnnotatedConstruct)
  @SuppressWarnings("unchecked")
  public final <A extends Annotation> A[] getAnnotationsByType(final Class<A> annotationType) {
    return (A[])EMPTY_ANNOTATION_ARRAY; // deliberate
  }
  
  @Override // VariableElement
  public final Object getConstantValue() {
    return null;
  }

  @Override // VariableElement (Element)
  public final List<? extends Element> getEnclosedElements() {
    return List.of();
  }
  
  @Override // VariableElement (Element)
  public final Element getEnclosingElement() {
    return null; // deliberate
  }

  @Override // VariableElement (Element)
  public final ElementKind getKind() {
    return LOCAL_VARIABLE;
  }

  @Override // VariableElement (Element)
  public final Set<Modifier> getModifiers() {
    return Set.of();
  }

  @Override // VariableElement (Element)
  public final Name getSimpleName() {
    return this.name;
  }

  @Override // Object
  public final int hashCode() {
    return this.name.hashCode() ^ this.type.hashCode();
  }
  
}
