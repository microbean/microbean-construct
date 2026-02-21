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

import java.lang.constant.ClassDesc;
import java.lang.constant.Constable;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;

import java.util.Objects;
import java.util.Optional;

import java.util.stream.IntStream;

import javax.lang.model.element.Name;

import static java.lang.constant.ConstantDescs.BSM_INVOKE;
import static java.lang.constant.ConstantDescs.CD_String;

import static java.lang.constant.MethodHandleDesc.ofConstructor;

import static java.util.Objects.requireNonNull;

/**
 * A {@link Name} implementation based on {@link String}s.
 *
 * <p>This {@link Name} implementation differs from {@link StringName} in that there is no {@link
 * org.microbean.construct.PrimordialDomain} involved, and therefore no notion of any kind of delegate.</p>
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see Name
 *
 * @see StringName
 */
public final class SyntheticName implements Constable, Name {


  /*
   * Instance fields.
   */


  private final String value;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link SyntheticName}.
   *
   * @param value the actual name; must not be {@code null}
   *
   * @exception NullPointerException if {@code value} is {@code null}
   */
  public SyntheticName(final Name value) {
    super();
    this.value = value instanceof SyntheticName sn ? sn.value : value.toString();
  }
  
  /**
   * Creates a new {@link SyntheticName}.
   *
   * @param value the actual name; must not be {@code null}
   *
   * @exception NullPointerException if {@code value} is {@code null}
   */
  public SyntheticName(final String value) {
    super();
    this.value = requireNonNull(value, "value");
  }


  /*
   * Instance methods.
   */


  @Override // Name (CharSequence)
  public final char charAt(final int index) {
    return this.value.charAt(index);
  }

  @Override // Name (CharSequence)
  public final IntStream chars() {
    return this.value.chars();
  }

  @Override // Name (CharSequence)
  public final IntStream codePoints() {
    return this.value.codePoints();
  }

  @Override // Name
  public final boolean contentEquals(final CharSequence cs) {
    return this == cs || cs != null && this.value.contentEquals(cs.toString());
  }

  @Override // Constable
  public final Optional<DynamicConstantDesc<SyntheticName>> describeConstable() {
    return
      Optional.of(DynamicConstantDesc.ofNamed(BSM_INVOKE,
                                              this.value,
                                              this.getClass().describeConstable().orElseThrow(),
                                              ofConstructor(this.getClass().describeConstable().orElseThrow(),
                                                            CD_String),
                                              this.value));
  }

  @Override // Object
  public final boolean equals(final Object other) {
    return this == other || switch (other) {
    case null -> false;
    case SyntheticName sn when this.getClass() == sn.getClass() -> Objects.equals(this.value, sn.value);
    default -> false;
    };
  }

  @Override // Object
  public final int hashCode() {
    return this.value.hashCode();
  }

  @Override // Name (CharSequence)
  public final boolean isEmpty() {
    return this.value.isEmpty();
  }

  @Override // Name (CharSequence)
  public final int length() {
    return this.value.length();
  }

  @Override // Name (CharSequence)
  public final CharSequence subSequence(final int start, final int end) {
    return this.value.subSequence(start, end);
  }

  @Override // Name (CharSequence)
  public final String toString() {
    return this.value;
  }

}
