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
import static javax.lang.model.element.NestingKind.TOP_LEVEL;

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
 * <p>This interface is modeled on a deliberately restricted combination of the {@link javax.lang.model.util.Elements}
 * and {@link javax.lang.model.util.Types} interfaces.</p>
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
   */
  public Name binaryName(final TypeElement e);

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

  public PackageElement packageElement(final CharSequence canonicalName);

  public PackageElement packageElement(final ModuleElement asSeenFrom, final CharSequence canonicalName);

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

  // (Convenience.)
  // (Unboxing.)
  public default PrimitiveType primitiveType(final QualifiedNameable qn) {
    return this.primitiveType(qn.getQualifiedName());
  }

  public PrimitiveType primitiveType(final TypeKind kind);

  // (Unboxing.)
  public PrimitiveType primitiveType(final TypeMirror t);

  public RecordComponentElement recordComponentElement(final ExecutableElement e);

  public boolean sameType(final TypeMirror t0, final TypeMirror t1);

  public boolean subsignature(final ExecutableType t0, final ExecutableType t1);

  public boolean subtype(TypeMirror t0, TypeMirror t1);

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

  public TypeElement typeElement(final CharSequence canonicalName);

  public TypeElement typeElement(final ModuleElement asSeenFrom, final CharSequence canonicalName);

  public default TypeElement typeElement(final PrimitiveType t) {
    try (var lock = this.lock()) {
      return this.typeElement(t.getKind());
    }
  }

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

  public default TypeParameterElement typeParameterElement(Parameterizable p, final CharSequence name) {
    Objects.requireNonNull(name, "name");
    while (p != null) {
      // A call to getTypeParameters() does not cause symbol completion, but name acquisition also needs to be
      // serialized globally.
      try (var lock = this.lock()) {
        final List<? extends TypeParameterElement> tpes = p.getTypeParameters();
        for (final TypeParameterElement tpe : tpes) {
          if (tpe.getSimpleName().contentEquals(name)) {
            return tpe;
          }
        }
      }
      p = switch (p) {
      case ExecutableElement ee -> (Parameterizable)ee.getEnclosingElement();
      case TypeElement te when te.getNestingKind() != TOP_LEVEL -> (Parameterizable)te.getEnclosingElement();
      default -> null;
      };
    }
    return null;
  }

  public default TypeVariable typeVariable(Parameterizable p, final CharSequence name) {
    final TypeParameterElement e = this.typeParameterElement(p, name);
    return e == null ? null : (TypeVariable)e.asType();
  }

  public default Name unnamedName() {
    return this.name("");
  }

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

  public default WildcardType wildcardType() {
    return this.wildcardType(null, null);
  }

  public WildcardType wildcardType(TypeMirror extendsBound, TypeMirror superBound);

}
