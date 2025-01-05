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

import java.lang.constant.ClassDesc;
import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import java.util.function.Supplier;

import javax.annotation.processing.ProcessingEnvironment;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;

import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.microbean.construct.element.StringName;
import org.microbean.construct.element.UniversalElement;

import org.microbean.construct.type.UniversalType;

import static java.lang.constant.ConstantDescs.BSM_INVOKE;

/**
 * A {@linkplain Domain domain of Java constructs} that can be used at annotation processing time or at runtime.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see Domain
 *
 * @see RuntimeProcessingEnvironmentSupplier
 */
@SuppressWarnings({ "try", "unchecked" })
public class DefaultDomain implements Constable, Domain {

  private final Supplier<? extends ProcessingEnvironment> pe;

  private final Supplier<? extends Unlockable> locker;

  /**
   * Creates a new {@link DefaultDomain} <strong>for use at runtime</strong>.
   *
   * @see #DefaultDomain(ProcessingEnvironment, Lock)
   */
  public DefaultDomain() {
    this(null, null);
  }

  /**
   * Creates a new {@link DefaultDomain} whose usage type is determined by the argument supplied to this constructor.
   *
   * @param pe a {@link ProcessingEnvironment}; may be {@code null} in which case the return value of an invocation of
   * {@link Supplier#get()} on the return value of an invocation of {@link RuntimeProcessingEnvironmentSupplier#of()}
   * will be used instead
   *
   * @see #DefaultDomain(ProcessingEnvironment, Lock)
   *
   * @see SymbolCompletionLock
   */
  public DefaultDomain(final ProcessingEnvironment pe) {
    this(pe, null);
  }

  /**
   * Creates a new {@link DefaultDomain} <strong>for use at runtime</strong>.
   *
   * @param lock a {@link Lock} to use to serialize symbol completion; may be {@code null} in which case a global {@link
   * ReentrantLock} will be used instead
   *
   * @see #DefaultDomain(ProcessingEnvironment, Lock)
   *
   * @see RuntimeProcessingEnvironmentSupplier
   *
   * @see SymbolCompletionLock
   */
  public DefaultDomain(final Lock lock) {
    this(null, lock);
  }

  /**
   * Creates a new {@link DefaultDomain} whose usage type is determined by the arguments supplied to this constructor.
   *
   * @param pe a {@link ProcessingEnvironment}; may be {@code null} in which case the return value of an invocation of
   * {@link Supplier#get()} on the return value of an invocation of {@link RuntimeProcessingEnvironmentSupplier#of()}
   * will be used instead
   *
   * @param lock a {@link Lock} to use to serialize symbol completion; if {@code null} and {@code pe} is {@code null},
   * then a global {@link ReentrantLock} will be used instead; if {@code null} and {@code pe} is non-{@code null}, then
   * no serialization of symbol completion will occur and this {@link DefaultDomain} will not be safe for concurrent use
   * by multiple threads
   *
   * @see RuntimeProcessingEnvironmentSupplier
   *
   * @see SymbolCompletionLock
   */
  public DefaultDomain(final ProcessingEnvironment pe, final Lock lock) {
    super();
    if (pe == null) {
      this.pe = RuntimeProcessingEnvironmentSupplier.of();
      final Lock l = lock == null ? SymbolCompletionLock.INSTANCE : lock;
      this.locker = () -> {
        l.lock();
        return l::unlock;
      };
    } else {
      this.pe = () -> pe;
      this.locker = lock == null ? DefaultDomain::noopLock : () -> {
        lock.lock();
        return lock::unlock;
      };
    }
  }


  /*
   * Instance methods.
   */


  /**
   * Returns a {@link UniversalType} representing an {@link javax.lang.model.type.ArrayType} whose {@linkplain
   * javax.lang.model.type.ArrayType#getComponentType() component type} {@linkplain #sameType(TypeMirror, TypeMirror) is
   * the same as} the supplied {@link TypeMirror}.
   *
   * @param t a {@link TypeMirror} representing a {@linkplain javax.lang.model.type.ArrayType#getComponentType()
   * component type}; must not be {@code null}
   *
   * @return a non-{@code null} {@link UniversalType}
   *
   * @exception NullPointerException if {@code t} is {@code null}
   *
   * @exception IllegalArgumentException if {@code t} is unsuitable for use as a {@linkplain
   * javax.lang.model.type.ArrayType#getComponentType() component type}
   *
   * @see Types#getArrayType(TypeMirror)
   */
  @Override // Domain
  public UniversalType arrayTypeOf(TypeMirror t) {
    t = unwrap(t);
    try (var lock = lock()) {
      return UniversalType.of(this.types().getArrayType(t), this);
    }
  }

