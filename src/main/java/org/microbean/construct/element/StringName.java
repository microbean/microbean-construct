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
package org.microbean.construct.element;

import java.lang.constant.ClassDesc;
import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;

import java.util.Objects;
import java.util.Optional;

import java.util.stream.IntStream;

import javax.lang.model.element.Name;

import org.microbean.construct.Domain;

import static java.lang.constant.ConstantDescs.BSM_INVOKE;

/**
 * A {@link Name} implementation based on {@link String}s.
 *
 * @param value the actual name; must not be {@code null}
 *
 * @param domain a {@link Domain}; must not be {@code null}
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see Name
 *
 * @see Domain#toString(CharSequence)
 */
public final record StringName(String value, Domain domain) implements Constable, Name {

  /**
   * Creates a new {@link StringName}.
   *
   * @param value the actual name; must not be {@code null}
   *
   * @param domain a {@link Domain}; must not be {@code null}
   *
   * @exception NullPointerException if either argument is {@code null}
   */
  public StringName(final CharSequence value, final Domain domain) {
    // We deliberately route even String-typed values through Domain#toString(CharSequence) in case the Domain wishes to
    // cache the intermediate Name.
    this(switch (value) {
      case StringName sn -> sn.value();
      default -> domain.toString(value);
      }, domain);
  }

  /**
   * Creates a new {@link StringName}.
   *
   * @param value the actual name; must not be {@code null}
   *
   * @param domain a {@link Domain}; must not be {@code null}
   *
   * @exception NullPointerException if either argument is {@code null}
   */
  public StringName {
    Objects.requireNonNull(value, "value");
    Objects.requireNonNull(domain, "domain");
  }

  @Override // Name (CharSequence)
  public final char charAt(final int index) {
    return this.value().charAt(index);
  }

  @Override // Name (CharSequence)
  public final IntStream chars() {
    return this.value().chars();
  }

  @Override // Name (CharSequence)
  public final IntStream codePoints() {
    return this.value().codePoints();
  }

  @Override // Name
  @SuppressWarnings("try")
  public final boolean contentEquals(final CharSequence cs) {
    return this == cs || switch (cs) {
    case null -> false;
    case String s -> this.value().contentEquals(s);
    case StringName sn -> this.value().contentEquals(sn.value());
    case Name n -> this.value().contentEquals(this.domain().toString(n));
    default -> this.value().contentEquals(cs.toString());
    };
  }

  @Override // Constable
  public final Optional<? extends ConstantDesc> describeConstable() {
    return (this.domain() instanceof Constable c ? c.describeConstable() : Optional.<ConstantDesc>empty())
      .map(domainDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                MethodHandleDesc.ofConstructor(ClassDesc.of(this.getClass().getName()),
                                                                               ClassDesc.of(CharSequence.class.getName()),
                                                                               ClassDesc.of(Domain.class.getName())),
                                                this.value(),
                                                domainDesc));
  }

  @Override // Record
  public final boolean equals(final Object other) {
    return this == other || switch (other) {
    case null -> false;
    case StringName sn when this.getClass() == sn.getClass() -> Objects.equals(this.value(), sn.value());
    default -> false;
    };
  }

  @Override // Record
  public final int hashCode() {
    return this.value().hashCode();
  }

  @Override // Name (CharSequence)
  public final boolean isEmpty() {
    return this.value().isEmpty();
  }

  @Override // Name (CharSequence)
  public final int length() {
    return this.value().length();
  }

  @Override // Name (CharSequence)
  public final CharSequence subSequence(final int start, final int end) {
    return this.value().subSequence(start, end);
  }

  @Override // Name (CharSequence)
  public final String toString() {
    return this.value();
  }


  /*
   * Static methods.
   */


  /**
   * Returns a {@link StringName} whose {@link #value()} method will return a {@link String} {@linkplain
   * String#equals(Object) equal to} the {@linkplain Domain#toString(CharSequence) <code>String</code> conversion of}
   * the supplied {@link CharSequence}, and whose {@link #domain()} method will return a {@link Domain} {@linkplain
   * #equals(Object) equal to} the supplied {@link Domain}.
   *
   * @param cs a {@link CharSequence}; must not be {@code null}
   *
   * @param domain a {@link Domain}; must not be {@code null}
   *
   * @return a {@link StringName}; never {@code null}
   *
   * @exception NullPointerException if either argument is {@code null}
   *
   * @see Domain#toString(CharSequence)
   */
  public static final StringName of(final CharSequence cs, final Domain domain) {
    return cs instanceof StringName sn ? sn : new StringName(domain.toString(cs), domain);
  }

}
