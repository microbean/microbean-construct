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

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import javax.lang.model.util.Elements;
import javax.lang.model.util.Elements.Origin;

import org.microbean.construct.element.UniversalElement;

import org.microbean.construct.type.UniversalType;

import static java.util.Collections.unmodifiableList;

import static javax.lang.model.type.TypeKind.DECLARED;

/**
 * A representation of a domain of valid Java constructs.
 *
 * <p>A <dfn id="domain">domain</dfn> is a set of valid Java <a href="#construct">constructs</a>. A {@link Domain}
 * provides access to a domain and its members.</p>
 *
 * <p>A Java <dfn id="construct">construct</dfn> is either a <a href="#type">type</a> or an <a
 * href="#element">element</a>.</p>
 *
 * <p>A <dfn id="type">type</dfn> is a usage of a Java type, most commonly represented as a {@link TypeMirror}.</p>
 *
 * <p>An <dfn id="element">element</dfn> ia a declaration of a Java program element, most commonly represented as an
 * {@link Element}.</p>
 *
 * <p>Domains impose constraints on the <a href="#type">types</a> and <a href="#element">elements</a> they contain, and
 * on the kinds and semantics of operations that can be performed on them.</p>
 *
 * <p>This interface is modeled on a deliberately restricted combination of the {@link javax.lang.model.util.Elements}
 * and {@link javax.lang.model.util.Types} interfaces.</p>
 *
 * <p>{@link Domain} implementations must be thread-safe.</p>
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see PrimordialDomain
 *
 * @see <a href="https://bugs.openjdk.org/browse/JDK-8055219">JDK-8055219</a>
 */
@SuppressWarnings("try")
public interface Domain extends PrimordialDomain {

  /**
   * Returns an immutable, determinate, non-{@code null} {@link List} of {@link AnnotationMirror} instances representing
   * all annotations <dfn>present</dfn> on an element, whether <dfn>directly present</dfn> or present via inheritance.
   *
   * @param element the {@link Element} whose present annotations should be returned; must not be {@code null}
   *
   * @return an immutable, determinate, non-{@code null} {@link List} of {@link AnnotationMirror} instances representing
   * all annotations <dfn>present</dfn> on an element, whether <dfn>directly present</dfn> or present via inheritance
   *
   * @exception NullPointerException if {@code element} is {@code null}
   *
   * @see javax.lang.model.util.Elements#getAllAnnotationMirrors(Element)
   */
  public default List<? extends AnnotationMirror> allAnnotationMirrors(Element element) {
    element = UniversalElement.of(element, this); // handles locking, symbol completion, etc.
    final List<AnnotationMirror> annotations = new ArrayList<>(8);
    annotations.addAll(element.getAnnotationMirrors().reversed());
    TypeMirror sc = ((TypeElement)element).getSuperclass();
    if (sc.getKind() == DECLARED) {
      element = ((DeclaredType)sc).asElement();
      WHILE_LOOP:
      while (element != null && element.getKind().isDeclaredType()) {
        for (final AnnotationMirror a : element.getAnnotationMirrors().reversed()) {
          // See if it's inherited
          final TypeElement annotationTypeElement = (TypeElement)a.getAnnotationType().asElement();
          for (final AnnotationMirror metaAnnotation : annotationTypeElement.getAnnotationMirrors()) {
            if (((TypeElement)metaAnnotation.getAnnotationType().asElement()).getQualifiedName().contentEquals("java.lang.annotation.Inherited")) {
              for (final AnnotationMirror existingAnnotation : annotations) {
                if (existingAnnotation.getAnnotationType().asElement().equals(annotationTypeElement)) {
                  continue WHILE_LOOP;
                }
              }
              annotations.add(a);
              break;
            }
          }
        }
        sc = ((TypeElement)element).getSuperclass();
        element = sc.getKind() == DECLARED ? ((DeclaredType)sc).asElement() : null;
      }
    }
    return annotations.isEmpty() ? List.of() : unmodifiableList(annotations.reversed());
  }

  /**
   * Returns an {@link ArrayType} whose {@linkplain ArrayType#getComponentType() component type} is {@linkplain
   * #sameType(TypeMirror, TypeMirror) the same as} the supplied {@code componentType}.
   *
   * @param componentType the component type; must not be {@code null}
   *
   * @return a non-{@code null} {@link ArrayType} whose {@linkplain ArrayType#getComponentType() component type} is
   * {@linkplain #sameType(TypeMirror, TypeMirror) the same as} the supplied {@code componentType}
   *
   * @exception NullPointerException if {@code componentType} is {@code null}
   *
   * @exception IllegalArgumentException if {@code componentType} is not a valid component type
   *
   * @see javax.lang.model.util.Types#getArrayType(TypeMirror)
   */
  // Type factory method
  public ArrayType arrayTypeOf(final TypeMirror componentType);

  /**
   * Returns a non-{@code null} {@link TypeMirror} representing the type of the supplied {@link Element} when that
   * {@link Element} is viewed as a member of, or otherwise directly contained by, the supplied {@code containingType}.
   *
   * <p>For example, when viewed as a member of the parameterized type {@link java.util.Set Set&lt;String&gt;}, the
   * {@link java.util.Set#add(Object)} method (represented as an {@link ExecutableElement}) {@linkplain
   * ExecutableElement#asType() has} a {@linkplain ExecutableType type} whose {@linkplain
   * ExecutableType#getParameterTypes() method parameter is of type} {@link String} (not {@link String}'s erasure).</p>
   *
   * @param containingType the containing {@link DeclaredType}; must not be {@code null}
   *
   * @param e the member {@link Element}; must not be {@code null}
   *
   * @return a non-{@code null} {@linkplain TypeMirror type} representing the {@linkplain Element#asType() type of} the
   * supplied {@link Element} when viewed as a member of the supplied {@code containingType}; never {@code null}
   *
   * @exception NullPointerException if either {@code containingType} or {@code e} is {@code null}
   *
   * @exception IllegalArgumentException if {@code e} cannot be viewed as a member of the supplied {@code
   * containingType} (because it is the wrong {@linkplain Element#getKind() kind}, for example)
   *
   * @see javax.lang.model.util.Types#asMemberOf(DeclaredType, Element)
   */
  public TypeMirror asMemberOf(final DeclaredType containingType, final Element e);

  /**
   * Returns {@code true} if and only if the supplied {@code payload} (the first argument) is considered assignable to
   * the supplied {@code receiver} (the second argument) according to <a
   * href="https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.2">the rules of the Java Language
   * Specification</a>; <strong>note the counterintuitive order of the parameters</strong>.
   *
   * <p>Perhaps surprisingly, the "left hand side" of the putative assignment is represented by the second parameter
   * ({@code receiver}). The "right hand side" of the putative assignment is represented by the first parameter
   * ({@code payload}). This follows the contract of the {@link javax.lang.model.util.Types#isAssignable(TypeMirror,
   * TypeMirror)} method, on which this method is modeled.</p>
   *
   * @param payload the {@link TypeMirror} being assigned; must not be {@code null}
   *
   * @param receiver the {@link TypeMirror} receiving the assignment; must not be {@code null}
   *
   * @return {@code true} if and only if {@code payload} is assignable to {@code receiver} according to <a
   * href="https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.2">the rules of the Java Language
   * Specification</a>
   *
   * @exception NullPointerException if either {@code payload} or {@code receiver} is {@code null}
   *
   * @exception IllegalArgumentException if either {@link TypeMirror} is not one that can take part in an assignment
   *
   * @see javax.lang.model.util.Types#isAssignable(TypeMirror, TypeMirror)
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.2 Java Language Specification, section
   * 5.2
   */
  // Note the strange positioning of payload and receiver.
  public boolean assignable(final TypeMirror payload, final TypeMirror receiver);

  /**
   * Returns the (non-{@code null}) <a
   * href="https://docs.oracle.com/javase/specs/jls/se21/html/jls-13.html#jls-13.1"><dfn>binary name</dfn></a> of the
   * supplied {@link TypeElement}.
   *
   * @param e a {@link TypeElement}; must not be {@code null}
   *
   * @return a non-{@code null} {@link Name}
   *
   * @exception NullPointerException if {@code e} is {@code null}
   *
   * @see javax.lang.model.util.Elements#getBinaryName(TypeElement)
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se21/html/jls-13.html#jls-13.1 Java Language Specification, section
   * 13.1
   */
  public Name binaryName(final TypeElement e);

