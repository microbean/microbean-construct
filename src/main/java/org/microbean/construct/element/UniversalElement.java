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
package org.microbean.construct.element;

import java.lang.annotation.Annotation;

import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import java.util.concurrent.locks.Lock;

import java.util.function.Supplier;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.ModuleElement.Directive;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.microbean.construct.Domain;

import org.microbean.construct.constant.Constables;

import org.microbean.construct.type.UniversalType;

@SuppressWarnings("preview")
public final class UniversalElement
  implements Constable,
             ExecutableElement,
             ModuleElement,
             PackageElement,
             Parameterizable,
             RecordComponentElement,
             TypeElement,
             TypeParameterElement,
             VariableElement {

  private final Domain domain;

  // volatile not needed
  private Supplier<? extends Element> delegateSupplier;

  @SuppressWarnings("try")
  public UniversalElement(final Element delegate, final Domain domain) {
    super();
    Objects.requireNonNull(delegate, "delegate");
    this.domain = Objects.requireNonNull(domain, "domain");
    this.delegateSupplier = switch (delegate) {
      case null -> throw new NullPointerException("delegate");
      case UniversalElement ue -> () -> ue;
      default -> () -> {
        final Element unwrappedDelegate = unwrap(delegate);
        assert !(unwrappedDelegate instanceof UniversalElement);
        try (var lock = this.domain.lock()) {
          // Complete symbols on first access
          unwrappedDelegate.getKind();
          // A lock is no longer needed
          this.delegateSupplier = () -> unwrappedDelegate;
        }
        return unwrappedDelegate;
      };
    };
  }

  @Override // Element
  public final <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
    return switch (this.getKind()) {
    case
      ANNOTATION_TYPE,
      CLASS,
      ENUM,
      INTERFACE,
      RECORD                 -> v.visitType(this, p);
    case TYPE_PARAMETER      -> v.visitTypeParameter(this, p);
    case
      BINDING_VARIABLE,
      ENUM_CONSTANT,
      EXCEPTION_PARAMETER,
      FIELD, LOCAL_VARIABLE,
      PARAMETER,
      RESOURCE_VARIABLE      -> v.visitVariable(this, p);
    case RECORD_COMPONENT    -> v.visitRecordComponent(this, p);
    case
      CONSTRUCTOR,
      INSTANCE_INIT,
      METHOD,
      STATIC_INIT            -> v.visitExecutable(this, p);
    case PACKAGE             -> v.visitPackage(this, p);
    case MODULE              -> v.visitModule(this, p);
    case OTHER               -> v.visitUnknown(this, p);
    };
  }

  @Override // Element
  public final TypeMirror asType() {
    return UniversalType.of(this.delegate().asType(), this.domain);
  }

  public final Element delegate() {
    return this.delegateSupplier.get();
  }

  @Override // Constable
  public final Optional<? extends ConstantDesc> describeConstable() {
    assert !(this.delegate() instanceof UniversalElement);
    return Constables.describe(this.delegate(), this.domain);
  }

  @Override // RecordComponentElement
  public final UniversalElement getAccessor() {
    return switch (this.getKind()) {
    case RECORD_COMPONENT -> this.wrap(((RecordComponentElement)this.delegate()).getAccessor());
    default -> null;
    };
  }

  @Override // Element
  public final <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
    return this.delegate().getAnnotation(annotationType); // TODO: wrap?
  }

  @Override // Element
  public final List<? extends AnnotationMirror> getAnnotationMirrors() {
    return this.delegate().getAnnotationMirrors(); // TOOD: wrap?
  }

  @Override // Element
  public final <A extends Annotation> A[] getAnnotationsByType(final Class<A> annotationType) {
    return this.delegate().getAnnotationsByType(annotationType); // TODO: wrap?
  }

  @Override // TypeParameterElement
  public final List<? extends UniversalType> getBounds() {
    return switch (this.getKind()) {
    case TYPE_PARAMETER -> UniversalType.of(((TypeParameterElement)this.delegate()).getBounds(), this.domain);
    default -> List.of();
    };
  }

  @Override // VariableElement
  public final Object getConstantValue() {
    return switch (this.getKind()) {
    case BINDING_VARIABLE, ENUM_CONSTANT, EXCEPTION_PARAMETER, FIELD, LOCAL_VARIABLE, PARAMETER, RESOURCE_VARIABLE ->
      ((VariableElement)this.delegate()).getConstantValue();
    default -> null;
    };
  }

  @Override // ExecutableElement
  public final AnnotationValue getDefaultValue() {
    return switch (this.getKind()) {
    case ANNOTATION_TYPE ->
      // TODO: delegating annotation value? could be a type mirror after all
      ((ExecutableElement)this.delegate()).getDefaultValue();
    default -> null;
    };
  }

  @Override // ModuleElement
  public final List<? extends Directive> getDirectives() {
    return switch (this.getKind()) {
    case MODULE -> ((ModuleElement)this.delegate()).getDirectives(); // TODO: wrap? Probably not necessary
    default -> List.of();
    };
  }

  @Override // Element
  public final List<? extends UniversalElement> getEnclosedElements() {
    return this.wrap(this.delegate().getEnclosedElements());
  }

  @Override // Element
  public final UniversalElement getEnclosingElement() {
    return this.wrap(this.delegate().getEnclosingElement());
  }

  @Override // TypeParameterElement
  public final UniversalElement getGenericElement() {
    return switch (this.getKind()) {
    case TYPE_PARAMETER -> this.wrap(((TypeParameterElement)this.delegate()).getGenericElement());
    default -> this.getEnclosingElement(); // illegal state
    };
  }

  @Override // TypeElement
  public final List<? extends UniversalType> getInterfaces() {
    return switch (this.getKind()) {
    case ANNOTATION_TYPE, CLASS, ENUM, INTERFACE, RECORD ->
      UniversalType.of(((TypeElement)this.delegate()).getInterfaces(), this.domain);
    default -> List.of();
    };
  }

  @Override // Element
  public final ElementKind getKind() {
    return this.delegate().getKind();
  }

  @Override // Element
  public final Set<Modifier> getModifiers() {
    return this.delegate().getModifiers();
  }

  @Override // TypeElement
  public final NestingKind getNestingKind() {
    return switch (this.getKind()) {
    case ANNOTATION_TYPE, CLASS, ENUM, INTERFACE, RECORD -> ((TypeElement)this.delegate()).getNestingKind();
    default -> NestingKind.TOP_LEVEL; // illegal state
    };
  }

  @Override // ExecutableElement
  public final List<? extends UniversalElement> getParameters() {
    return switch (this.getKind()) {
    case CONSTRUCTOR, METHOD -> this.wrap(((ExecutableElement)this.delegate()).getParameters());
    default -> List.of();
    };
  }

  @Override // ModuleElement, PackageElement, TypeElement
  public final Name getQualifiedName() {
    return switch (this.getKind()) {
    case ANNOTATION_TYPE, CLASS, ENUM, INTERFACE, MODULE, PACKAGE, RECORD ->
      ((QualifiedNameable)this.delegate()).getQualifiedName();
    default -> this.domain.unnamedName();
    };
  }

  @Override // ExecutableElement
  public final UniversalType getReceiverType() {
    return
      this.getKind().isExecutable() ?
      UniversalType.of(this.asType(), this.domain).getReceiverType() :
      UniversalType.of(this.domain.noType(TypeKind.NONE), this.domain);
  }

  @Override // TypeElement
  public final List<? extends UniversalElement> getRecordComponents() {
    return switch (this.getKind()) {
    case RECORD -> this.wrap(((TypeElement)this.delegate()).getRecordComponents());
    default -> List.of();
    };
  }

  @Override // ExecutableElement
  public final UniversalType getReturnType() {
    return UniversalType.of(this.asType(), this.domain).getReturnType();
  }

  @Override // Element
  public final Name getSimpleName() {
    return this.domain.name(this.delegate().getSimpleName());
  }

  @Override // TypeElement
  public final UniversalType getSuperclass() {
    return switch (this.getKind()) {
    case ANNOTATION_TYPE, CLASS, ENUM, INTERFACE, RECORD ->
      UniversalType.of(((TypeElement)this.delegate()).getSuperclass(), this.domain);
    default -> UniversalType.of(this.domain.noType(TypeKind.NONE), this.domain);
    };
  }

  @Override // ExecutableElement
  public final List<? extends UniversalType> getThrownTypes() {
    return
      this.getKind().isExecutable() ?
      UniversalType.of(((ExecutableElement)this.delegate()).getThrownTypes(), this.domain) :
      List.of();
  }

  @Override // ExecutableElement
  public final List<? extends UniversalElement> getTypeParameters() {
    return switch (this.getKind()) {
    case CLASS, CONSTRUCTOR, ENUM, INTERFACE, RECORD, METHOD ->
      this.wrap(((Parameterizable)this.delegate()).getTypeParameters());
    default -> List.of();
    };
  }

  @Override // ExecutableElement
  public final boolean isDefault() {
    return switch (this.getKind()) {
    case METHOD -> ((ExecutableElement)this.delegate()).isDefault();
    default     -> false;
    };
  }

  @Override // ModuleElement
  public final boolean isOpen() {
    return switch (this.getKind()) {
    case MODULE -> ((ModuleElement)this.delegate()).isOpen();
    default     -> false;
    };
  }

  @Override // ModuleElement, PackageElement
  public final boolean isUnnamed() {
    return switch (this.getKind()) {
    case MODULE  -> ((ModuleElement)this.delegate()).isUnnamed();
    case PACKAGE -> ((PackageElement)this.delegate()).isUnnamed();
    default      -> this.getSimpleName().isEmpty();
    };
  }

  @Override // ExecutableElement
  public final boolean isVarArgs() {
    return switch (this.getKind()) {
    case CONSTRUCTOR, METHOD -> ((ExecutableElement)this.delegate()).isVarArgs();
    default                  -> false;
    };
  }

  @Override // Element
  public final int hashCode() {
    return this.delegate().hashCode();
  }

  @Override // Element
  public final boolean equals(final Object other) {
    if (this == other) {
      return true;
    } else if (other != null && other.getClass() == this.getClass()) {
      return this.delegate().equals(((UniversalElement)other).delegate());
    } else {
      return false;
    }
  }

  @Override // Element
  public final String toString() {
    return this.delegate().toString();
  }

  private final UniversalElement wrap(final Element e) {
    return of(e, this.domain);
  }

  private final List<UniversalElement> wrap(final Collection<? extends Element> es) {
    return of(es, this.domain);
  }


  /*
   * Static methods.
   */


  public static final List<UniversalElement> of(final Collection<? extends Element> es, final Domain domain) {
    final List<UniversalElement> newEs = new ArrayList<>(es.size());
    for (final Element e : es) {
      newEs.add(of(e, domain));
    }
    return Collections.unmodifiableList(newEs);
  }

  public static final UniversalElement of(final Element e, final Domain domain) {
    return switch (e) {
    case null ->  null;
    case UniversalElement ue -> ue;
    default -> new UniversalElement(e, domain);
    };
  }

  @SuppressWarnings("unchecked")
  public static final <E extends Element> E unwrap(E e) {
    while (e instanceof UniversalElement ue) {
      e = (E)ue.delegate();
    }
    return e;
  }

}
