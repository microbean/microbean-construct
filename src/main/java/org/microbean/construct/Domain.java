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

import java.util.List;
import java.util.Objects;

import javax.lang.model.element.Element;
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

import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.ElementKind.METHOD;

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
 */
@SuppressWarnings("try")
public interface Domain {

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
  public ArrayType arrayTypeOf(final TypeMirror componentType);

  /**
   * Returns the {@link Element} declaring the supplied {@link TypeMirror}, or {@code null} if there is no such {@link
   * Element}.
   *
   * @param t a {@link TypeMirror}; must not be {@code null}
   *
   * @return an {@link Element}, or {@code null}
   *
   * @see javax.lang.model.util.Types#asElement(TypeMirror)
   */
  public Element asElement(final TypeMirror t);

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
   * Specification</a>.
   *
   * @param payload the {@link TypeMirror} being assigned; must not be {@code null}
   *
   * @param receiver the {@link TypeMirror} receiving the assignment; must not be {@code null}
   *
   * @return {@code true} if and only if {@code payload} is assignable to {@code receiver}
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
   * @see javax.lang.model.util.Elements#getBinaryName(TypeElement)
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se21/html/jls-13.html#jls-13.1 Java Language Specification, section
   * 13.1
   */
  public Name binaryName(final TypeElement e);

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
   * TypeElement} that bears the supplied {@code canonicalName}, or {@code null} if there is no such {@link TypeElement}
   * (and therefore no such {@link DeclaredType}).
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
   * supplied {@link TypeElement} {@link #generic(Element) is generic}, then the supplied {@link TypeElement}'s raw type
   * is returned.</p>
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
  public DeclaredType declaredType(final DeclaredType enclosingType,
                                   final TypeElement typeElement,
                                   final TypeMirror... typeArguments);

  /**
   * Returns a non-{@code null} {@link List} of the <dfn>direct supertypes</dfn> of the supplied {@link TypeMirror},
   * which is normally a {@linkplain TypeKind#DECLARED declared type}.
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
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-4.html#jls-4.10 Java Language Specification, section
   * 4.10
   */
  public List<? extends TypeMirror> directSupertypes(final TypeMirror t);

  /**
   * Returns the {@link Element} responsible for declaring the supplied {@link TypeMirror}, which is most commonly a
   * {@link DeclaredType}, a {@link TypeVariable}, a {@link NoType} with a {@link TypeKind} of {@link TypeKind#MODULE},
   * or a {@link NoType} with a {@link TypeKind} of {@link TypeKind#PACKAGE}, or {@code null} if there is no such {@link
   * Element}.
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
   * A convenience method that returns an {@link ExecutableElement} representing the static initializer, constructor or
   * method described by the supplied arguments, or {@code null} if no such {@link ExecutableElement} exists.
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
   * @return an {@link ExecutableElement} with an {@link javax.lang.model.element.ElementKind} of {@link
   * javax.lang.model.element.ElementKind#CONSTRUCTOR}, {@link javax.lang.model.element.ElementKind#METHOD}, or {@link
   * javax.lang.model.element.ElementKind#STATIC_INIT}, or {@code null}
   *
   * @exception NullPointerException if any argument is {@code null}
   */
  public default ExecutableElement executableElement(final TypeElement declaringElement,
                                                     final TypeMirror returnType,
                                                     final CharSequence name,
                                                     final TypeMirror... parameterTypes) {
    try (var lock = this.lock()) {
      final List<? extends Element> ees = declaringElement.getEnclosedElements();
      return ees.stream()
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
  public default boolean generic(final Element e) {
    if (Objects.requireNonNull(e, "e") instanceof Parameterizable p) {
      try (var lock = this.lock()) {
        return switch (e.getKind()) {
        case CLASS, CONSTRUCTOR, ENUM, INTERFACE, METHOD, RECORD -> !p.getTypeParameters().isEmpty();
        default -> false;
        };
      }
    }
    return false;
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
  public default TypeElement javaLangObject() {
    return this.typeElement("java.lang.Object");
  }

  /**
   * Semantically locks an opaque lock used to serialize symbol completion, and returns it in the form of an {@link
   * Unlockable}.
   *
   * <p>Implementations of this method must not return {@code null}.</p>
   *
   * @return an {@link Unlockable} in a semantically locked state; never {@code null}
   *
   * @see Unlockable#close()
   */
  public Unlockable lock();

  /**
   * Returns a {@link ModuleElement} representing the module {@linkplain ModuleElement#getQualifiedName() named} by the
   * supplied {@code qualifiedName}, or {@code null} if there is no such {@link ModuleElement}.
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
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-8.html#jls-8.4.5 Java Language Specification, section
   * 8.4.5
   */
  public NoType noType(final TypeKind kind);

  /**
   * Returns a {@link NullType} implementation {@linkplain TypeMirror#getKind() whose <code>TypeKind</code>} is {@link
   * TypeKind#NULL}.
   *
   * @return a non-{@code null} {@link NullType}
   *
   * @see javax.lang.model.util.Types#getNullType()
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-4.html#jls-4.1 Java Language Specification, section 4.1
   */
  public NullType nullType();

  /**
   * Returns a {@link PackageElement} representing the package bearing the supplied {@code canonicalName}, or {@code null}
   * if there is no such {@link PackageElement}.
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
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-6.html#jls-6.7 Java Language Specification, section
   * 6.7
   */
  public PackageElement packageElement(final CharSequence canonicalName);

  /**
   * Returns a {@link PackageElement} representing the package bearing the supplied {@code canonicalName} as seen from
   * the module represented by the supplied {@link ModuleElement}, or {@code null} if there is no such {@link
   * PackageElement}.
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
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-6.html#jls-6.7 Java Language Specification, section
   * 6.7
   */
  public PackageElement packageElement(final ModuleElement asSeenFrom, final CharSequence canonicalName);

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
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-4.html#jls-4.2 Java Language Specification, section
   * 4.2
   */
  // (Canonical.)
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
  public PrimitiveType primitiveType(final TypeMirror t);

  /**
   * Returns a {@link RecordComponentElement} corresponding to the supplied {@link ExecutableElement}, or {@code null}
   * if there is no such {@link RecordComponentElement}.
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
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-4.html#jls-4.10 Java Language Specification, section
   * 4.10
   */
  public boolean subtype(TypeMirror candidateSubtype, TypeMirror supertype);

  /**
   * Converts the supplied {@link CharSequence}, which is often a {@link Name}, into a {@link String}, and returns the
   * conversion, {@linkplain #lock() locking} when appropriate to serialize symbol completion.
   *
   * @param name the {@link CharSequence} to convert; may be {@code null} in which case {@code null} will be returned
   *
   * @return a {@link String}, or {@code null} if {@code name} was {@code null}
   *
   * @see #lock()
   */
  public default String toString(final CharSequence name) {
    return switch (name) {
    case null -> null;
    case String s -> s;
    case Name n -> {
      try (var lock = this.lock()) {
        yield n.toString();
      }
    }
    default -> name.toString();
    };
  }

  /**
   * Returns a {@link TypeElement} representing the element bearing the supplied <dfn>canonical name</dfn>, or {@code
   * null} if there is no such {@link TypeElement}.
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
  public TypeElement typeElement(final CharSequence canonicalName);

  /**
   * Returns a {@link TypeElement} representing the element bearing the supplied <dfn>canonical name</dfn>, as read or
   * seen from the module represented by the supplied {@link ModuleElement}, or {@code null} if there is no such {@link
   * TypeElement}.
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
  // (Canonical.)
  // (Boxing.)
  public default TypeElement typeElement(final PrimitiveType t) {
    try (var lock = this.lock()) {
      return this.typeElement(t.getKind());
    }
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
   * Name#contentEquals(CharSequence) is equal to} the supplied {@code name}, or {@code null} if there is no such {@link
   * TypeParameterElement}.
   *
   * @param p a {@link Parameterizable}; must not be {@code null}
   *
   * @param name a name valid for a type parameter; must not be {@code null}
   *
   * @return a {@link TypeParameterElement}, or {@code null}
   */
  public default TypeParameterElement typeParameterElement(Parameterizable p, final CharSequence name) {
    Objects.requireNonNull(p, "p");
    Objects.requireNonNull(name, "name");
    while (p != null) {
      // A call to getTypeParameters() does not cause symbol completion, but name acquisition also needs to be
      // serialized globally.
      try (var lock = this.lock()) {
        for (final TypeParameterElement tpe : p.getTypeParameters()) {
          if (tpe.getSimpleName().contentEquals(name)) {
            return tpe;
          }
        }
      }
      p = switch (p) {
      case ExecutableElement ee -> (Parameterizable)ee.getEnclosingElement();
      case TypeElement te -> (Parameterizable)te.getEnclosingElement();
      default -> null;
      };
    }
    return null;
  }

  /**
   * A convenience method that returns the {@link TypeVariable} {@linkplain TypeParameterElement#asType() declared by}
   * the {@link TypeParameterElement} {@linkplain Parameterizable#getTypeParameters() contained} by the supplied {@link
   * Parameterizable} whose {@linkplain TypeParameterElement#getSimpleName() name} {@linkplain
   * Name#contentEquals(CharSequence) is equal to} the supplied {@code name}, or {@code null} if there is no such {@link
   * TypeParameterElement} or {@link TypeVariable}.
   *
   * @param p a {@link Parameterizable}; must not be {@code null}
   *
   * @param name a name valid for a type parameter; must not be {@code null}
   *
   * @return a {@link TypeVariable}, or {@code null}
   *
   * @see #typeParameterElement(Parameterizable, CharSequence)
   */
  public default TypeVariable typeVariable(Parameterizable p, final CharSequence name) {
    final TypeParameterElement e = this.typeParameterElement(p, name);
    return e == null ? null : (TypeVariable)e.asType();
  }

  /**
   * A convenience method that returns the first {@link VariableElement} with a {@linkplain
   * javax.lang.model.element.ElementKind#isVariable() variable <code>ElementKind</code>} and {@linkplain
   * Element#getSimpleName() bearing} the supplied {@code simpleName} that the supplied {@code enclosingElement}
   * {@linkplain Element#getEnclosedElements() encloses}, or {@code null} if there is no such {@link VariableElement}.
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
   * @see javax.lang.model.element.ElementKind#isVariable()
   *
   * @see Element#getSimpleName()
   *
   * @see VariableElement
   */
  public default VariableElement variableElement(final Element enclosingElement, final CharSequence simpleName) {
    Objects.requireNonNull(simpleName, "simpleName");
    try (var lock = lock()) {
      for (final Element ee : enclosingElement.getEnclosedElements()) {
        if (ee.getKind().isVariable() && ee.getSimpleName().contentEquals(simpleName)) {
          return (VariableElement)ee;
        }
      }
    }
    return null;
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
  public WildcardType wildcardType(TypeMirror extendsBound, TypeMirror superBound);

}