  /**
   * Returns {@code true} if and only if the supplied {@link ExecutableElement} represents a <dfn>bridge method</dfn>.
   *
   * @param e an {@link ExecutableElement}; must not be {@code null}
   *
   * @return {@code true} if and only if the supplied {@link ExecutableElement} represents a bridge method
   *
   * @exception NullPointerException if {@code e} is {@code null}
   *
   * @see javax.lang.model.util.Elements#isBridge(ExecutableElement)
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-8.html#jls-8.4.8.3 Java Language Specification,
   * section 8.4.8.3
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-15.html#jls-15.12.4.5 Java Language Specification,
   * section 15.12.4.5
   */
  public boolean bridge(final ExecutableElement e);

  /**
   * Applies <a href="https://docs.oracle.com/javase/specs/jls/se23/html/jls-5.html#jls-5.1.10"><dfn>capture
   * conversion</dfn></a> to the supplied {@link TypeMirror}, which is normally a {@linkplain TypeKind#WILDCARD wildcard
   * type}.
   *
   * @param wildcard a {@link TypeMirror}; must not be {@code null}; if not a {@linkplain TypeKind#WILDCARD wildcard
   * type}, then it will be returned unchanged
   *
   * @return a non-{@code null} {@link TypeMirror} representing the result of <a
   * href="https://docs.oracle.com/javase/specs/jls/se23/html/jls-5.html#jls-5.1.10">capture conversion</a> applied to
   * the supplied {@link TypeMirror}
   *
   * @exception NullPointerException if {@code wildcard} is {@code null}
   *
   * @see javax.lang.model.util.Types#capture(TypeMirror)
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-5.html#jls-5.1.10 Java Language Specification, section
   * 5.1.10
   */
  public TypeMirror capture(final TypeMirror wildcard);

  /**
   * Returns {@code true} if and only if {@code candidateContainer} <dfn>contains</dfn> {@code candidate}, according to
   * the <a href="https://docs.oracle.com/javase/specs/jls/se23/html/jls-4.html#jls-4.5.1">Java Language Specification,
   * section 4.5.1</a>.
   *
   * @param candidateContainer the putative containing type; normally a {@linkplain TypeKind#WILDCARD wildcard type};
   * must not be {@code null}
   *
   * @param candidate the putative contained type; must not be {@code null}
   *
   * @return {@code true} if and only if {@code candidateContainer} <dfn>contains</dfn> {@code candidate}, according to
   * the <a href="https://docs.oracle.com/javase/specs/jls/se23/html/jls-4.html#jls-4.5.1">Java Language Specification,
   * section 4.5.1</a>; {@code false} otherwise
   *
   * @exception NullPointerException if either argument is {@code null}
   *
   * @exception IllegalArgumentException if either argument is either an {@linkplain TypeKind#EXECUTABLE executable
   * type}, a {@linkplain TypeKind#MODULE module type}, or a {@linkplain TypeKind#PACKAGE package type}
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-4.html#jls-4.5.1 Java Language Specification, section
   * 4.5.1
   */
  public boolean contains(final TypeMirror candidateContainer, final TypeMirror candidate);

  /**
   * A convenience method that returns the {@link DeclaredType} {@linkplain TypeElement#asType() of} a {@link
   * TypeElement} that bears the supplied {@code canonicalName}, <strong>or {@code null} if there is no such {@link
   * TypeElement} (and therefore no such {@link DeclaredType})</strong>.
   *
   * @param canonicalName a valid canonical name; must not be {@code null}
   *
   * @return a {@link DeclaredType} with a {@link TypeKind} of {@link TypeKind#DECLARED}, or {@code null}
   *
   * @see #typeElement(CharSequence)
   *
   * @see #declaredType(TypeElement, TypeMirror...)
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-6.html#jls-6.7 Java Language Specification, section
   * 6.7
   */
  // (Convenience.)
  public default DeclaredType declaredType(final CharSequence canonicalName) {
    final TypeElement e = this.typeElement(canonicalName);
    return e == null ? null : this.declaredType(e);
  }

  /**
   * Returns the {@link DeclaredType} {@linkplain TypeElement#asType() of} the supplied {@link TypeElement} with the
   * supplied {@link TypeMirror} arguments (if any), yielding a parameterized type.
   *
   * <p>Given a {@link TypeElement} representing the class named {@link java.util.Set java.util.Set} and a {@link
   * TypeMirror} representing the type declared by {@link String java.lang.String}, for example, this method will return
   * a {@link DeclaredType} representing the parameterized type corresponding to {@link java.util.Set
   * java.util.Set&lt;java.lang.String&gt;}.</p>
   *
   * <p>The number of supplied type arguments must either equal the number of the supplied {@link TypeElement}'s
   * {@linkplain TypeElement#getTypeParameters() formal type parameters}, or must be zero. If it is zero, and if the
   * supplied {@link TypeElement} {@linkplain #generic(Element) is generic}, then the supplied {@link TypeElement}'s raw
   * type is returned.</p>
   *
   * <p>If a parameterized type is returned, {@linkplain DeclaredType#asElement() its <code>TypeElement</code>} must not
   * be contained within a {@linkplain #generic(Element) generic} outer class. The parameterized type {@code
   * Outer<String>.Inner<Number>}, for example, may be constructed by first using this method to get the type {@code
   * Outer<String>}, and then invoking {@link #declaredType(DeclaredType, TypeElement, TypeMirror...)}.</p>
   *
   * @param typeElement a {@link TypeElement}; must not be {@code null}
   *
   * @param typeArguments any type arguments (represented by {@link TypeMirror}s); must not be {@code null}
   *
   * @return a non-{@code null} {@link DeclaredType} with a {@link TypeKind} of {@link TypeKind#DECLARED}
   *
   * @exception NullPointerException if {@code typeElement} or {@code typeArguments} is {@code null}
   *
   * @see javax.lang.model.util.Types#getDeclaredType(TypeElement, TypeMirror...)
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-4.html#jls-4.5 Java Language Specification, section 4.5
   */
  // Type factory method.
  public DeclaredType declaredType(final TypeElement typeElement,
                                   final TypeMirror... typeArguments);

  /**
   * Returns the {@link DeclaredType} {@linkplain TypeElement#asType() of} the supplied {@link TypeElement} with the
   * supplied {@link TypeMirror} arguments (if any), given a containing {@link DeclaredType} of which it is a member,
   * yielding a parameterized type.
   *
   * <p>Given a {@link DeclaredType} representing the parameterized type corresponding to {@code Outer<}{@link
   * String}{@code >} (see the {@link #declaredType(TypeElement, TypeMirror...)} method), a {@link TypeElement}
   * representing the class named {@code Outer.Inner} and a {@link DeclaredType} representing the non-generic class
   * corresponding to {@link Number}, for example, this method will return a {@link DeclaredType} representing the
   * parameterized type corresponding to {@code Outer<}{@link String}{@code >}{@code .Inner<}{@link Number}{@code >}.</p>
   *
   * <p>The number of supplied type arguments must either equal the number of the supplied {@link TypeElement}'s
   * {@linkplain TypeElement#getTypeParameters() formal type parameters}, or must be zero. If it is zero, and if the
   * supplied {@link TypeElement} {@link #generic(Element) is generic}, then the supplied {@link TypeElement}'s raw type
   * is returned.</p>
   *
   * @param enclosingType a {@link DeclaredType} representing the containing type; must not be {@code null}
   *
   * @param typeElement a {@link TypeElement}; must not be {@code null}
   *
   * @param typeArguments any type arguments (represented by {@link TypeMirror}s); must not be {@code null}
   *
   * @return a non-{@code null} {@link DeclaredType} with a {@link TypeKind} of {@link TypeKind#DECLARED}
   *
   * @exception NullPointerException if {@code enclosingType}, {@code typeElement}, or {@code typeArguments} is {@code
   * null}
   *
   * @see #declaredType(TypeElement, TypeMirror...)
   *
   * @see javax.lang.model.util.Types#getDeclaredType(DeclaredType, TypeElement, TypeMirror...)
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-4.html#jls-4.5 Java Language Specification, section 4.5
   */
  // Type factory method.
  public DeclaredType declaredType(final DeclaredType enclosingType,
                                   final TypeElement typeElement,
                                   final TypeMirror... typeArguments);