  @Override // Domain
  public UniversalElement asElement(TypeMirror t) {
    t = unwrap(t);
    try (var lock = lock()) {
      return UniversalElement.of(this.types().asElement(t), this);
    }
  }

  @Override // Domain
  public UniversalType asMemberOf(DeclaredType containingType, Element e) {
    containingType = unwrap(containingType);
    e = unwrap(e);
    try (var lock = lock()) {
      return UniversalType.of(this.types().asMemberOf(containingType, e), this);
    }
  }

  @Override // Domain
  // Note the strange positioning of payload and receiver.
  public boolean assignable(TypeMirror payload, TypeMirror receiver) {
    payload = unwrap(payload);
    receiver = unwrap(receiver);
    try (var lock = lock()) {
      return this.types().isAssignable(payload, receiver);
    }
  }

  @Override // Domain
  public StringName binaryName(TypeElement e) {
    e = unwrap(e);
    try (var lock = lock()) {
      return StringName.of(this.elements().getBinaryName(e).toString(), this);
    }
  }

  @Override // Domain
  public UniversalType capture(TypeMirror t) {
    t = unwrap(t);
    try (var lock = lock()) {
      return UniversalType.of(this.types().capture(t), this);
    }
  }

  @Override // Domain
  public boolean contains(TypeMirror t0, TypeMirror t1) {
    t0 = unwrap(t0);
    t1 = unwrap(t1);
    try (var lock = lock()) {
      return this.types().contains(t0, t1);
    }
  }

  // (Convenience.)
  @Override // Domain
  public UniversalType declaredType(final CharSequence canonicalName) {
    return UniversalType.of(Domain.super.declaredType(canonicalName), this);
  }

  @Override // Domain
  public UniversalType declaredType(TypeElement typeElement,
                                    TypeMirror... typeArguments) {
    typeElement = unwrap(typeElement);
    typeArguments = unwrap(typeArguments);
    try (var lock = lock()) {
      return UniversalType.of(this.types().getDeclaredType(typeElement, typeArguments), this);
    }
  }

  @Override // Domain
  public UniversalType declaredType(DeclaredType enclosingType,
                                    TypeElement typeElement,
                                    TypeMirror... typeArguments) {
    enclosingType = unwrap(enclosingType);
    typeElement = unwrap(typeElement);
    typeArguments = unwrap(typeArguments);
    try (var lock = lock()) {
      return UniversalType.of(this.types().getDeclaredType(enclosingType, typeElement, typeArguments), this);
    }
  }

  @Override // Constable
  public Optional<? extends ConstantDesc> describeConstable() {
    return
      Optional.of(DynamicConstantDesc.of(BSM_INVOKE,
                                         MethodHandleDesc.ofConstructor(ClassDesc.of(this.getClass().getName()))));
  }

  @Override // Domain
  public List<? extends UniversalType> directSupertypes(TypeMirror t) {
    t = unwrap(t);
    try (var lock = lock()) {
      return UniversalType.of(this.types().directSupertypes(t), this);
    }
  }

  @Override // Domain
  public Element element(TypeMirror t) {
    t = unwrap(t);
    try (var lock = lock()) {
      return UniversalElement.of(this.types().asElement(t), this);
    }
  }

  private final Elements elements() {
    return this.pe().getElementUtils();
  }

