/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2024 microBean™.
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
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;

/**
 * A domain of Java constructs.
 *
 * <p>A <dfn id="domain">domain</dfn> is a universe of valid Java <a href="#construct">constructs</a>.</p>
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
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 */
@SuppressWarnings("try")
public interface Domain {

  public ArrayType arrayTypeOf(final TypeMirror componentType);

  // Note the strange positioning of payload and receiver.
  public boolean assignable(final TypeMirror payload, final TypeMirror receiver);

  public Name binaryName(final TypeElement e);

  public default TypeElement box(final PrimitiveType t) {
    return switch (t.getKind()) { // won't cause symbol completion
    case BOOLEAN -> this.typeElement("java.lang.Boolean");
    case BYTE -> this.typeElement("java.lang.Byte");
    case CHAR -> this.typeElement("java.lang.Character");
    case DOUBLE -> this.typeElement("java.lang.Double");
    case FLOAT -> this.typeElement("java.lang.Float");
    case INT -> this.typeElement("java.lang.Integer");
    case LONG -> this.typeElement("java.lang.Long");
    case SHORT -> this.typeElement("java.lang.Short");
    default -> throw new IllegalArgumentException("t: " + t);
    };
  }

  public TypeMirror capture(final TypeMirror wildcard);

  public boolean contains(final TypeMirror candidateContainer, final TypeMirror candidate);

  public default DeclaredType declaredType(final CharSequence canonicalName) {
    return this.declaredType(this.typeElement(canonicalName));
  }

  public DeclaredType declaredType(final TypeElement typeElement,
                                   final TypeMirror... typeArguments);

  public DeclaredType declaredType(final DeclaredType enclosingType,
                                   final TypeElement typeElement,
                                   final TypeMirror... typeArguments);

  public List<? extends TypeMirror> directSupertypes(final TypeMirror t);

  public Element element(final TypeMirror t);

  public <T extends TypeMirror> T erasure(final T t);

  public default boolean generic(final Element e) {
    if (Objects.requireNonNull(e, "e") instanceof Parameterizable p) {
      try (var lock = this.lock()) {
        return switch (e.getKind()) {
        case CLASS, CONSTRUCTOR, ENUM, INTERFACE, METHOD, RECORD -> !p.getTypeParameters().isEmpty();
        default                                                  -> false;
        };
      }
    }
    return false;
  }

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
  
  public ModuleElement moduleElement(final CharSequence canonicalName);

  public Name name(final CharSequence name);

  public NoType noType(final TypeKind kind);

  public NullType nullType();

  public PrimitiveType primitiveType(final TypeKind kind);

  public boolean sameType(final TypeMirror t0, final TypeMirror t1);

  public boolean subtype(TypeMirror t0, TypeMirror t1);

  public TypeElement typeElement(final CharSequence canonicalName);

  public TypeElement typeElement(final ModuleElement asSeenFrom, final CharSequence canonicalName);

  public default PrimitiveType unbox(final TypeElement e) {
    // getQualifiedName() does not cause symbol completion.
    return switch (e.getQualifiedName().toString()) {
    case "java.lang.Boolean"   -> this.primitiveType(TypeKind.BOOLEAN);
    case "java.lang.Byte"      -> this.primitiveType(TypeKind.BYTE);
    case "java.lang.Character" -> this.primitiveType(TypeKind.CHAR);
    case "java.lang.Double"    -> this.primitiveType(TypeKind.DOUBLE);
    case "java.lang.Float"     -> this.primitiveType(TypeKind.FLOAT);
    case "java.lang.Integer"   -> this.primitiveType(TypeKind.INT);
    case "java.lang.Long"      -> this.primitiveType(TypeKind.LONG);
    case "java.lang.Short"     -> this.primitiveType(TypeKind.SHORT);
    default -> throw new IllegalArgumentException("e: " + e);
    };
  }

  public default Name unnamedName() {
    return this.name("");
  }

  public default WildcardType wildcardType() {
    return this.wildcardType(null, null);
  }

  public WildcardType wildcardType(TypeMirror extendsBound, TypeMirror superBound);

}