  /**
   * Returns a non-{@code null} {@link List} of the <dfn>direct supertypes</dfn> of the supplied {@link TypeMirror},
   * which is normally a {@linkplain TypeKind#DECLARED declared type}.
   *
   * <p>The direct supertypes returned by this method are actually a subset of the direct supertypes of a type as
   * defined in the <a href="https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html#jls-4.10">Java Language
   * Specification, section 4.10</a>. Specifically, the subset contains only those types that can be expressed in the
   * {@code extends} or {@code implements} clauses of the Java language. For example, a type {@code Baz} can declare
   * only that it {@code extends Foo<Bar>}, not {@code Foo<?>}, even though {@code Foo<?>} is a specification-described
   * direct supertype of {@code Baz}.</p>
   *
   * @param t a {@link TypeMirror}; must not be {@code null}; must not be an {@linkplain TypeKind#EXECUTABLE executable
   * type}, a {@linkplain TypeKind#MODULE module type}, or a {@linkplain TypeKind#PACKAGE package type}
   *
   * @return a non-{@code null}, immutable {@link List} of {@link TypeMirror}s representing the direct supertypes
   *
   * @exception NullPointerException if {@code t} is {@code null}
   *
   * @exception IllegalArgumentException if either argument is either an {@linkplain TypeKind#EXECUTABLE executable
   * type}, a {@linkplain TypeKind#MODULE module type}, or a {@linkplain TypeKind#PACKAGE package type}
   *
   * @see javax.lang.model.util.Types#directSupertypes(TypeMirror)
   *
   * @see <a href="https://bugs.openjdk.org/browse/JDK-8055219">JDK-8055219</a>
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html#jls-4.10 Java Language Specification, section
   * 4.10
   */
  public List<? extends TypeMirror> directSupertypes(final TypeMirror t);

  /**
   * Returns the {@link Element} responsible for declaring the supplied {@link TypeMirror}, which is most commonly a
   * {@link DeclaredType}, a {@link TypeVariable}, a {@link NoType} with a {@link TypeKind} of {@link TypeKind#MODULE},
   * or a {@link NoType} with a {@link TypeKind} of {@link TypeKind#PACKAGE}, <strong>or {@code null} if there is no
   * such {@link Element}</strong>.
   *
   * @param t a {@link TypeMirror}; must not be {@code null}
   *
   * @return an {@link Element}, or {@code null}
   *
   * @exception NullPointerException if {@code t} is {@code null}
   *
   * @see javax.lang.model.util.Types#asElement(TypeMirror)
   */
  public Element element(final TypeMirror t);

  /**
   * A convenience method that returns the <dfn>element type</dfn> of the supplied {@link TypeMirror}.
   *
   * <p>The element type of an {@linkplain TypeKind#ARRAY array type} is the element type of its {@linkplain
   * ArrayType#getComponentType() component type}.</p>.
   *
   * <p>The element type of every other kind of type is the type itself. Note that the semantics of the prior sentence
   * diverge deliberately, primarily for convenience, from those of the relevant section in the Java Language
   * Specification.</p>
   *
   * @param t a {@link TypeMirror}; must not be {@code null}
   *
   * @return the <dfn>element type</dfn> of the supplied {@link TypeMirror}; never {@code null}
   *
   * @exception NullPointerException if {@code t} is {@code null}
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-10.html#jls-10.1 Java Language Specification, section
   * 10.1
   */
  // (Convenience.)
  public default TypeMirror elementType(final TypeMirror t) {
    return switch (t) {
    case null -> throw new NullPointerException("t");
    case UniversalType ut -> ut.elementType();
    default -> {
      try (var lock = lock()) {
        yield t.getKind() == TypeKind.ARRAY ? this.elementType(((ArrayType)t).getComponentType()) : t;
      }
    }
    };
  }

  /**
   * Returns the <dfn>erasure</dfn> of the supplied {@link TypeMirror}.
   *
   * @param <T> a {@link TypeMirror} specialization
   *
   * @param t the {@link TypeMirror} representing the type whose erasure should be returned; must not be {@code null}
   *
   * @return the erasure of the supplied {@link TypeMirror}; never {@code null}
   *
   * @exception NullPointerException if {@code t} is {@code null}
   *
   * @exception IllegalArgumentException if {@code t} is a {@link NoType} with a {@link TypeKind} of {@link
   * TypeKind#MODULE}, or a {@link NoType} with a {@link TypeKind} of {@link TypeKind#PACKAGE}
   *
   * @see javax.lang.model.util.Types#erasure(TypeMirror)
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-4.html#jls-4.6 Java Language Specification, section
   * 4.6
   */
  public <T extends TypeMirror> T erasure(final T t);

  /**
   * Returns an {@link ExecutableElement} corresponding to the supplied {@link Executable}.
   *
   * @param e an {@link Executable}; must not be {@code null}
   *
   * @return an {@link ExecutableElement} corresponding to the supplied {@link Executable}; never {@code null}
   *
   * @exception NullPointerException if {@code e} is {@code null}
   *
   * @exception IllegalArgumentException if somehow {@code e} is neither a {@link Constructor} nor a {@link Method}
   */
  // (Convenience.)
  public default ExecutableElement executableElement(final Executable e) {
    return switch (e) {
    case null -> throw new NullPointerException("e");
    case Constructor<?> c ->
      this.executableElement(this.typeElement(c.getDeclaringClass().getCanonicalName()),
                             this.noType(TypeKind.VOID),
                             "<init>",
                             this.types(c.getParameterTypes()));
    case Method m ->
      this.executableElement(this.typeElement(m.getDeclaringClass().getCanonicalName()),
                             this.type(m.getReturnType()),
                             m.getName(),
                             this.types(m.getParameterTypes()));
    default -> throw new IllegalArgumentException("e: " + e);
    };
  }


  /**
   * A convenience method that returns an {@link ExecutableElement} representing the static initializer, constructor or
   * method described by the supplied arguments, <strong>or {@code null} if no such {@link ExecutableElement}
   * exists</strong>.
   *
   * @param declaringElement a {@link TypeElement} representing the class that declares the executable; must not be
   * {@code null}
   *
   * @param returnType the {@linkplain ExecutableElement#getReturnType() return type} of the executable; must not be
   * {@code null}
   *
   * @param name the {@linkplain ExecutableElement#getSimpleName() name} of the executable; must not be {@code null}
   *
   * @param parameterTypes {@link TypeMirror}s that represent the executable's {@linkplain
   * ExecutableElement#getParameters() parameter types}
   *
   * @return an {@link ExecutableElement} with an {@link ElementKind} of {@link ElementKind#CONSTRUCTOR}, {@link
   * ElementKind#METHOD}, or {@link ElementKind#STATIC_INIT}, or {@code null}
   *
   * @exception NullPointerException if any argument is {@code null}
   */
  // (Convenience.)
  public default ExecutableElement executableElement(final TypeElement declaringElement,
                                                     final TypeMirror returnType,
                                                     final CharSequence name,
                                                     final TypeMirror... parameterTypes) {
    return switch (declaringElement) {
    case null -> throw new NullPointerException("declaringElement");
    case UniversalElement ue -> {
      final List<? extends UniversalElement> ees = ue.getEnclosedElements();
      yield ees.stream()
        .sequential()
        .filter(e -> e.getKind().isExecutable() && e.getSimpleName().contentEquals(name))
        .map(UniversalElement.class::cast)
        .filter(ee -> {
            if (!this.sameType(returnType, ee.getReturnType())) {
              return false;
            }
            final List<? extends UniversalElement> ps = ee.getParameters();
            if (ps.size() != parameterTypes.length) {
              return false;
            }
            for (int i = 0; i < parameterTypes.length; i++) {
              if (!this.sameType(ps.get(i).asType(), parameterTypes[i])) {
                return false;
              }
            }
            return true;
          })
        .findFirst()
        .orElse(null);
    }
    default -> {
      try (var lock = this.lock()) {
        final List<? extends Element> ees = declaringElement.getEnclosedElements();
        yield ees.stream()
          .sequential()
          .filter(e -> e.getKind().isExecutable() && e.getSimpleName().contentEquals(name))
          .map(ExecutableElement.class::cast)
          .filter(ee -> {
              if (!this.sameType(returnType, ee.getReturnType())) {
                return false;
              }
              final List<? extends VariableElement> ps = ee.getParameters();
              if (ps.size() != parameterTypes.length) {
                return false;
              }
              for (int i = 0; i < parameterTypes.length; i++) {
                if (!this.sameType(ps.get(i).asType(), parameterTypes[i])) {
                  return false;
                }
              }
              return true;
            })
          .findFirst()
          .orElse(null);
      }
    }
    };
  }

