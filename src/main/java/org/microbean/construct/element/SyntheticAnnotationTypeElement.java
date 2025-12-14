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

import java.lang.annotation.Annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;

import org.microbean.construct.type.NoneType;

import static java.util.Collections.unmodifiableList;

import static java.util.Objects.requireNonNull;

import static javax.lang.model.element.ElementKind.ANNOTATION_TYPE;
import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.ElementKind.ENUM;

import static javax.lang.model.element.Modifier.ABSTRACT;

import static javax.lang.model.type.TypeKind.ARRAY;
import static javax.lang.model.type.TypeKind.DECLARED;

/**
 * An <strong>experimental</strong> {@link TypeElement} implementation that is wholly synthetic and suitable only for
 * (partially) modeling {@linkplain AnnotationMirror#getAnnotationType() annotation types}.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see SyntheticAnnotationMirror
 */
public final class SyntheticAnnotationTypeElement implements TypeElement {

  private static final Set<Modifier> modifiers = Set.of(ABSTRACT);

  private final List<? extends AnnotationMirror> annotationMirrors;

  private final SyntheticName fqn;

  private final Type type;

  private final List<? extends InternalAnnotationElement> elements;

  /**
   * Creates a new {@link SyntheticAnnotationTypeElement}.
   *
   * @param annotationMirrors a {@link List} of {@link AnnotationMirror}s modeling the annotations this element has;
   * must not be {@code null}
   *
   * @param fullyQualifiedName the fully qualified name of the synthetic element; must conform to Java classname
   * restrictions; must not be {@code null}
   *
   * @param elements a {@link List} of {@link SyntheticAnnotationElement}s modeling the annotation elements; must not be
   * {@code null}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @exception IllegalArgumentException if {@code fullyQualifiedName} does not conform to Java classname restrictions
   *
   * @see SyntheticAnnotationElement
   *
   * @see SyntheticAnnotationMirror
   */
  public SyntheticAnnotationTypeElement(final List<? extends AnnotationMirror> annotationMirrors,
                                        final String fullyQualifiedName,
                                        final List<? extends SyntheticAnnotationElement> elements) {
    super();
    this.annotationMirrors = List.copyOf(annotationMirrors);
    this.fqn = SyntheticName.of(fullyQualifiedName);
    this.type = new Type();
    final List<InternalAnnotationElement> elements0 = new ArrayList<>(elements.size());
    for (final SyntheticAnnotationElement e : elements) {
      elements0.add(new InternalAnnotationElement(e.annotationMirrors(), e.type(), e.name()));
    }
    this.elements = unmodifiableList(elements0);
  }

