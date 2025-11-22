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
package org.microbean.construct.type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;

import org.microbean.construct.UniversalConstruct;
import org.microbean.construct.Domain;

import org.microbean.construct.element.UniversalElement;

import static javax.lang.model.type.TypeKind.DECLARED;
import static javax.lang.model.type.TypeKind.NONE;
import static javax.lang.model.type.TypeKind.VOID;

/**
 * A {@link TypeMirror} and {@link UniversalConstruct} implementation.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see TypeMirror#getKind()
 *
 * @see UniversalConstruct
 */
public final class UniversalType
  extends UniversalConstruct<TypeMirror>
  implements ArrayType,
             ErrorType,
             ExecutableType,
             IntersectionType,
             NoType,
             NullType,
             PrimitiveType,
             TypeVariable,
             UnionType,
             WildcardType {

  // Eventually this should become a lazy constant/stable value
  private volatile UniversalType erasure;

  /**
   * Creates a new {@link UniversalType}.
   *
   * @param delegate a {@link TypeMirror} to which operations will be delegated; must not be {@code null}
   *
   * @param domain a {@link Domain} from which the supplied {@code delegate} is presumed to have originated; must not be
   * {@code null}
   *
   * @exception NullPointerException if either argument is {@code null}
   *
   * @see #delegate()
   */
  @SuppressWarnings("try")
  public UniversalType(final TypeMirror delegate, final Domain domain) {
    super(delegate, domain);
  }

  @Override // TypeMirror
  public final <R, P> R accept(final TypeVisitor<R, P> v, final P p) {
    return switch (this.getKind()) {
    case ARRAY        -> v.visitArray(this, p);
    case DECLARED     -> v.visitDeclared(this, p);
    case ERROR        -> v.visitError(this, p);
    case EXECUTABLE   -> v.visitExecutable(this, p);
    case INTERSECTION -> v.visitIntersection(this, p);
    case
      MODULE,
      NONE,
      PACKAGE,
      VOID            -> v.visitNoType(this, p);
    case NULL         -> v.visitNull(this, p);
    case
      BOOLEAN,
      BYTE,
      CHAR,
      DOUBLE,
      FLOAT,
      INT,
      LONG,
      SHORT           -> v.visitPrimitive(this, p);
    case TYPEVAR      -> v.visitTypeVariable(this, p);
    case UNION        -> v.visitUnion(this, p);
    case WILDCARD     -> v.visitWildcard(this, p);
    case OTHER        -> v.visitUnknown(this, p);
    };
  }

  @Override // Various
  public final UniversalElement asElement() {
    return switch (this.getKind()) {
    case DECLARED -> UniversalElement.of(((DeclaredType)this.delegate()).asElement(), this.domain());
    case TYPEVAR  -> UniversalElement.of(((TypeVariable)this.delegate()).asElement(), this.domain());
    default -> null;
    };
  }

  /**
   * Returns the <dfn>element type</dfn> of this {@link UniversalType} if it is an array type, or simply this {@link
   * UniversalType} if it is not.
   *
   * @return the <dfn>element type</dfn> of this {@link UniversalType} if it is an array type, or this {@link
   * UniversalType} if it is not; never {@code null}
   */
  public final UniversalType elementType() {
    return this.elementType(this);
  }

  private final UniversalType elementType(final UniversalType t) {
    return switch (t.getKind()) {
    case ARRAY -> this.elementType(t.getComponentType());
    default -> t;
    };
  }

  /**
   * Returns the <dfn>erasure</dfn> of this {@link UniversalType}.
   *
   * @return the erasure of this {@link UniversalType}; never {@code null}
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-4.html#jls-4.6 Java Language Specification, section
   * 4.6
   */
  public final UniversalType erasure() {
    UniversalType t = this.erasure; // volatile read
    if (t == null) {
      t = this.erasure = switch (this.getKind()) { // volatile write, read
      case ARRAY, DECLARED, TYPEVAR -> this.wrap(this.domain().erasure(this));
      default -> this;
      };
    }
    return t;
  }

  /**
   * Returns {@code true} if and only if this {@link UniversalType} is declared by a {@linkplain
   * UniversalElement#generic() generic declaration}.
   *
   * @return {@code true} if and only if this {@link UniversalType} is declared by a {@linkplain
   * UniversalElement#generic() generic declaration}
   *
   * @see UniversalElement#generic()
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-8.html#jls-8.1.2 Java Language Specification, section
   * 8.1.2
   */
  public final boolean generic() {
    final UniversalElement e = this.asElement();
    return e != null && e.generic();
  }

  @Override // UnionType
  public final List<? extends UniversalType> getAlternatives() {
    return switch (this.getKind()) {
    case UNION -> this.wrap(((UnionType)this.delegate()).getAlternatives());
    default -> List.of();
    };
  }

  @Override // IntersectionType
  public final List<? extends UniversalType> getBounds() {
    return switch (this.getKind()) {
    case INTERSECTION -> this.wrap(((IntersectionType)this.delegate()).getBounds());
    default -> List.of();
    };
  }

  @Override // ArrayType
  public final UniversalType getComponentType() {
    return switch (this.getKind()) {
    case ARRAY -> this.wrap(((ArrayType)this.delegate()).getComponentType());
    default -> this.wrap(this.domain().noType(NONE));
    };
  }

  @Override // DeclaredType
  public final UniversalType getEnclosingType() {
    return switch(this.getKind()) {
    case DECLARED -> this.wrap(((DeclaredType)this.delegate()).getEnclosingType());
    default -> this.wrap(this.domain().noType(NONE));
    };
  }

  @Override // WildcardType
  public final UniversalType getExtendsBound() {
    return switch (this.getKind()) {
    case WILDCARD -> this.wrap(((WildcardType)this.delegate()).getExtendsBound());
    default -> null;
    };
  }

  @Override // TypeMirror
  public final TypeKind getKind() {
    return this.delegate().getKind();
  }

  @Override // TypeVariable
  public final UniversalType getLowerBound() {
    return switch (this.getKind()) {
    case TYPEVAR -> this.wrap(((TypeVariable)this.delegate()).getLowerBound());
    default -> this.wrap(this.domain().nullType()); // bottom (null) type, not NONE type
    };
  }

  @Override // TypeVariable
  public final UniversalType getUpperBound() {
    return switch (this.getKind()) {
    case TYPEVAR -> this.wrap(((TypeVariable)this.delegate()).getUpperBound());
    default -> this.wrap(this.domain().javaLangObject().asType());
    };
  }

  @Override // ExecutableType
  public final List<? extends UniversalType> getParameterTypes() {
    return switch (this.getKind()) {
    case EXECUTABLE -> this.wrap(((ExecutableType)this.delegate()).getParameterTypes());
    default -> List.of();
    };
  }

  @Override // ExecutableType
  public final UniversalType getReceiverType() {
    return switch (this.getKind()) {
    case EXECUTABLE -> this.wrap(((ExecutableType)this.delegate()).getReceiverType());
    default -> this.wrap(this.domain().noType(NONE));
    };
  }

  @Override // ExecutableType
  public final UniversalType getReturnType() {
    return switch (this.getKind()) {
    case EXECUTABLE -> this.wrap(((ExecutableType)this.delegate()).getReturnType());
    default -> this.wrap(this.domain().noType(VOID));
    };
  }

  @Override // WildcardType
  public final UniversalType getSuperBound() {
    return switch (this.getKind()) {
    case WILDCARD -> this.wrap(((WildcardType)this.delegate()).getSuperBound());
    default -> null;
    };
  }

  @Override // ExecutableType
  public final List<? extends UniversalType> getThrownTypes() {
    return switch (this.getKind()) {
    case EXECUTABLE -> this.wrap(((ExecutableType)this.delegate()).getThrownTypes());
    default -> List.of();
    };
  }

  @Override // DeclaredType
  public final List<? extends UniversalType> getTypeArguments() {
    return switch (this.getKind()) {
    case DECLARED -> this.wrap(((DeclaredType)this.delegate()).getTypeArguments());
    default -> List.of();
    };
  }

  @Override // ExecutableType
  public final List<? extends UniversalType> getTypeVariables() {
    return switch (this.getKind()) {
    case EXECUTABLE -> this.wrap(((ExecutableType)this.delegate()).getTypeVariables());
    default -> List.of();
    };
  }

  /**
   * A convenience method that returns {@code true} if and only if this represents the type declared by the {@code
   * java.lang.Object} class.
   *
   * @return {@code true} if and only if this is the type declared by the {@code java.lang.Object} class
   *
   * @see UniversalElement#javaLangObject()
   */
  public final boolean javaLangObject() {
    return this.getKind() == DECLARED && this.asElement().javaLangObject();
  }

  /**
   * Returns {@code true} if and only if this {@link UniversalType} represents a <dfn>parameterized type</dfn>.
   *
   * @return {@code true} if and only if this {@link UniversalType} represents a parameterized type
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-4.html#jls-4.5 Java Language Specification, section
   * 4.5
   */
  public final boolean parameterized() {
    return this.getKind() == DECLARED && !this.getTypeArguments().isEmpty();
  }

  /**
   * Returns {@code true} if and only if this {@link UniversalType} represents a <dfn>prototypical type</dfn>.
   *
   * <p>Prototypical types are not defined by the Java Language Specification. They are partially defined by the
   * {@linkplain javax.lang.model.element.TypeElement#asType() specification of the <code>TypeElement#asType()</code>
   * method}.</p>
   *
   * @return {@code true} if and only if this {@link UniversalType} represents a <dfn>prototypical type</dfn>
   *
   * @see javax.lang.model.element.TypeElement#asType()
   */
  public final boolean prototypical() {
    return this.getKind() == DECLARED && this.equals(this.asElement().asType());
  }

  /**
   * Returns {@code true} if and only if this {@link UniversalType} represents the <dfn>raw</dfn> usage of either a
   * {@linkplain #parameterized() parameterized} type or an array type whose {@linkplain #elementType() element type} is
   * parameterized.
   *
   * @return {@code true} if this {@link UniversalType} is the raw usage of either a parameterized type or an array type
   * whose element type is parameterized
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-4.html#jls-4.5 Java Language Specification, section
   * 4.5
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-4.html#jls-4.8 Java Language Specification, section
   * 4.8
   */
  public final boolean raw() {
    return switch (this.getKind()) {
    case ARRAY -> this.elementType().raw();
    case DECLARED -> this.asElement().generic() && this.getTypeArguments().isEmpty();
    default -> false;
    };
  }

  /**
   * Returns the {@link UniversalType} that is the <dfn>raw</dfn> usage of this {@link UniversalType}, if it is capable
   * of having such a usage, <strong>or {@code null} if it is not</strong>.
   *
   * @return a {@link UniversalType} whose {@link #raw()} method is guaranteed to return {@code true}, or {@code null}
   *
   * @see #raw()
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-4.html#jls-4.8 Java Language Specification, section
   * 4.8
   */
  public final UniversalType rawType() {
    return switch (this.getKind()) {
    case ARRAY -> this.elementType().rawType();
    default -> this.parameterized() ? this.erasure() : null;
    };
  }

  /**
   * Returns {@code true} if and only if this {@link UniversalType} is the <dfn>same type</dfn> as the supplied {@link
   * TypeMirror}.
   *
   * <p>The definition of <dfn>type sameness</dfn> appears in the contract of the {@link Domain#sameType(TypeMirror,
   * TypeMirror)} method, which, in turn, relies on the contract of the {@link
   * javax.lang.model.util.Types#isSameType(TypeMirror, TypeMirror)} method.</p>
   *
   * @param t a {@link TypeMirror}; may be {@code null} in which case {@code false} will be returned
   *
   * @return {@code true} if and only if this {@link UniversalType} is the <dfn>same type</dfn> as the supplied {@link
   * TypeMirror}
   *
   * @see Domain#sameType(TypeMirror, TypeMirror)
   *
   * @see javax.lang.model.util.Types#isSameType(TypeMirror, TypeMirror)
   *
   * @see #equals(Object)
   *
   * @see TypeMirror#equals(Object)
   */
  public final boolean sameType(final TypeMirror t) {
    return this.domain().sameType(this, t);
  }

  private final List<? extends UniversalType> wrap(final Collection<? extends TypeMirror> ts) {
    return of(ts, this.domain());
  }

  private final UniversalType wrap(final TypeMirror t) {
    return of(t, this.domain());
  }


  /*
   * Static methods.
   */


  /**
   * Returns a non-{@code null}, immutable {@link List} of {@link UniversalType}s whose elements wrap the supplied
   * {@link List}'s elements.
   *
   * @param ts a {@link Collection} of {@link TypeMirror}s; must not be {@code null}
   *
   * @param domain a {@link Domain}; must not be {@code null}
   *
   * @return a non-{@code null}, immutable {@link List} of {@link UniversalType}s
   *
   * @exception NullPointerException if either argument is {@code null}
   */
  public static final List<? extends UniversalType> of(final Collection<? extends TypeMirror> ts, final Domain domain) {
    if (ts.isEmpty()) {
      return List.of();
    }
    final List<UniversalType> newTs = new ArrayList<>(ts.size());
    for (final TypeMirror t : ts) {
      newTs.add(of(t, domain));
    }
    return Collections.unmodifiableList(newTs);
  }

  /**
   * Returns a non-{@code null} {@link UniversalType} that is either the supplied {@link TypeMirror} (if it itself is
   * {@code null} or is a {@link UniversalType}) or one that wraps it.
   *
   * @param t a {@link TypeMirror}; may be {@code null} in which case {@code null} will be returned
   *
   * @param domain a {@link Domain}; must not be {@code null}
   *
   * @return a {@link UniversalType}, or {@code null} (if {@code t} is {@code null})
   *
   * @exception NullPointerException if {@code domain} is {@code null}
   *
   * @see #UniversalType(TypeMirror, Domain)
   */
  public static final UniversalType of(final TypeMirror t, final Domain domain) {
    return switch (t) {
    case null -> null;
    case UniversalType ut -> ut;
    default -> new UniversalType(t, domain);
    };
  }

}