  /**
   * A convenience method that returns {@code true} if and only if the supplied {@link Element} is <dfn>generic</dfn>.
   *
   * @param e an {@link Element}; must not be {@code null}
   *
   * @return {@code true} if and only if the supplied {@link Element} is <dfn>generic</dfn>; {@code false} otherwise
   *
   * @exception NullPointerException if {@code e} is {@code null}
   *
   * @see Parameterizable
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-8.html#jls-8.1.2 Java Language Specification, section
   * 8.1.2
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-8.html#jls-8.4.4 Java Language Specification, section
   * 8.4.4
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-8.html#jls-8.8.4 Java Language Specification, section
   * 8.8.4
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-9.html#jls-9.1.2 Java Language Specification, section
   * 9.1.2
   */
  // (Convenience.)
  public default boolean generic(final Element e) {
    return switch (e) {
    case null -> throw new NullPointerException("e");
    case UniversalElement ue -> ue.generic();
    case Parameterizable p -> {
      try (var lock = this.lock()) {
        yield switch (e.getKind()) {
        case CLASS, CONSTRUCTOR, ENUM, INTERFACE, METHOD, RECORD -> !p.getTypeParameters().isEmpty();
        default -> false;
        };
      }
    }
    default -> false;
    };
  }

  /**
   * A convenience method that returns {@code true} if and only if the supplied {@link TypeMirror} is declared by an
   * {@link Element} that {@linkplain #generic(Element) is generic}.
   *
   * @param t a {@link TypeMirror}; must not be {@code null}
   *
   * @return {@code true} if and only if the supplied {@link TypeMirror} is declared by an {@link Element} that
   * {@linkplain #generic(Element) is generic}
   *
   * @exception NullPointerException if {@code t} is {@code null}
   *
   * @see #generic(Element)
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-8.html#jls-8.1.2 Java Language Specification, section
   * 8.1.2
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-8.html#jls-8.4.4 Java Language Specification, section
   * 8.4.4
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-8.html#jls-8.8.4 Java Language Specification, section
   * 8.8.4
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-9.html#jls-9.1.2 Java Language Specification, section
   * 9.1.2
   */
  // (Convenience.)
  public default boolean generic(final TypeMirror t) {
    return switch (t) {
    case null -> throw new NullPointerException("t");
    case UniversalType ut -> ut.generic();
    default -> {
      try (var lock = this.lock()) {
        final Element e = this.element(t);
        yield e != null && this.generic(e);
      }
    }
    };
  }

  /**
   * A convenience method that returns {@code true} if and only if the supplied {@link Element} represents the (essentially
   * primordial) {@code java.lang.Object} {@link Element}.
   *
   * @param e an {@link Element}; must not be {@code null}
   *
   * @return {@code true} if and only if the supplied {@link Element} represents the (essentially
   * primordial) {@code java.lang.Object} {@link Element}; {@code false} otherwise
   *
   * @exception NullPointerException if {@code e} is {@code null}
   */
  // (Convenience.)
  public default boolean javaLangObject(final Element e) {
    return switch (e) {
    case null -> throw new NullPointerException("e");
    case UniversalElement ue -> ue.javaLangObject();
    default -> {
      try (var lock = this.lock()) {
        yield
          e.getKind() == ElementKind.CLASS &&
          ((QualifiedNameable)e).getQualifiedName().contentEquals("java.lang.Object");
      }
    }
    };
  }

  /**
   * A convenience method that returns {@code true} if and only if the supplied {@link TypeMirror} represents the {@link
   * DeclaredType} declared by the (essentially primordial) {@code java.lang.Object} element.
   *
   * @param t a {@link TypeMirror}; must not be {@code null}
   *
   * @return {@code true} represents the {@link
   * DeclaredType} declared by the (essentially primordial) {@code java.lang.Object} element; {@code false} otherwise
   *
   * @exception NullPointerException if {@code t} is {@code null}
   *
   * @see #javaLangObject(Element)
   */
  // (Convenience.)
  public default boolean javaLangObject(final TypeMirror t) {
    return switch (t) {
    case null -> throw new NullPointerException("t");
    case UniversalType ut -> ut.javaLangObject();
    default -> {
      try (var lock = this.lock()) {
        yield
          t.getKind() == DECLARED &&
          javaLangObject(((DeclaredType)t).asElement());
      }
    }
    };
  }

  /**
   * A convenience method that returns the {@link TypeElement} representing the class named {@link Object
   * java.lang.Object}.
   *
   * @return a non-{@code null} {@link TypeElement} whose {@linkplain TypeElement#getQualifiedName() qualified name} is
   * {@linkplain Name#contentEquals(CharSequence) equal to} {@code java.lang.Object}
   *
   * @see #typeElement(CharSequence)
   */
  // (Convenience.)
  public default TypeElement javaLangObject() {
    return this.typeElement("java.lang.Object");
  }

  // (Convenience.)
  @Override // PrimordialDomain
  public default DeclaredType javaLangObjectType() {
    return (DeclaredType)this.javaLangObject().asType();
  }

  /**
   * Returns a {@link ModuleElement} representing the module {@linkplain ModuleElement#getQualifiedName() named} by the
   * supplied {@code qualifiedName}, <strong>or {@code null} if there is no such {@link ModuleElement}</strong>.
   *
   * @param qualifiedName a name suitable for naming a module; must not be {@code null}; may be {@linkplain
   * CharSequence#isEmpty() empty}, in which case a {@link ModuleElement} representing an <dfn>unnamed module</dfn> will
   * be returned
   *
   * @return a {@link ModuleElement}, or {@code null}
   *
   * @exception NullPointerException if {@code qualifiedName} is {@code null}
   *
   * @see javax.lang.model.util.Elements#getModuleElement(CharSequence)
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-7.html#jls-7.7 Java Language Specification, section
   * 7.7
   */
  // Element factory method.
  public ModuleElement moduleElement(final CharSequence qualifiedName);

  /**
   * Returns a {@link Name} representing the supplied {@link CharSequence}.
   *
   * @param name a {@link CharSequence}; must not be {@code null}
   *
   * @return a non-{@code null} {@link Name} representing the supplied {@link name}
   *
   * @exception NullPointerException if {@code name} is {@code null}
   *
   * @see #lock()
   *
   * @see javax.lang.model.util.Elements#getName(CharSequence)
   */
  // Element factory method.
  public Name name(final CharSequence name);

  /**
   * Returns a {@link NoType} {@linkplain TypeMirror#getKind() bearing} the supplied {@link TypeKind}, if the supplied
   * {@link TypeKind} is either {@link TypeKind#NONE} or {@link TypeKind#VOID}.
   *
   * @param kind a {@link TypeKind}; must be either {@link TypeKind#NONE} or {@link TypeKind#VOID}
   *
   * @return a non-{@code null} {@link NoType} {@linkplain TypeMirror#getKind() bearing} the supplied {@link TypeKind}
   *
   * @exception NullPointerException if {@code kind} is {@code null}
   *
   * @exception IllegalArgumentException if {@code kind} is non-{@code null} and neither {@link TypeKind#NONE} nor
   * {@link TypeKind#VOID}
   *
   * @see TypeKind#NONE
   *
   * @see TypeKind#VOID
   *
   * @see javax.lang.model.util.Types#getNoType(TypeKind)
   *
   * @see NoType
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-8.html#jls-8.4.5 Java Language Specification, section
   * 8.4.5
   */
  // Type factory method.
  @Override // PrimordialDomain
  public NoType noType(final TypeKind kind);

  /**
   * Returns a {@link NullType} implementation {@linkplain TypeMirror#getKind() whose <code>TypeKind</code>} is {@link
   * TypeKind#NULL}.
   *
   * @return a non-{@code null} {@link NullType}
   *
   * @see javax.lang.model.util.Types#getNullType()
   *
   * @see NullType
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-4.html#jls-4.1 Java Language Specification, section 4.1
   */
  // Type factory method.
  @Override // PrimordialDomain
  public NullType nullType();

