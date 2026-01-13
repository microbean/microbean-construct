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
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;

import org.microbean.construct.type.NoneType;

import static java.util.Collections.unmodifiableList;

import static java.util.Objects.requireNonNull;

import static javax.lang.model.element.ElementKind.ANNOTATION_TYPE;
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


  /*
   * Static fields.
   */


  private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];

  private static final Set<Modifier> ABSTRACT_ONLY_MODIFIERS = Set.of(ABSTRACT);


  /*
   * Instance fields.
   */


  private final List<? extends AnnotationMirror> annotationMirrors;

  private final SyntheticName fqn;

  private final SyntheticName sn;

  private final Type type;

  private final List<? extends InternalAnnotationElement> elements;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link SyntheticAnnotationTypeElement}, mostly, if not exclusively, for use by {@link
   * SyntheticAnnotationMirror} instances.
   *
   * @param fullyQualifiedName the fully qualified name of the synthetic element; must conform to Java classname
   * restrictions; must not be {@code null}
   *
   * @exception NullPointerException if {@code fullyQualifiedName} is {@code null}
   *
   * @exception IllegalArgumentException if {@code fullyQualifiedName} does not conform to Java classname restrictions
   *
   * @see #SyntheticAnnotationTypeElement(List, SyntheticName, List)
   */
  public SyntheticAnnotationTypeElement(final SyntheticName fullyQualifiedName) {
    this(List.of(), fullyQualifiedName, List.of());
  }

  /**
   * Creates a new {@link SyntheticAnnotationTypeElement}, mostly, if not exclusively, for use by {@link
   * SyntheticAnnotationMirror} instances.
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
   * @see #SyntheticAnnotationTypeElement(List, SyntheticName, List)
   */
  public SyntheticAnnotationTypeElement(final SyntheticName fullyQualifiedName,
                                        final List<? extends SyntheticAnnotationElement> elements) {
    this(List.of(), fullyQualifiedName, elements);
  }

  /**
   * Creates a new {@link SyntheticAnnotationTypeElement}, mostly, if not exclusively, for use by {@link
   * SyntheticAnnotationMirror} instances.
   *
   * @param annotationMirrors a {@link List} of {@link AnnotationMirror}s modeling the annotations this element has;
   * must not be {@code null}
   *
   * @param fullyQualifiedName the fully qualified name of the synthetic element; must conform to Java classname
   * restrictions; must not be {@code null}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @exception IllegalArgumentException if {@code fullyQualifiedName} does not conform to Java classname restrictions
   *
   * @see #SyntheticAnnotationTypeElement(List, SyntheticName, List)
   */
  public SyntheticAnnotationTypeElement(final List<? extends AnnotationMirror> annotationMirrors,
                                        final SyntheticName fullyQualifiedName) {
    this(annotationMirrors, fullyQualifiedName, List.of());
  }

  /**
   * Creates a new {@link SyntheticAnnotationTypeElement}, mostly, if not exclusively, for use by {@link
   * SyntheticAnnotationMirror} instances.
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
                                        final SyntheticName fullyQualifiedName,
                                        final List<? extends SyntheticAnnotationElement> elements) {
    super();
    this.annotationMirrors = List.copyOf(annotationMirrors);
    final String fqn = fullyQualifiedName.toString();
    final int i = fqn.lastIndexOf('.');
    this.sn = i >= 0 ? new SyntheticName(fqn.substring(i + 1)) : fullyQualifiedName;
    this.fqn = fullyQualifiedName;
    this.type = new Type();
    if (elements.isEmpty()) {
      this.elements = List.of();
    } else {
      final List<InternalAnnotationElement> elements0 = new ArrayList<>(elements.size());
      for (final SyntheticAnnotationElement e : elements) {
        elements0.add(new InternalAnnotationElement(e.annotationMirrors(), e.type(), e.name(), e.defaultValue()));
      }
      this.elements = unmodifiableList(elements0);
    }
  }


  /*
   * Instance methods.
   */


  @Override // TypeElement (Element)
  public final <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
    return v.visitType(this, p);
  }

  @Override // TypeElement (Element)
  public final TypeMirror asType() {
    return this.type;
  }

  @Override // TypeElement (AnnotatedConstruct)
  public final List<? extends AnnotationMirror> getAnnotationMirrors() {
    return this.annotationMirrors;
  }

  @Override // TypeElement (AnnotatedConstruct)
  public final <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
    return null; // deliberate
  }

  @Override // TypeElement (AnnotatedConstruct)
  @SuppressWarnings("unchecked")
  public final <A extends Annotation> A[] getAnnotationsByType(final Class<A> annotationType) {
    return (A[])EMPTY_ANNOTATION_ARRAY; // deliberate
  }

  @Override // TypeElement (Element)
  public final List<? extends Element> getEnclosedElements() {
    return this.elements;
  }

  @Override // TypeElement (Element)
  public final Element getEnclosingElement() {
    return null; // really should be a package but we're synthetic
  }

  @Override // TypeElement
  public final List<? extends TypeMirror> getInterfaces() {
    return List.of();
  }

  @Override // TypeElement (Element)
  public final Set<Modifier> getModifiers() {
    return ABSTRACT_ONLY_MODIFIERS;
  }

  @Override // TypeElement (Element)
  public final ElementKind getKind() {
    return ElementKind.ANNOTATION_TYPE;
  }

  @Override // TypeElement
  public final NestingKind getNestingKind() {
    return NestingKind.TOP_LEVEL;
  }

  @Override // TypeElement (QualifiedNameable)
  public final SyntheticName getQualifiedName() {
    return this.fqn;
  }

  @Override // TypeElement (Element)
  public final SyntheticName getSimpleName() {
    return this.sn;
  }

  @Override // TypeElement
  public final TypeMirror getSuperclass() {
    return NoneType.of();
  }

  @Override // TypeElement
  public final List<? extends TypeParameterElement> getTypeParameters() {
    return List.of();
  }

  @Override // TypeElement (Element)
  public final String toString() {
    return this.getQualifiedName().toString(); // TODO: robustify
  }


  /*
   * Static methods.
   */


  // See https://docs.oracle.com/javase/specs/jls/se25/html/jls-9.html#jls-9.6.1
  private static final TypeMirror validateScalarType(final TypeMirror type) {
    return switch (type) {
    case null -> throw new NullPointerException("t");
    case PrimitiveType t when t.getKind().isPrimitive() -> t;
    case DeclaredType t when t.getKind() == DECLARED -> {
      final TypeElement te = (TypeElement)t.asElement();
      yield switch (te.getKind()) {
      case ANNOTATION_TYPE, ENUM -> t;
      default -> {
        final Name fqn = te.getQualifiedName();
        if (fqn.contentEquals("java.lang.Class") || fqn.contentEquals("java.lang.String")) {
          yield t;
        }
        throw new IllegalArgumentException("type: " + type);
      }
      };
    }
    default -> throw new IllegalArgumentException("type: " + type);
    };
  }


  /*
   * Inner and nested classes.
   */


  /**
   * An <strong>experimental</strong> collection of information out of which a synthetic annotation element may be
   * fashioned.
   *
   * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
   *
   * @see SyntheticAnnotationTypeElement#SyntheticAnnotationTypeElement(List, SyntheticName, List)
   */
  public static final class SyntheticAnnotationElement {


    /*
     * Instance fields.
     */


    private final List<? extends AnnotationMirror> annotationMirrors;

    private final TypeMirror type;

    private final SyntheticName name;

    private final SyntheticAnnotationValue defaultValue;


    /*
     * Constructors.
     */


    /**
     * Creates a new {@link SyntheticAnnotationElement}.
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
     *
     * @see #SyntheticAnnotationElement(List, TypeMirror, SyntheticName, SyntheticAnnotationValue)
     *
     * @spec https://docs.oracle.com/javase/specs/jls/se25/html/jls-9.html#jls-9.6.1 Java Language Specification,
     * section 9.6.1
     */
    public SyntheticAnnotationElement(final TypeMirror type, // the "return type"
                                      final SyntheticName name) {
      this(List.of(), type, name, null);
    }

    /**
     * Creates a new {@link SyntheticAnnotationElement}.
     *
     * @param type the type of the annotation element; must conform to Java annotation element type restrictions; must
     * not be {@code null}
     *
     * @param name the name of the annotation element; must conform to Java method naming requirements; must not be
     * {@code null}
     *
     * @param defaultValue a {@link SyntheticAnnotationValue} representing the default value; may be {@code null}
     *
     * @exception NullPointerException if any argument is {@code null}
     *
     * @exception IllegalArgumentException if any argument does not conform to its requirements
     *
     * @see #SyntheticAnnotationElement(List, TypeMirror, SyntheticName, SyntheticAnnotationValue)
     *
     * @spec https://docs.oracle.com/javase/specs/jls/se25/html/jls-9.html#jls-9.6.1 Java Language Specification,
     * section 9.6.1
     */
    public SyntheticAnnotationElement(final TypeMirror type, // the "return type"
                                      final SyntheticName name,
                                      final SyntheticAnnotationValue defaultValue) {
      this(List.of(), type, name, defaultValue);
    }

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
     *
     * @see #SyntheticAnnotationElement(List, TypeMirror, SyntheticName, SyntheticAnnotationValue)
     *
     * @spec https://docs.oracle.com/javase/specs/jls/se25/html/jls-9.html#jls-9.6.1 Java Language Specification,
     * section 9.6.1
     */
    public SyntheticAnnotationElement(final List<? extends AnnotationMirror> annotationMirrors,
                                      final TypeMirror type, // the "return type"
                                      final SyntheticName name) {
      this(annotationMirrors, type, name, null);
    }

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
     * @param defaultValue a {@link SyntheticAnnotationValue} representing the default value; may be {@code null}
     *
     * @exception NullPointerException if any argument is {@code null}
     *
     * @exception IllegalArgumentException if any argument does not conform to its requirements
     *
     * @spec https://docs.oracle.com/javase/specs/jls/se25/html/jls-9.html#jls-9.6.1 Java Language Specification,
     * section 9.6.1
     */
    public SyntheticAnnotationElement(final List<? extends AnnotationMirror> annotationMirrors,
                                      final TypeMirror type, // the "return type"
                                      final SyntheticName name,
                                      final SyntheticAnnotationValue defaultValue) {
      super();
      this.annotationMirrors = List.copyOf(annotationMirrors);
      this.type = switch (type) {
      case null -> throw new NullPointerException("type");
      case ArrayType t when t.getKind() == ARRAY -> validateScalarType(t.getComponentType());
      default -> validateScalarType(type);
      };
      if (name.equals("getClass") || name.equals("hashCode") || name.equals("toString")) {
        // java.lang.Object-declared methods that might otherwise meet annotation element requirements
        throw new IllegalArgumentException("name: " + name);
      }
      this.name = name;
      this.defaultValue = defaultValue;
    }


    /*
     * Instance fields.
     */


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
      Objects.equals(this.annotationMirrors, sae.annotationMirrors); // TODO: hmm
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
      c = this.annotationMirrors.hashCode(); // TODO: hmm
      hashCode = 17 * hashCode + c;
      return hashCode;
    }

    final SyntheticName name() {
      return this.name;
    }

    final TypeMirror type() {
      return this.type;
    }

    final SyntheticAnnotationValue defaultValue() {
      return this.defaultValue;
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
  // Note: inner class; see asElement()
  private class Type implements DeclaredType {


    /*
     * Constructors.
     */


    private Type() {
      super();
    }


    /*
     * Instance fields.
     */


    @Override // DeclaredType (TypeMirror)
    public final <R, P> R accept(final TypeVisitor<R, P> v, final P p) {
      return v.visitDeclared(this, p);
    }

    @Override // DeclaredType (AnnotatedConstruct)
    public final <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
      return null; // deliberate
    }

    @Override // DeclaredType (AnnotatedConstruct)
    public final List<? extends AnnotationMirror> getAnnotationMirrors() {
      return List.of(); // deliberate
    }

    @Override // DeclaredType (AnnotatedConstruct)
    @SuppressWarnings("unchecked")
    public final <A extends Annotation> A[] getAnnotationsByType(final Class<A> annotationType) {
      return (A[])EMPTY_ANNOTATION_ARRAY; // deliberate
    }

    @Override // DeclaredType (TypeMirror)
    public final TypeKind getKind() {
      return TypeKind.DECLARED;
    }

    @Override // DeclaredType
    public final NoneType getEnclosingType() {
      return NoneType.of();
    }

    @Override // DeclaredType
    public final List<? extends TypeMirror> getTypeArguments() {
      return List.of();
    }

    @Override // DeclaredType
    public final SyntheticAnnotationTypeElement asElement() {
      return SyntheticAnnotationTypeElement.this;
    }

    @Override // DeclaredType (TypeMirror)
    public final String toString() {
      return this.asElement().toString();
    }

  }

  // Note: inner class, built out of SyntheticAnnotationElement instances; see getEnclosingElement()
  private final class InternalAnnotationElement implements ExecutableElement {


    /*
     * Instance fields.
     */


    private final List<? extends AnnotationMirror> annotationMirrors;

    private final Type t;

    private final SyntheticName name;

    private final SyntheticAnnotationValue defaultValue;


    /*
     * Constructors.
     */


    private InternalAnnotationElement(final List<? extends AnnotationMirror> annotationMirrors,
                                      final TypeMirror type,
                                      final SyntheticName name,
                                      final SyntheticAnnotationValue defaultValue) {
      super();
      this.annotationMirrors = List.copyOf(annotationMirrors);
      this.t = new Type(type);
      this.name = requireNonNull(name, "name");
      this.defaultValue = defaultValue;
    }


    /*
     * Instance fields.
     */


    @Override // ExecutableElement
    public final <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
      return v.visitExecutable(this, p);
    }

    @Override // ExecutableElement
    public final Type asType() {
      return this.t;
    }

    @Override // ExecutableElement (AnnotatedConstruct)
    public final List<? extends AnnotationMirror> getAnnotationMirrors() {
      return this.annotationMirrors;
    }

    @Override // ExecutableElement (AnnotatedConstruct)
    public final <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
      return null; // deliberate
    }

    @Override // ExecutableElement (AnnotatedConstruct)
    @SuppressWarnings("unchecked")
    public final <A extends Annotation> A[] getAnnotationsByType(final Class<A> annotationType) {
      return (A[])EMPTY_ANNOTATION_ARRAY; // deliberate
    }

    @Override // ExecutableElement
    public AnnotationValue getDefaultValue() {
      return this.defaultValue;
    }

    @Override // ExecutableElement (Element)
    public final List<? extends Element> getEnclosedElements() {
      return List.of();
    }

    @Override // ExecutableElement (Element)
    public final SyntheticAnnotationTypeElement getEnclosingElement() {
      return SyntheticAnnotationTypeElement.this;
    }

    @Override // ExecutableElement (Element)
    public final Set<Modifier> getModifiers() {
      return ABSTRACT_ONLY_MODIFIERS;
    }

    @Override // ExecutableElement (Element)
    public final ElementKind getKind() {
      return ElementKind.METHOD;
    }

    @Override // ExecutableElement
    public final List<? extends VariableElement> getParameters() {
      return List.of();
    }

    @Override // ExecutableElement
    public final TypeMirror getReceiverType() {
      return this.asType().getReceiverType();
    }

    @Override // ExecutableElement
    public final TypeMirror getReturnType() {
      return this.asType().getReturnType();
    }

    @Override // ExecutableElement (Element)
    public final SyntheticName getSimpleName() {
      return this.name;
    }

    @Override // ExecutableElement
    public final List<? extends TypeMirror> getThrownTypes() {
      return this.asType().getThrownTypes(); // (should be zero)
    }

    @Override // ExecutableElement
    public final List<? extends TypeParameterElement> getTypeParameters() {
      return List.of();
    }

    @Override // ExecutableElement
    public final boolean isDefault() {
      return false; // technically this could be true if java.lang.annotation.Annotation ever gets a default method
    }

    @Override // ExecutableElement
    public final boolean isVarArgs() {
      return false;
    }


    /*
     * Inner and nested classes.
     */


    private static final class Type implements ExecutableType {


      /*
       * Instance fields.
       */


      private final TypeMirror type;


      /*
       * Constructors.
       */


      private Type(final TypeMirror type) { // the "return type"
        super();
        this.type = switch (type) {
        case null -> throw new NullPointerException("type");
        case ArrayType t when t.getKind() == ARRAY -> validateScalarType(t.getComponentType());
        default -> validateScalarType(type);
        };
      }


      /*
       * Instance methods.
       */


      @Override // TypeMirror
      public final <R, P> R accept(final TypeVisitor<R, P> v, final P p) {
        return v.visitExecutable(this, p);
      }

      @Override // ExecutableType (AnnotatedConstruct)
      public final <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
        return null; // deliberate
      }

      @Override // ExecutableType (AnnotatedConstruct)
      public final List<? extends AnnotationMirror> getAnnotationMirrors() {
        return List.of(); // can't actually put type annotations on an executable type to my knowledge
      }

      @Override // ExecutableType (AnnotatedConstruct)
      @SuppressWarnings("unchecked")
      public final <A extends Annotation> A[] getAnnotationsByType(final Class<A> annotationType) {
      return (A[])EMPTY_ANNOTATION_ARRAY; // deliberate
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
      public final NoneType getReceiverType() {
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
