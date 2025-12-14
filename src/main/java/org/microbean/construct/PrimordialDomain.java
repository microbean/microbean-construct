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
package org.microbean.construct;

import javax.lang.model.element.Name;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.microbean.construct.element.StringName;
import org.microbean.construct.element.SyntheticName;

/**
 * A view of an underlying domain of valid Java constructs that exposes {@linkplain #nullType() the null type}, various
 * kinds of {@linkplain #noType(TypeKind) pseudo-types}, the {@linkplain #javaLangObjectType() prototypical
 * <code>java.lang.Object</code> type}, and the ability to {@linkplain #lock() globally lock for symbol completion if
 * needed}.
 *
 * <p>{@link PrimordialDomain}s are primordial because they expose the "first principles" constructs needed to compose
 * other constructs.</p>
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see #javaLangObjectType()
 *
 * @see #lock()
 *
 * @see #noType(TypeKind)
 *
 * @see #nullType()
 */
public interface PrimordialDomain {

  /**
   * Returns the (non-{@code null}, determinate) {@link DeclaredType} representing the <a
   * href="https://docs.oracle.com/en/java/javase/25/docs/api/java.compiler/javax/lang/model/element/TypeElement.html#prototypicaltype"><dfn>prototypical
   * type</dfn></a> of {@link Object java.lang.Object}.
   *
   * <p>Implementations of this method must not return {@code null}.</p>
   *
   * <p>{@link DeclaredType} instances returned by implementations of this method must return {@link TypeKind#DECLARED}
   * from their {@link TypeMirror#getKind()} method.</p>
   *
   * @return the {@link DeclaredType} representing the <dfn>prototypical type</dfn> of {@link Object java.lang.Object};
   * never {@code null}
   */
  public DeclaredType javaLangObjectType();

  /**
   * Semantically locks an opaque lock used to serialize symbol completion, and returns it in the form of an {@link
   * Unlockable}.
   *
   * <p>Implementations of this method must not return {@code null}.</p>
   *
   * @return an {@link Unlockable} in a semantically locked state; never {@code null}
   *
   * @see SymbolCompletionLock
   *
   * @see Unlockable#close()
   */
  public Unlockable lock();

  /**
   * Returns a (non-{@code null}, determinate) {@link NoType} representing the supplied {@link TypeKind}, provided it is
   * either {@link TypeKind#NONE} or {@link TypeKind#VOID}.
   *
   * <p>Implementations of this method must not return {@code null}.</p>
   *
   * @param kind a {@link TypeKind}; must be either {@link TypeKind#NONE} or {@link TypeKind#VOID}
   *
   * @return a {@link NoType} representing the supplied {@link TypeKind}; never {@code null}
   *
   * @exception NullPointerException if {@code kind} is {@code null}
   *
   * @exception IllegalArgumentException if {@code kind} is neither {@link TypeKind#NONE} nor {@link TypeKind#VOID}
   *
   * @see javax.lang.model.util.Types#getNoType(TypeKind)
   */
  public NoType noType(final TypeKind kind);

  /**
   * Returns a (non-{@code null}, determinate) {@link NullType} representing the null type.
   *
   * <p>Implementations of thsi method must not return {@code null}.</p>
   *
   * @return a {@link NullType} representing the null type; never {@code null}
   *
   * @see javax.lang.model.util.Types#getNullType()
   */
  public NullType nullType();

  /**
   * A convenience method that converts the supplied {@link CharSequence}, which is often a {@link Name}, into a {@link
   * String}, and returns the conversion, {@linkplain #lock() locking} when appropriate to serialize symbol completion.
   *
   * <p>The default implementation of this method may return {@code null} if the supplied {@code name} is {@code
   * null}.</p>
   *
   * <p>In many implementations of domains, converting a {@link Name} to a {@link String} can cause problems if symbol
   * completion is taking place concurrently and the symbol completion lock is not held. This method helps avoid those
   * problems.</p>
   *
   * <p>Overriding this method is not normally needed.</p>
   *
   * @param name the {@link CharSequence} to convert; may be {@code null} in which case {@code null} will be returned
   *
   * @return a {@link String}, or {@code null} if {@code name} was {@code null}
   *
   * @see #lock()
   *
   * @see Name
   */
  @SuppressWarnings("try")
  public default String toString(final CharSequence name) {
    return switch (name) {
    case null -> null;
    case String s -> s;
    case StringName sn -> sn.value();
    case SyntheticName sn -> sn.toString();
    case Name n -> {
      try (var lock = this.lock()) {
        yield n.toString();
      }
    }
    default -> name.toString();
    };
  }
  
}