  /**
   * Returns the {@linkplain Origin origin} of the supplied {@link Element}.
   *
   * @param e a non-{@code null} {@link Element}
   *
   * @return a non-{@code null} {@link Origin}
   *
   * @see Elements#getOrigin(Element)
   *
   * @see Origin
   */
  public Origin origin(final Element e);

  /**
   * Returns a {@link PackageElement} representing the package bearing the supplied {@code canonicalName}, <strong>or
   * {@code null} if there is no such {@link PackageElement}</strong>.
   *
   * @param canonicalName a canonical name suitable for naming a package; must not be {@code null}; may be {@linkplain
   * CharSequence#isEmpty() empty}, in which case a {@link ModuleElement} representing an <dfn>unnamed package</dfn> will
   * be returned
   *
   * @return a {@link PackageElement}, or {@code null}
   *
   * @exception NullPointerException if {@code canonicalName} is {@code null}
   *
   * @see javax.lang.model.util.Elements#getPackageElement(CharSequence)
   *
   * @see PackageElement
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-6.html#jls-6.7 Java Language Specification, section
   * 6.7
   */
  // Element factory method.
  public PackageElement packageElement(final CharSequence canonicalName);

  /**
   * Returns a {@link PackageElement} representing the package bearing the supplied {@code canonicalName} as seen from
   * the module represented by the supplied {@link ModuleElement}, <strong>or {@code null} if there is no such {@link
   * PackageElement}</strong>.
   *
   * @param asSeenFrom a {@link ModuleElement}; must not be {@code null}
   *
   * @param canonicalName a canonical name suitable for naming a package; must not be {@code null}; may be {@linkplain
   * CharSequence#isEmpty() empty}, in which case a {@link ModuleElement} representing an <dfn>unnamed package</dfn> will
   * be returned
   *
   * @return a {@link PackageElement}, or {@code null}
   *
   * @exception NullPointerException if either {@code asSeenFrom} or {@code canonicalName} is {@code null}
   *
   * @see javax.lang.model.util.Elements#getPackageElement(ModuleElement, CharSequence)
   *
   * @see PackageElement
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-6.html#jls-6.7 Java Language Specification, section
   * 6.7
   */
  // Element factory method.
  public PackageElement packageElement(final ModuleElement asSeenFrom, final CharSequence canonicalName);

  /**
   * Returns a {@link Parameterizable} corresponding to the supplied (reflective) {@link GenericDeclaration}.
   *
   * @param gd a {@link GenericDeclaration}; must not be {@code null}
   *
   * @return a {@link Parameterizable} corresponding to the supplied {@link GenericDeclaration}; never {@code null}
   *
   * @exception NullPointerException if {@code gd} is {@code null}
   *
   * @exception IllegalArgumentException if {@code gd} is neither a {@link Class} nor an {@link Executable}
   *
   * @see Parameterizable
   */
  // (Convenience.)
  public default Parameterizable parameterizable(final GenericDeclaration gd) {
    return switch (gd) {
    case null -> throw new NullPointerException("gd");
    case Class<?> c -> this.typeElement(c.getCanonicalName());
    case Executable e -> this.executableElement(e);
    default -> throw new IllegalArgumentException("gd: " + gd);
    };
  }

  /**
   * A convenience method that returns {@code true} if and only if {@code t} is a {@link DeclaredType}, {@linkplain
   * TypeMirror#getKind() has a <code>TypeKind</code>} of {@link TypeKind#DECLARED DECLARED}, and {@linkplain
   * DeclaredType#getTypeArguments() has a non-empty type arguments list}.
   *
   * @param t a {@link TypeMirror}; must not be {@code null}
   *
   * @return {@code true} if and only if {@code t} is a {@link DeclaredType}, {@linkplain
   * TypeMirror#getKind() has a <code>TypeKind</code>} of {@link TypeKind#DECLARED DECLARED}, and {@linkplain
   * DeclaredType#getTypeArguments() has a non-empty type arguments list}; {@code false} otherwise
   *
   * @exception NullPointerException if {@code t} is {@code null}
   */
  // (Convenience.)
  public default boolean parameterized(final TypeMirror t) {
    return switch (t) {
    case null -> throw new NullPointerException("t");
    case UniversalType ut -> ut.parameterized();
    default -> {
      try (var lock = this.lock()) {
        yield t.getKind() == DECLARED && !((DeclaredType)t).getTypeArguments().isEmpty();
      }
    }
    };
  }

  /**
   * Returns the result of applying <dfn>unboxing conversion</dfn> to the (logical) {@link TypeElement} bearing the
   * supplied {@code canonicalName}.
   *
   * @param canonicalName a canonical name of either a primitive type or a so-called "wrapper" type; must not be {@code
   * null}
   *
   * @return a non-{@code null} {@link PrimitiveType} with a {@linkplain TypeKind#isPrimitive() primitive} {@link
   * TypeKind}
   *
   * @exception NullPointerException if {@code canonicalName} is {@code null}
   *
   * @exception IllegalArgumentException if {@code canonicalName} {@linkplain #toString(CharSequence) converted to a
   * <code>String</code>} {@linkplain String#equals(Object) is not equal to} {@code boolean}, {@code byte}, {@code
   * char}, {@code double}, {@code float}, {@code int}, {@code long}, {@code short}, {@link Boolean java.lang.Boolean},
   * {@link Byte java.lang.Byte}, {@link Character java.lang.Character}, {@link Double java.lang.Double}, {@link Float
   * java.lang.Float}, {@link Integer java.lang.Integer}, {@link Long java.lang.Long}, or {@link Short java.lang.Short}
   *
   * @see #primitiveType(TypeKind)
   *
   * @see PrimitiveType
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-5.html#jls-5.1.8 Java Language Specification, section
   * 5.1.8
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-6.html#jls-6.7 Java Language Specification, section
   * 6.7
   */
  // (Convenience.)
  // (Unboxing.)
  public default PrimitiveType primitiveType(final CharSequence canonicalName) {
    final String s = this.toString(Objects.requireNonNull(canonicalName, "canonicalName"));
    return switch (s) {
    case "boolean", "java.lang.Boolean" -> this.primitiveType(TypeKind.BOOLEAN);
    case "byte", "java.lang.Byte" -> this.primitiveType(TypeKind.BYTE);
    case "char", "java.lang.Character" -> this.primitiveType(TypeKind.CHAR);
    case "double", "java.lang.Double" -> this.primitiveType(TypeKind.DOUBLE);
    case "float", "java.lang.Float" -> this.primitiveType(TypeKind.FLOAT);
    case "int", "java.lang.Integer" -> this.primitiveType(TypeKind.INT);
    case "long", "java.lang.Long" -> this.primitiveType(TypeKind.LONG);
    case "short", "java.lang.Short" -> this.primitiveType(TypeKind.SHORT);
    default -> throw new IllegalArgumentException("canonicalName: " + s);
    };
  }

  /**
   * Returns the result of applying <dfn>unboxing conversion</dfn> to the {@linkplain Element#asType() type declared by
   * the supplied <code>TypeElement</code>}.
   *
   * @param e a {@link TypeElement}; must not be {@code null}
   *
   * @return a non-{@code null} {@link PrimitiveType} with a {@linkplain TypeKind#isPrimitive() primitive} {@link
   * TypeKind}
   *
   * @exception NullPointerException if {@code e} is {@code null}
   *
   * @exception IllegalArgumentException if there is no unboxing conversion that can be applied to the {@linkplain
   * Element#asType() type declared by <code>e</code>}
   *
   * @see javax.lang.model.util.Types#unboxedType(TypeMirror)
   *
   * @see #primitiveType(TypeMirror)
   *
   * @see PrimitiveType
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-5.html#jls-5.1.8 Java Language Specification, section
   * 5.1.8
   */
  // (Convenience.)
  // (Unboxing.)
  public default PrimitiveType primitiveType(final TypeElement e) {
    return this.primitiveType(e.asType());
  }