  @Override // Element
  public final <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
    return v.visitType(this, p);
  }

  @Override // Element
  public final TypeMirror asType() {
    return this.type;
  }

  @Override // AnnotatedConstruct
  public final List<? extends AnnotationMirror> getAnnotationMirrors() {
    return this.annotationMirrors;
  }

  @Override // AnnotatedConstruct
  public final <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
    throw new UnsupportedOperationException(); // deliberate
  }

  @Override // AnnotatedConstruct
  public final <A extends Annotation> A[] getAnnotationsByType(final Class<A> annotationType) {
    throw new UnsupportedOperationException(); // deliberate
  }

  @Override
  public final List<? extends Element> getEnclosedElements() {
    return this.elements;
  }

  @Override
  public final Element getEnclosingElement() {
    return null; // really should be a package but we're synthetic
  }

  @Override
  public final List<? extends TypeMirror> getInterfaces() {
    return List.of();
  }

  @Override
  public final Set<Modifier> getModifiers() {
    return modifiers;
  }

  @Override
  public final ElementKind getKind() {
    return ElementKind.ANNOTATION_TYPE;
  }

  @Override
  public final NestingKind getNestingKind() {
    return NestingKind.TOP_LEVEL;
  }

  @Override
  public final Name getQualifiedName() {
    return this.fqn;
  }

  @Override
  public final Name getSimpleName() {
    final String fqn = this.getQualifiedName().toString();
    final int i = fqn.lastIndexOf('.');
    return i >= 0 ? SyntheticName.of(fqn.substring(i + 1)) : this.getQualifiedName();
  }

  @Override
  public final TypeMirror getSuperclass() {
    return NoneType.of();
  }

  @Override
  public final List<? extends TypeParameterElement> getTypeParameters() {
    return List.of();
  }

  @Override
  public final String toString() {
    return "@" + this.getQualifiedName().toString(); // TODO: robustify
  }

  /**
   * An <strong>experimental</strong> collection of information out of which a synthetic annotation element may be
   * fashioned.
   *
   * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
   */
  public static final class SyntheticAnnotationElement {

    private final List<? extends AnnotationMirror> annotationMirrors;

    private final TypeMirror type;

    private final String name;

    /**
     * Creates a new {@link SyntheticAnnotationElement}.
     *
     * @param annotationMirrors a {@link List} of {@link AnnotationMirror}s modeling the annotations this element has;
     * must not be {@code null}
     *
     * @param type the type of the annotation element; must conform to Java annotation element type restrictions; must
     * not be {@code null}
     *
     * @param name the name of the annotation element; must conform to Java method naming requirements; must not be
     * {@code null}
     *
     * @exception NullPointerException if any argument is {@code null}
     *
     * @exception IllegalArgumentException if any argument does not conform to its requirements
     */
    public SyntheticAnnotationElement(final List<? extends AnnotationMirror> annotationMirrors,
                                      final TypeMirror type, // the "return type"
                                      final String name) {
      super();
      this.annotationMirrors = List.copyOf(annotationMirrors);
      this.type = requireNonNull(type, "type");
      this.name = requireNonNull(name, "name");
    }

    final List<? extends AnnotationMirror> annotationMirrors() {
      return this.annotationMirrors;
    }

    @Override // Object
    public final boolean equals(final Object other) {
      return this == other || switch (other) {
      case null -> false;
      case SyntheticAnnotationElement sae when this.getClass() == sae.getClass() ->
      Objects.equals(this.name, sae.name) &&
      Objects.equals(this.type, sae.type) &&
      Objects.equals(this.annotationMirrors, sae.annotationMirrors);
      default -> false;
      };
    }

    @Override // Object
    public final int hashCode() {
      int hashCode = 31;
      int c = this.name.hashCode();
      hashCode = 17 * hashCode + c;
      c = this.type.hashCode();
      hashCode = 17 * hashCode + c;
      c = this.annotationMirrors.hashCode();
      hashCode = 17 * hashCode + c;
      return hashCode;
    }

    final String name() {
      return this.name;
    }

    final TypeMirror type() {
      return this.type;
    }

  }

  /**
   * An <strong>experimental</strong>, synthetic {@link DeclaredType} implementation that models the synthetic
   * annotation type declared by the enclosing synthetic {@link SyntheticAnnotationTypeElement}.
   *
   * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
   *
   * @see SyntheticAnnotationTypeElement
   */
  private class Type implements DeclaredType {

    private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];

    private Type() {
      super();
    }

    @Override // TypeMirror
    public final <R, P> R accept(final TypeVisitor<R, P> v, final P p) {
      return v.visitDeclared(this, p);
    }

    @Override // ExecutableType (AnnotatedConstruct)
    public final <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
      return null; // deliberate
    }

    @Override // ExecutableType (AnnotatedConstruct)
    public final List<? extends AnnotationMirror> getAnnotationMirrors() {
      return List.of(); // deliberate
    }

    @Override // ExecutableType (AnnotatedConstruct)
    @SuppressWarnings("unchecked")
    public final <A extends Annotation> A[] getAnnotationsByType(final Class<A> annotationType) {
      return (A[])EMPTY_ANNOTATION_ARRAY; // deliberate
    }

    @Override // DeclaredType (TypeMirror)
    public final TypeKind getKind() {
      return TypeKind.DECLARED;
    }

    @Override
    public final TypeMirror getEnclosingType() {
      return NoneType.of();
    }

    @Override // DeclaredType (TypeMirror)
    public final List<? extends TypeMirror> getTypeArguments() {
      return List.of();
    }

    @Override // DeclaredType
    public final SyntheticAnnotationTypeElement asElement() {
      return SyntheticAnnotationTypeElement.this;
    }

    @Override
    public final String toString() {
      return this.asElement().toString();
    }

  }

  private final class InternalAnnotationElement implements ExecutableElement {

    private final List<? extends AnnotationMirror> annotationMirrors;

    private final Type t;

    private final SyntheticName name;

    private InternalAnnotationElement(final List<? extends AnnotationMirror> annotationMirrors,
                                      final TypeMirror type,
                                      final String name) {
      super();
      this.annotationMirrors = List.copyOf(annotationMirrors);
      this.t = new Type(type);
      this.name = SyntheticName.of(name);
    }

    @Override // Element
    public final <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
      return v.visitExecutable(this, p);
    }

    @Override // Element
    public final Type asType() {
      return this.t;
    }

    @Override // AnnotatedConstruct
    public final List<? extends AnnotationMirror> getAnnotationMirrors() {
      return this.annotationMirrors;
    }

    @Override // AnnotatedConstruct
    public final <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
      throw new UnsupportedOperationException(); // deliberate
    }

    @Override // AnnotatedConstruct
    public final <A extends Annotation> A[] getAnnotationsByType(final Class<A> annotationType) {
      throw new UnsupportedOperationException(); // deliberate
    }

    @Override
    public AnnotationValue getDefaultValue() {
      return null; // deliberate
    }

    @Override
    public final List<? extends Element> getEnclosedElements() {
      return List.of();
    }

    @Override
    public final SyntheticAnnotationTypeElement getEnclosingElement() {
      return SyntheticAnnotationTypeElement.this;
    }

    @Override
    public final Set<Modifier> getModifiers() {
      throw new UnsupportedOperationException();
    }

    @Override
    public final ElementKind getKind() {
      return ElementKind.METHOD;
    }

    @Override
    public final List<? extends VariableElement> getParameters() {
      return List.of();
    }

    @Override
    public final TypeMirror getReceiverType() {
      return asType().getReceiverType();
    }

    @Override
    public final TypeMirror getReturnType() {
      return asType().getReturnType();
    }

    @Override
    public final Name getSimpleName() {
      return this.name;
    }

    @Override
    public final List<? extends TypeMirror> getThrownTypes() {
      return asType().getThrownTypes();
    }

    @Override
    public final List<? extends TypeParameterElement> getTypeParameters() {
      return List.of();
    }

    @Override
    public final boolean isDefault() {
      return false;
    }

    @Override
    public final boolean isVarArgs() {
      return false;
    }

    private static final class Type implements ExecutableType {

      private final TypeMirror type;

      private Type(final TypeMirror type) {
        super();
        this.type = validate(type);
      }

      private static TypeMirror validate(final TypeMirror type) {
        TypeMirror t = type;
        TypeKind k = t.getKind();
        if (k == ARRAY && t instanceof ArrayType a) {
          t = a.getComponentType();
          k = t.getKind();
        }
        if (k.isPrimitive()) {
          return type;
        }
        if (k == DECLARED) {
          final TypeElement e = (TypeElement)((DeclaredType)t).asElement();
          switch (e.getKind()) {
          case ANNOTATION_TYPE:
          case ENUM:
            return type;
          case CLASS:
            final Name fqn = e.getQualifiedName();
            if (fqn.contentEquals("java.lang.String") || fqn.contentEquals("java.lang.Class")) {
              return type;
            }
            break;
          default:
            break;
          }
        }
        throw new IllegalArgumentException("type: " + type);
      }

      @Override // TypeMirror
      public final <R, P> R accept(final TypeVisitor<R, P> v, final P p) {
        return v.visitExecutable(this, p);
      }

      @Override // ExecutableType (AnnotatedConstruct)
      public final <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
        throw new UnsupportedOperationException(); // deliberate
      }

      @Override // ExecutableType (AnnotatedConstruct)
      public final List<? extends AnnotationMirror> getAnnotationMirrors() {
        return List.of(); // can't actually put type annotations on an executable type to my knowledge
      }

      @Override // ExecutableType (AnnotatedConstruct)
      public final <A extends Annotation> A[] getAnnotationsByType(final Class<A> annotationType) {
        throw new UnsupportedOperationException(); // deliberate
      }

      @Override // ExecutableType (TypeMirror)
      public final TypeKind getKind() {
        return TypeKind.EXECUTABLE;
      }

      @Override // ExecutableType
      public final List<? extends TypeMirror> getParameterTypes() {
        return List.of();
      }

      @Override // ExecutableType
      public final TypeMirror getReceiverType() {
        return NoneType.of();
      }

      @Override // ExecutableType
      public final TypeMirror getReturnType() {
        return this.type;
      }

      @Override // ExecutableType
      public final List<? extends TypeMirror> getThrownTypes() {
        return List.of();
      }

      @Override // ExecutableType
      public final List<? extends TypeVariable> getTypeVariables() {
        return List.of();
      }

    }

  }

}