  @Override // Object
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    } else if (other != null && other.getClass() == this.getClass()) {
      return
        Objects.equals(this.pe(), ((DefaultDomain)other).pe()) &&
        Objects.equals(this.locker, ((DefaultDomain)other).locker);
    } else {
      return false;
    }
  }

  @Override // Domain
  public UniversalType erasure(TypeMirror t) {
    t = unwrap(t);
    try (var lock = lock()) {
      return UniversalType.of(this.types().erasure(t), this); // (unchecked warning; OK)
    }
  }

  // (Convenience.)
  @Override // Domain
  public UniversalElement executableElement(final TypeElement declaringElement,
                                            final TypeMirror returnType,
                                            final CharSequence name,
                                            final TypeMirror... parameterTypes) {
    return UniversalElement.of(Domain.super.executableElement(declaringElement, returnType, name, parameterTypes), this);
  }

  @Override // Object
  public int hashCode() {
    return this.pe().hashCode() ^ this.locker.hashCode();
  }

  // (Convenience.)
  @Override // Domain
  public UniversalElement javaLangObject() {
    return UniversalElement.of(Domain.super.javaLangObject(), this);
  }

  /**
   * Returns a non-{@code null} {@link Unlockable} that should be used in a {@code try}-with-resources block guarding
   * operations that might cause symbol completion.
   *
   * @return a non-{@code null} {@link Unlockable}
   *
   * @see Unlockable#close()
   */
  public final Unlockable lock() {
    return this.locker.get();
  }

  // (Canonical.)
  @Override // Domain
  public UniversalElement moduleElement(final CharSequence canonicalName) {
    try (var lock = lock()) {
      return UniversalElement.of(this.elements().getModuleElement(canonicalName), this);
    }
  }

  // (Canonical.)
  @Override // Domain
  public StringName name(final CharSequence name) {
    return switch (name) {
    case StringName sn -> sn;
    // You will be tempted to add other efficient cases here. Do not.
    default -> {
      try (var lock = lock()) {
        yield StringName.of(this.elements().getName(name).toString(), this);
      }
    }
    };
  }

  // (Canonical.)
  @Override // Domain
  public UniversalType noType(final TypeKind kind) {
    return UniversalType.of(this.types().getNoType(kind), this);
  }

  // (Canonical.)
  @Override // Domain
  public UniversalType nullType() {
    return UniversalType.of(this.types().getNullType(), this);
  }

  // (Canonical.)
  @Override // Domain
  public UniversalElement packageElement(final CharSequence canonicalName) {
    try (var lock = lock()) {
      return UniversalElement.of(this.elements().getPackageElement(canonicalName), this);
    }
  }

  // (Canonical.)
  @Override // Domain
  public UniversalElement packageElement(ModuleElement asSeenFrom, final CharSequence canonicalName) {
    asSeenFrom = unwrap(asSeenFrom);
    try (var lock = lock()) {
      return UniversalElement.of(this.elements().getPackageElement(asSeenFrom, canonicalName), this);
    }
  }

  private final ProcessingEnvironment pe() {
    return this.pe.get();
  }

  // (Canonical.)
  @Override // Domain
  public UniversalType primitiveType(final TypeKind kind) {
    return UniversalType.of(this.types().getPrimitiveType(kind), this);
  }

  // (Convenience.)
  // (Unboxing.)
  @Override // Domain
  public UniversalType primitiveType(final CharSequence canonicalName) {
    return UniversalType.of(Domain.super.primitiveType(canonicalName), this);
  }

  // (Convenience.)
  // (Unboxing.)
  @Override // Domain
  public UniversalType primitiveType(final TypeElement e) {
    return UniversalType.of(Domain.super.primitiveType(e), this);
  }

  // (Canonical.)
  // (Unboxing.)
  @Override // Domain
  public UniversalType primitiveType(TypeMirror t) {
    t = unwrap(t);
    try (var lock = lock()) {
      return UniversalType.of(this.types().unboxedType(t), this);
    }
  }

  @Override // Domain
  public UniversalType rawType(final TypeMirror t) {
    return UniversalType.of(Domain.super.rawType(t), this);
  }

  // (Canonical.)
  @Override // Domain
  public RecordComponentElement recordComponentElement(ExecutableElement e) {
    e = unwrap(e);
    try (var lock = lock()) {
      return UniversalElement.of(this.elements().recordComponentFor(e), this);
    }
  }

  @Override // Domain
  public boolean sameType(TypeMirror t0, TypeMirror t1) {
    if (t0 == t1) {
      // Critical; javax.lang.model.Types#isSameType(TypeMirror, TypeMirror) returns false here, on purpose, if the two
      // identical references are wildcards. This is almost never what anyone wants.
      return true;
    } else if (t0 == null || t1 == null) {
      return false;
    }
    t0 = unwrap(t0);
    t1 = unwrap(t1);
    try (var lock = lock()) {
      return this.types().isSameType(t0, t1);
    }
  }

  @Override // Domain
  public boolean subsignature(ExecutableType t0, ExecutableType t1) {
    t0 = unwrap(t0);
    t1 = unwrap(t1);
    try (var lock = lock()) {
      return this.types().isSubsignature(t0, t1);
    }
  }

  @Override // Domain
  public boolean subtype(TypeMirror candidateSubtype, TypeMirror candidateSupertype) {
    candidateSubtype = unwrap(candidateSubtype);
    candidateSupertype = unwrap(candidateSupertype);
    try (var lock = lock()) {
      return this.types().isSubtype(candidateSubtype, candidateSupertype);
    }
  }

  @Override // Domain
  public String toString(final CharSequence name) {
    return switch (name) {
    case null -> null;
    case String s -> s;
    case StringName sn -> sn.value();
    case Name n -> {
      try (var lock = lock()) {
        yield n.toString();
      }
    }
    default -> name.toString();
    };
  }

  // (Canonical.)
  @Override // Domain
  public UniversalElement typeElement(final CharSequence canonicalName) {
    try (var lock = lock()) {
      return UniversalElement.of(this.elements().getTypeElement(switch (canonicalName.toString()) {
          case "boolean" -> "java.lang.Boolean";
          case "byte" -> "java.lang.Byte";
          case "char" -> "java.lang.Character";
          case "double" -> "java.lang.Double";
          case "float" -> "java.lang.Float";
          case "int" -> "java.lang.Integer";
          case "long" -> "java.lang.Long";
          case "short" -> "java.lang.Short";
          default -> canonicalName;
          }), this);
    }
  }

  // (Canonical.)
  @Override // Domain
  public UniversalElement typeElement(ModuleElement asSeenFrom, final CharSequence canonicalName) {
    asSeenFrom = unwrap(asSeenFrom);
    try (var lock = lock()) {
      return UniversalElement.of(this.elements().getTypeElement(asSeenFrom, switch (canonicalName.toString()) {
          case "boolean" -> "java.lang.Boolean";
          case "byte" -> "java.lang.Byte";
          case "char" -> "java.lang.Character";
          case "double" -> "java.lang.Double";
          case "float" -> "java.lang.Float";
          case "int" -> "java.lang.Integer";
          case "long" -> "java.lang.Long";
          case "short" -> "java.lang.Short";
          default -> canonicalName;
          }), this);
    }
  }

  // (Canonical.)
  // (Boxing.)
  @Override // Domain
  public UniversalElement typeElement(PrimitiveType t) {
    t = unwrap(t);
    try (var lock = lock()) {
      return UniversalElement.of(this.types().boxedClass(t), this);
    }
  }

  // (Convenience.)
  // (Boxing.)
  @Override // Domain
  public UniversalElement typeElement(final TypeKind primitiveTypeKind) {
    return UniversalElement.of(Domain.super.typeElement(primitiveTypeKind), this);
  }

  // (Convenience.)
  @Override // Domain
  public UniversalElement typeParameterElement(final Parameterizable p, final CharSequence name) {
    return UniversalElement.of(Domain.super.typeParameterElement(p, name), this);
  }

  private final Types types() {
    return this.pe().getTypeUtils();
  }

  // (Convenience.)
  @Override // Domain
  public UniversalType typeVariable(final Parameterizable p, final CharSequence name) {
    return UniversalType.of(Domain.super.typeVariable(p, name), this);
  }

  // (Convenience.)
  @Override // Domain
  public UniversalElement variableElement(final Element e, final CharSequence name) {
    return UniversalElement.of(Domain.super.variableElement(e, name), this);
  }

  @Override // Domain
  public UniversalType wildcardType() {
    return this.wildcardType(null, null);
  }

  @Override // Domain
  public UniversalType wildcardType(TypeMirror extendsBound, TypeMirror superBound) {
    extendsBound = unwrap(extendsBound);
    superBound = unwrap(superBound);
    try (var lock = lock()) {
      return UniversalType.of(this.types().getWildcardType(extendsBound, superBound), this);
    }
  }


  /*
   * Static methods.
   */


  private static final void doNothing() {}

  private static final Unlockable noopLock() {
    return DefaultDomain::doNothing;
  }

  private static final <T extends TypeMirror> T unwrap(final T t) {
    return UniversalType.unwrap(t);
  }

  private static final <E extends Element> E unwrap(final E e) {
    return UniversalElement.unwrap(e);
  }

  private static final TypeMirror[] unwrap(final TypeMirror[] ts) {
    if (ts == null || ts.length <= 0) {
      return ts;
    }
    final TypeMirror[] rv = new TypeMirror[ts.length];
    for (int i = 0; i < ts.length; i++) {
      rv[i] = unwrap(ts[i]);
    }
    return rv;
  }

}