  /**
   * Returns the {@link PrimitiveType} corresponding to the supplied {@link TypeKind} (if it {@linkplain
   * TypeKind#isPrimitive() is primitive}).
   *
   * @param kind a {@linkplain TypeKind#isPrimitive() primitive} {@link TypeKind}; must not be {@code null}
   *
   * @return a non-{@code null} {@link PrimitiveType} {@linkplain TypeMirror#getKind() with} a {@linkplain
   * TypeKind#isPrimitive() primitive} {@link TypeKind}
   *
   * @exception NullPointerException if {@code kind} is {@code null}
   *
   * @exception IllegalArgumentException if {@code kind} {@linkplain TypeKind#isPrimitive() is not primitive}
   *
   * @see javax.lang.model.util.Types#getPrimitiveType(TypeKind)
   *
   * @see PrimitiveType
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-4.html#jls-4.2 Java Language Specification, section
   * 4.2
   */
  // (Canonical.)
  // Type factory method.
  public PrimitiveType primitiveType(final TypeKind kind);

  /**
   * Returns the result of applying <dfn>unboxing conversion</dfn> to the supplied {@link TypeMirror}.
   *
   * @param t a {@link TypeMirror}; must not be {@code null}
   *
   * @return a non-{@code null} {@link PrimitiveType} with a {@linkplain TypeKind#isPrimitive() primitive} {@link
   * TypeKind}
   *
   * @exception NullPointerException if {@code t} is {@code null}
   *
   * @exception IllegalArgumentException if there is no unboxing conversion that can be applied to {@code t}
   *
   * @see javax.lang.model.util.Types#unboxedType(TypeMirror)
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-5.html#jls-5.1.8 Java Language Specification, section
   * 5.1.8
   */
  // (Canonical.)
  // (Unboxing.)
  // Type factory method.
  public PrimitiveType primitiveType(final TypeMirror t);

  /**
   * A convenience method that returns {@code true} if and only if the supplied {@link TypeMirror} represents a
   * <dfn>prototypical type</dfn>.
   *
   * <p>Prototypical types are not defined by the Java Language Specification. They are partially defined by the
   * {@linkplain TypeElement#asType() specification of the <code>TypeElement#asType()</code>
   * method}.</p>
   *
   * @param t a {@link TypeMirror}; must not be {@code null}
   *
   * @return {@code true} if and only if {@code t} represents a <dfn>prototypical type</dfn>
   *
   * @exception NullPointerException if {@code t} is {@code null}
   *
   * @see TypeElement#asType()
   */
  // (Convenience.)
  public default boolean prototypical(final TypeMirror t) {
    return switch (t) {
    case null -> throw new NullPointerException("t");
    case UniversalType ut -> ut.prototypical();
    default -> {
      try (var lock = this.lock()) {
        yield t.getKind() == DECLARED && t.equals(((DeclaredType)t).asElement().asType());
      }
    }
    };
  }

  /**
   * A convenience method that returns {@code true} if and only if the supplied {@link TypeMirror} is a <dfn>raw
   * type</dfn> according to <a href="https://docs.oracle.com/javase/specs/jls/se23/html/jls-4.html#jls-4.8">the rules
   * of the Java Language Specification</a>
   *
   * @param t a {@link TypeMirror}; must not be {@code null}
   *
   * @return {@code true} if and only if the supplied {@link TypeMirror} is a <dfn>raw type</dfn> according to <a
   * href="https://docs.oracle.com/javase/specs/jls/se23/html/jls-4.html#jls-4.8">the rules of the Java Language
   * Specification</a>
   *
   * @exception NullPointerException if {@code t} is {@code null}
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-4.html#jls-4.8 Java Language Specification, section 4.8
   */
  // (Convenience.)
  public default boolean raw(final TypeMirror t) {
    return switch (t) {
    case null -> throw new NullPointerException("t");
    case UniversalType ut -> ut.raw();
    default -> {
      try (var lock = this.lock()) {
        yield switch (t.getKind()) {
        case ARRAY -> raw(elementType((ArrayType)t));
        case DECLARED -> {
          final DeclaredType dt = (DeclaredType)t;
          yield generic(dt.asElement()) && dt.getTypeArguments().isEmpty();
        }
        default -> false;
        };
      }
    }
    };
  }

  /**
   * A convenience method that returns the <dfn>raw type</dfn> corresponding to {@code t}, <strong>or {@code null} if
   * {@code t} is <a href="https://docs.oracle.com/javase/specs/jls/se23/html/jls-4.html#jls-4.8">incapable of yielding
   * a raw type</a></strong>.
   *
   * <p>Overrides of this method must conform to the requirements imposed by the relevant section of the relevant
   * version of the Java Language Specification concerning raw types.</p>
   *
   * @param t a {@link TypeMirror}; must not be {@code null}
   *
   * @return the raw type corresponding to the supplied {@link TypeMirror}, or {@code null} if {@code t} is <a
   * href="https://docs.oracle.com/javase/specs/jls/se23/html/jls-4.html#jls-4.8">incapable of yielding a raw type</a>
   *
   * @exception NullPointerException if {@code t} is {@code null}
   *
   * @see #parameterized(TypeMirror)
   *
   * @see #elementType(TypeMirror)
   *
   * @see #erasure(TypeMirror)
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-4.html#jls-4.8 Java Language Specification, section
   * 4.8
   */
  // (Convenience.)
  public default TypeMirror rawType(TypeMirror t) {
    return switch (t) {
    case null -> throw new NullPointerException("t");
    case UniversalType ut -> ut.elementType().parameterized() ? this.erasure(ut) : null;
    default -> this.parameterized(this.elementType(t)) ? this.erasure(t) : null;
    };
  }

  /**
   * Returns a {@link RecordComponentElement} corresponding to the supplied {@link ExecutableElement}, <strong>or {@code
   * null} if there is no such {@link RecordComponentElement}</strong>.
   *
   * @param e an {@link ExecutableElement} {@linkplain ExecutableElement#getEnclosingElement() enclosed by} a record
   * representing an <dfn>accessor method</dfn>; must not be {@code null}
   *
   * @return a {@link RecordComponentElement} corresponding to the supplied {@link ExecutableElement}, or {@code null}
   *
   * @exception NullPointerException if {@code e} is {@code null}
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-8.html#jls-8.10.3 Java Language Specification, section
   * 8.10.3
   */
  public RecordComponentElement recordComponentElement(final ExecutableElement e);

  /**
   * Returns {@code true} if and only if the two arguments represent the <dfn>same type</dfn>.
   *
   * <p>This method differs from the {@link javax.lang.model.util.Types#isSameType(TypeMirror, TypeMirror)} method in
   * two ways:</p>
   *
   * <ul>
   *
   * <li>Its arguments may be {@code null}. If both arguments are {@code null}, {@code true} is returned. If only one
   * argument is {@code null}, {@code false} is returned.</li>
   *
   * <li>If the same Java object reference is passed as both arguments, {@code true} is returned (even if it {@linkplain
   * TypeMirror#getKind() has a <code>TypeKind</code>} of {@link TypeKind#WILDCARD}).</li>
   *
   * </ul>
   *
   * @param t0 the first {@link TypeMirror}; may be {@code null}
   *
   * @param t1 the second {@link TypeMirror}; may be {@code null}
   *
   * @return {@code true} if and only if the two arguments represent the <dfn>same type</dfn>; {@code false} otherwise
   *
   * @see javax.lang.model.util.Types#isSameType(TypeMirror, TypeMirror)
   *
   * @see <a href="https://bugs.openjdk.org/browse/JDK-8055219">JDK-8055219</a>
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-4.html#jls-4.3.4 Java Language Specification, section
   * 4.3.4
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-4.html#jls-4.5.1 Java Language Specification, section
   * 4.5.1
   */
  public boolean sameType(final TypeMirror t0, final TypeMirror t1);

  /**
   * Returns {@code true} if and only if {@code et0} is a <dfn>subsignature</dfn> of {@code et1}.
   *
   * @param et0 the first {@link ExecutableType}; must not be {@code null}
   *
   * @param et1 the second {@link ExecutableType}; must not be {@code null}
   *
   * @return {@code true} if and only if {@code et0} is a <dfn>subsignature</dfn> of {@code et1}
   *
   * @exception NullPointerException if either argument is {@code null}
   *
   * @exception ClassCastException if this method is implemented in terms of the {@link
   * javax.lang.model.util.Types#isSubsignature(ExecutableType, ExecutableType)} method, and if each of the supplied
   * {@link ExecutableType} arguments does not have an {@linkplain TypeKind#EXECUTABLE <code>EXECUTABLE</code>
   * <code>TypeKind</code>}; this exception type is undocumented by the {@link
   * javax.lang.model.util.Types#isSubsignature(ExecutableType, ExecutableType)} method and so is subject to change
   * without prior notice
   *
   * @see javax.lang.model.util.Types#isSubsignature(ExecutableType, ExecutableType)
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-8.html#jls-8.4.2 Java Language Specification, section
   * 8.4.2
   */
  public boolean subsignature(final ExecutableType et0, final ExecutableType et1);

  /**
   * Returns {@code true} if and only if {@code candidateSubtype} is a <dfn>subtype</dfn> of {@code supertype}.
   *
   * @param candidateSubtype the first {@link TypeMirror}; must not be {@code null}
   *
   * @param supertype the second {@link TypeMirror}; must not be {@code null}
   *
   * @return {@code true} if and only if {@code candidateSubtype} is a <dfn>subtype</dfn> of {@code supertype}
   *
   * @exception NullPointerException if either argument is {@code null}
   *
   * @see javax.lang.model.util.Types#isSubtype(TypeMirror, TypeMirror)
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html#jls-4.10 Java Language Specification, section
   * 4.10
   */
  public boolean subtype(TypeMirror candidateSubtype, TypeMirror supertype);

  /**
   * A convenience method that returns the {@link TypeMirror} corresponding to the supplied (reflective) {@link Type}.
   *
   * @param t a {@link Type}; must not be {@code null}
   *
   * @return the {@link TypeMirror} corresponding to the supplied {@link Type}; never {@code null}
   *
   * @exception NullPointerException if {@code t} is {@code null}
   *
   * @exception IllegalArgumentException if {@code t} is not a {@link Class}, {@link GenericArrayType}, {@link
   * ParameterizedType}, {@link java.lang.reflect.TypeVariable} or {@link java.lang.reflect.WildcardType}
   */
  // (Convenience.)
  public default TypeMirror type(final Type t) {
    // TODO: anywhere there is domain.declaredType(), consider passing
    // domain.moduleElement(this.getClass().getModule().getName()) as the first argument. Not sure how this works
    // exactly but I think it might be necessary.
    return switch (t) {
    case null -> throw new NullPointerException("t");
    case Class<?> c when t == boolean.class -> this.primitiveType(TypeKind.BOOLEAN);
    case Class<?> c when t == byte.class -> this.primitiveType(TypeKind.BYTE);
    case Class<?> c when t == char.class -> this.primitiveType(TypeKind.CHAR);
    case Class<?> c when t == double.class -> this.primitiveType(TypeKind.DOUBLE);
    case Class<?> c when t == float.class -> this.primitiveType(TypeKind.FLOAT);
    case Class<?> c when t == int.class -> this.primitiveType(TypeKind.INT);
    case Class<?> c when t == long.class -> this.primitiveType(TypeKind.LONG);
    case Class<?> c when t == short.class -> this.primitiveType(TypeKind.SHORT);
    case Class<?> c when t == void.class -> this.noType(TypeKind.VOID);
    case Class<?> c when t == Object.class -> this.javaLangObject().asType(); // cheap and easy optimization
    case Class<?> c when c.isArray() -> this.arrayTypeOf(this.type(c.getComponentType()));
    case Class<?> c -> this.declaredType(c.getCanonicalName());
    case GenericArrayType g -> this.arrayTypeOf(this.type(g.getGenericComponentType()));
    case ParameterizedType pt when pt.getOwnerType() == null ->
      this.declaredType(this.typeElement(((Class<?>)pt.getRawType()).getCanonicalName()),
                        this.types(pt.getActualTypeArguments()));
    case ParameterizedType pt ->
      this.declaredType((DeclaredType)this.type(pt.getOwnerType()),
                        this.typeElement(((Class<?>)pt.getRawType()).getCanonicalName()),
                        this.types(pt.getActualTypeArguments()));
    case java.lang.reflect.TypeVariable<?> tv -> this.typeVariable(this.parameterizable(tv.getGenericDeclaration()), tv.getName());
    case java.lang.reflect.WildcardType w when w.getLowerBounds().length <= 0 -> this.wildcardType(this.type(w.getUpperBounds()[0]), null);
    case java.lang.reflect.WildcardType w -> this.wildcardType(null, this.type(w.getLowerBounds()[0]));
    default -> throw new IllegalArgumentException("t: " + t);
    };
  }

  /**
   * A convenience method that returns an array of {@link TypeMirror}s whose elements correspond, in order, to the
   * elements in the supplied {@link Type} array.
   *
   * @param ts an array of {@link Type}s; must not be {@code null}
   *
   * @return an array of {@link TypeMirror}s whose elements correspond, in order, to the elements in the supplied {@link
   * Type} array; never {@code null}
   *
   * @exception NullPointerException if {@code ts} is {@code null} or contains {@code null} elements
   *
   * @exception IllegalArgumentException if any element of {@code ts} is deemed illegal by the {@link #type(Type)}
   * method
   *
   * @see #type(Type)
   */
  // (Convenience.)
  public default TypeMirror[] types(final Type[] ts) {
    return switch (ts.length) {
    case 0 -> new TypeMirror[0];
    case 1 -> new TypeMirror[] { this.type(ts[0]) };
    default -> {
      final TypeMirror[] rv = new TypeMirror[ts.length];
      for (int i = 0; i < ts.length; i++) {
        rv[i] = this.type(ts[i]);
      }
      yield rv;
    }
    };
  }

  /**
   * Returns a {@link TypeElement} representing the element bearing the supplied <dfn>canonical name</dfn>, <strong>or
   * {@code null} if there is no such {@link TypeElement}</strong>.
   *
   * @param canonicalName a valid canonical name; must not be {@code null}
   *
   * @return a {@link TypeElement}, or {@code null}
   *
   * @exception NullPointerException if {@code canonicalName} is {@code null}
   *
   * @see javax.lang.model.util.Elements#getTypeElement(CharSequence)
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-6.html#jls-6.7 Java Language Specification, section
   * 6.7
   */
  // Element factory method.
  public TypeElement typeElement(final CharSequence canonicalName);

  /**
   * Returns a {@link TypeElement} representing the element bearing the supplied <dfn>canonical name</dfn>, as read or
   * seen from the module represented by the supplied {@link ModuleElement}, <strong>or {@code null} if there is no such
   * {@link TypeElement}</strong>.
   *
   * @param asSeenFrom a {@link ModuleElement}; must not be {@code null}
   *
   * @param canonicalName a valid canonical name; must not be {@code null}
   *
   * @return a {@link TypeElement}, or {@code null}
   *
   * @exception NullPointerException if either {@code asSeenFrom} or {@code canonicalName} is {@code null}
   *
   * @see javax.lang.model.util.Elements#getTypeElement(ModuleElement, CharSequence)
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-6.html#jls-6.7 Java Language Specification, section
   * 6.7
   */
  // Element factory method.
  public TypeElement typeElement(final ModuleElement asSeenFrom, final CharSequence canonicalName);

  /**
   * Returns a {@link TypeElement} representing the result of applying <dfn>boxing conversion</dfn> to the primitive
   * type represented by the supplied {@link PrimitiveType} argument.
   *
   * <p>The default implementation of this method calls the {@link #typeElement(TypeKind)} method with the supplied
   * {@link PrimitiveType}'s {@linkplain TypeMirror#getKind() affiliated <code>TypeKind</code>} and returns its
   * result.</p>
   *
   * @param t a {@link PrimitiveType} with a {@link TypeKind} that {@linkplain TypeKind#isPrimitive() is primitive};
   * must not be {@code null}
   *
   * @return a non-{@code null} {@link TypeElement} representing the result of applying boxing conversion to the
   * supplied argument
   *
   * @exception NullPointerException if {@code t} is {@code null}
   *
   * @exception ClassCastException if this method is implemented in terms of the {@link
   * javax.lang.model.util.Types#boxedClass(PrimitiveType)} method, and if the supplied {@link PrimitiveType} does not
   * have a {@linkplain TypeKind#isPrimitive() primitive <code>TypeKind</code>}; this exception type is undocumented by
   * the {@link javax.lang.model.util.Types#boxedClass(PrimitiveType)} method and so is subject to change without prior
   * notice
   *
   * @exception IllegalArgumentException if {@code primitiveTypeKind} {@linkplain TypeKind#isPrimitive() is not
   * primitive}
   *
   * @see #typeElement(TypeKind)
   *
   * @see javax.lang.model.util.Types#boxedClass(PrimitiveType)
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-5.html#jls-5.1.7 Java Language Specification, section
   * 5.1.7
   */
  // Element factory method.
  // (Canonical.)
  // (Boxing.)
  public default TypeElement typeElement(final PrimitiveType t) {
    return switch (t) {
    case null -> throw new NullPointerException("t");
    case UniversalType ut -> this.typeElement(ut.getKind());
    default -> {
      try (var lock = this.lock()) {
        yield this.typeElement(t.getKind());
      }
    }
    };
  }

  /**
   * Returns a {@link TypeElement} representing the result of applying <dfn>boxing conversion</dfn> to the primitive
   * type represented by the supplied {@link TypeKind} argument, if it {@linkplain TypeKind#isPrimitive() is primitive}.
   *
   * @param primitiveTypeKind a {@link TypeKind} that {@linkplain TypeKind#isPrimitive() is primitive}; must not be
   * {@code null}
   *
   * @return a non-{@code null} {@link TypeElement} representing the result of applying boxing conversion to the
   * supplied argument
   *
   * @exception IllegalArgumentException if {@code primitiveTypeKind} {@linkplain TypeKind#isPrimitive() is not
   * primitive}
   *
   * @see #typeElement(PrimitiveType)
   *
   * @see javax.lang.model.util.Types#boxedClass(PrimitiveType)
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-5.html#jls-5.1.7 Java Language Specification, section
   * 5.1.7
   */
  // (Convenience.)
  // (Boxing.)
  public default TypeElement typeElement(final TypeKind primitiveTypeKind) {
    return switch (primitiveTypeKind) {
    case BOOLEAN -> this.typeElement("java.lang.Boolean");
    case BYTE -> this.typeElement("java.lang.Byte");
    case CHAR -> this.typeElement("java.lang.Character");
    case DOUBLE -> this.typeElement("java.lang.Double");
    case FLOAT -> this.typeElement("java.lang.Float");
    case INT -> this.typeElement("java.lang.Integer");
    case LONG -> this.typeElement("java.lang.Long");
    case SHORT -> this.typeElement("java.lang.Short");
    default -> throw new IllegalArgumentException("primitiveTypeKind: " + primitiveTypeKind);
    };
  }

  /**
   * Returns the {@link TypeParameterElement} {@linkplain Parameterizable#getTypeParameters() contained} by the supplied
   * {@link Parameterizable} whose {@linkplain TypeParameterElement#getSimpleName() name} {@linkplain
   * Name#contentEquals(CharSequence) is equal to} the supplied {@code name}, <strong>or {@code null} if there is no
   * such {@link TypeParameterElement}</strong>.
   *
   * @param p a {@link Parameterizable}; must not be {@code null}
   *
   * @param name a name valid for a type parameter; must not be {@code null}
   *
   * @return a {@link TypeParameterElement}, or {@code null}
   */
  // (Convenience.)
  public default TypeParameterElement typeParameterElement(Parameterizable p, final CharSequence name) {
    Objects.requireNonNull(p, "p");
    Objects.requireNonNull(name, "name");
    while (p != null) {
      switch (p) {
      case UniversalElement ue:
        for (final UniversalElement tpe : ue.getTypeParameters()) {
          if (tpe.getSimpleName().contentEquals(name)) {
            return tpe;
          }
        }
        p = ue.getEnclosingElement();
        break;
      default:
        try (var lock = this.lock()) {
          for (final TypeParameterElement tpe : p.getTypeParameters()) {
            if (tpe.getSimpleName().contentEquals(name)) {
              return tpe;
            }
          }
          p = (Parameterizable)((Element)p).getEnclosingElement();
        }
        break;
      }
    }
    return null;
  }

  /**
   * A convenience method that returns the {@link TypeVariable} {@linkplain TypeParameterElement#asType() declared by}
   * the {@link TypeParameterElement} {@linkplain Parameterizable#getTypeParameters() contained} by the supplied {@link
   * Parameterizable} whose {@linkplain TypeParameterElement#getSimpleName() name} {@linkplain
   * Name#contentEquals(CharSequence) is equal to} the supplied {@code name}, <strong>or {@code null} if there is no
   * such {@link TypeParameterElement} or {@link TypeVariable}</strong>.
   *
   * @param p a {@link Parameterizable}; must not be {@code null}
   *
   * @param name a name valid for a type parameter; must not be {@code null}
   *
   * @return a {@link TypeVariable}, or {@code null}
   *
   * @see #typeParameterElement(Parameterizable, CharSequence)
   */
  // (Convenience.)
  public default TypeVariable typeVariable(Parameterizable p, final CharSequence name) {
    final TypeParameterElement e = this.typeParameterElement(p, name);
    return e == null ? null : (TypeVariable)e.asType();
  }

  /**
   * A convenience method that returns the first {@link VariableElement} with a {@linkplain ElementKind#isVariable()
   * variable <code>ElementKind</code>} and {@linkplain Element#getSimpleName() bearing} the supplied {@code simpleName}
   * that the supplied {@code enclosingElement} {@linkplain Element#getEnclosedElements() encloses}, <strong>or {@code
   * null} if there is no such {@link VariableElement}</strong>.
   *
   * @param enclosingElement an {@link Element}; must not be {@code null}
   *
   * @param simpleName a {@link CharSequence}; must not be {@code null}
   *
   * @return a {@link VariableElement}, or {@code null}
   *
   * @exception NullPointerException if either argument is {@code null}
   *
   * @see Element#getEnclosedElements()
   *
   * @see ElementKind#isVariable()
   *
   * @see Element#getSimpleName()
   *
   * @see VariableElement
   */
  // (Convenience.)
  public default VariableElement variableElement(final Element enclosingElement, final CharSequence simpleName) {
    Objects.requireNonNull(simpleName, "simpleName");
    return switch (enclosingElement) {
    case null -> throw new NullPointerException("enclosingElement");
    case UniversalElement ue -> {
      for (final UniversalElement ee : ue.getEnclosedElements()) {
        if (ee.getKind().isVariable() && ee.getSimpleName().contentEquals(simpleName)) {
          yield ee;
        }
      }
      yield null;
    }
    default -> {
      try (var lock = lock()) {
        for (final Element ee : enclosingElement.getEnclosedElements()) {
          if (ee.getKind().isVariable() && ee.getSimpleName().contentEquals(simpleName)) {
            yield (VariableElement)ee;
          }
        }
      }
      yield null;
    }
    };
  }

  /**
   * A convenience method that returns a new {@link WildcardType} {@linkplain TypeMirror#getKind() with a
   * <code>TypeKind</code>} of {@link TypeKind#WILDCARD}, an {@linkplain WildcardType#getExtendsBound() extends bound}
   * of {@code null}, and a {@linkplain WildcardType#getSuperBound() super bound} of {@code null}.
   *
   * @return a new, non-{@code null} {@link WildcardType}
   *
   * @see #wildcardType(TypeMirror, TypeMirror)
   *
   * @see javax.lang.model.util.Types#getWildcardType(TypeMirror, TypeMirror)
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-4.html#jls-4.5.1 Java Language Specification, section
   * 4.5.1
   */
  // (Convenience.)
  public default WildcardType wildcardType() {
    return this.wildcardType(null, null);
  }

  /**
   * Returns a new {@link WildcardType} {@linkplain TypeMirror#getKind() with a <code>TypeKind</code>} of {@link
   * TypeKind#WILDCARD}, an {@linkplain WildcardType#getExtendsBound() extends bound} of the supplied {@code
   * extendsBound}, and a {@linkplain WildcardType#getSuperBound() super bound} of the supplied {@code superBound}.
   *
   * <p>Any argument may be {@code null}. Both arguments may not be non-{@code null}.</p>
   *
   * @param extendsBound the upper bound of the new {@link WildcardType}; may be {@code null}
   *
   * @param superBound the lower bound of the new {@link WildcardType}; may be {@code null}
   *
   * @return a new, non-{@code null} {@link WildcardType}
   *
   * @exception IllegalArgumentException if both arguments are non-{@code null} or otherwise unsuitable for being the
   * bounds of a {@link WildcardType}
   *
   * @see javax.lang.model.util.Types#getWildcardType(TypeMirror, TypeMirror)
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-4.html#jls-4.5.1 Java Language Specification, section
   * 4.5.1
   */
  // Type factory method.
  public WildcardType wildcardType(TypeMirror extendsBound, TypeMirror superBound);

}
