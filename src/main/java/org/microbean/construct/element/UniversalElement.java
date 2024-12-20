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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.ModuleElement.Directive;
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

import org.microbean.construct.UniversalConstruct;
import org.microbean.construct.Domain;

import org.microbean.construct.type.UniversalType;

/**
 * An {@link Element} and {@link UniversalConstruct} implementation.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see Element#getKind()
 *
 * @see UniversalConstruct
 */
@SuppressWarnings("preview")
public final class UniversalElement
  extends UniversalConstruct<Element>
  implements ExecutableElement,
             ModuleElement,
             PackageElement,
             RecordComponentElement,
             TypeElement,
             TypeParameterElement,
             VariableElement {

  /**
   * Creates a new {@link UniversalElement}.
   *
   * @param delegate an {@link Element} to which operations will be delegated; must not be {@code null}
   *
   * @param domain a {@link Domain} from which the supplied {@code delegate} is presumed to have originated; must not be
   * {@code null}
   *
   * @exception NullPointerException if either argument is {@code null}
   *
   * @see #delegate()
   */
  public UniversalElement(final Element delegate, final Domain domain) {
    super(delegate, domain);
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
  public final UniversalType asType() {
    return UniversalType.of(this.delegate().asType(), this.domain());
  }

  @Override // RecordComponentElement
  public final UniversalElement getAccessor() {
    return switch (this.getKind()) {
    case RECORD_COMPONENT -> this.wrap(((RecordComponentElement)this.delegate()).getAccessor());
    default -> null;
    };
  }

  @Override // TypeParameterElement
  public final List<? extends UniversalType> getBounds() {
    return switch (this.getKind()) {
    case TYPE_PARAMETER -> UniversalType.of(((TypeParameterElement)this.delegate()).getBounds(), this.domain());
    default -> List.of();
    };
  }

  @Override // VariableElement
  @SuppressWarnings("try")
  public final Object getConstantValue() {
    return switch (this.getKind()) {
    case BINDING_VARIABLE, ENUM_CONSTANT, EXCEPTION_PARAMETER, FIELD, LOCAL_VARIABLE, PARAMETER, RESOURCE_VARIABLE -> {
      try (var lock = this.domain().lock()) {
        // There is a LOT going on here; take the domain lock for safety. See
        // https://github.com/openjdk/jdk/blob/jdk-25%2B3/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Symbol.java#L1813-L1829
        yield
          ((VariableElement)this.delegate()).getConstantValue(); // will be a boxed type or String
      }
    }
    default -> null;
    };
  }

  @Override // ExecutableElement
  public final AnnotationValueRecord getDefaultValue() {
    return switch (this.getKind()) {
      // See
      // https://github.com/openjdk/jdk/blob/jdk-25%2B3/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Symbol.java#L2287-L2290;
      // no concurrent Name access so no lock needed
    case METHOD -> AnnotationValueRecord.of(((ExecutableElement)this.delegate()).getDefaultValue(), this.domain());
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
      UniversalType.of(((TypeElement)this.delegate()).getInterfaces(), this.domain());
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
  @SuppressWarnings("try")
  public final StringName getQualifiedName() {
    return switch (this.getKind()) {
    case ANNOTATION_TYPE, CLASS, ENUM, INTERFACE, MODULE, PACKAGE, RECORD -> {
      try (var lock = this.domain().lock()) {
        yield StringName.of(((QualifiedNameable)this.delegate()).getQualifiedName().toString(), this.domain());
      }
    }
    default -> StringName.of("", this.domain());
    };
  }

  @Override // ExecutableElement
  public final UniversalType getReceiverType() {
    return
      this.getKind().isExecutable() ?
      UniversalType.of(this.asType(), this.domain()).getReceiverType() :
      UniversalType.of(this.domain().noType(TypeKind.NONE), this.domain());
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
    return UniversalType.of(this.asType(), this.domain()).getReturnType();
  }

  @Override // Element
  @SuppressWarnings("try")
  public final StringName getSimpleName() {
    try (var lock = this.domain().lock()) {
      return new StringName(this.delegate().getSimpleName().toString(), this.domain());
    }
  }

  @Override // TypeElement
  public final UniversalType getSuperclass() {
    return switch (this.getKind()) {
    case ANNOTATION_TYPE, CLASS, ENUM, INTERFACE, RECORD ->
      UniversalType.of(((TypeElement)this.delegate()).getSuperclass(), this.domain());
    default -> UniversalType.of(this.domain().noType(TypeKind.NONE), this.domain());
    };
  }

  @Override // ExecutableElement
  public final List<? extends UniversalType> getThrownTypes() {
    return
      this.getKind().isExecutable() ?
      UniversalType.of(((ExecutableElement)this.delegate()).getThrownTypes(), this.domain()) :
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
    default -> false;
    };
  }

  @Override // ModuleElement
  public final boolean isOpen() {
    return switch (this.getKind()) {
    case MODULE -> ((ModuleElement)this.delegate()).isOpen();
    default -> false;
    };
  }

  @Override // ModuleElement, PackageElement
  public final boolean isUnnamed() {
    return switch (this.getKind()) {
    case MODULE  -> ((ModuleElement)this.delegate()).isUnnamed();
    case PACKAGE -> ((PackageElement)this.delegate()).isUnnamed();
    default -> false;
    };
  }

  @Override // ExecutableElement
  public final boolean isVarArgs() {
    return switch (this.getKind()) {
    case CONSTRUCTOR, METHOD -> ((ExecutableElement)this.delegate()).isVarArgs();
    default -> false;
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

  private final UniversalElement wrap(final Element e) {
    return of(e, this.domain());
  }

  private final List<? extends UniversalElement> wrap(final Collection<? extends Element> es) {
    return of(es, this.domain());
  }


  /*
   * Static methods.
   */


  /**
   * Returns a non-{@code null}, immutable {@link List} of {@link UniversalElement}s whose elements wrap the supplied
   * {@link List}'s elements.
   *
   * @param es a {@link Collection} of {@link Element}s; must not be {@code null}
   *
   * @param domain a {@link Domain}; must not be {@code null}
   *
   * @return a non-{@code null}, immutable {@link List} of {@link UniversalElement}s
   *
   * @exception NullPointerException if either argument is {@code null}
   */
  public static final List<? extends UniversalElement> of(final Collection<? extends Element> es, final Domain domain) {
    final List<UniversalElement> newEs = new ArrayList<>(es.size());
    for (final Element e : es) {
      newEs.add(of(e, domain));
    }
    return Collections.unmodifiableList(newEs);
  }

  /**
   * Returns a {@link UniversalElement} that is either the supplied {@link Element} (if it itself is {@code null} or is
   * a {@link UniversalElement}) or one that wraps it.
   *
   * @param e an {@link Element}; may be {@code null} in which case {@code null} will be returned
   *
   * @param domain a {@link Domain}; must not be {@code null}
   *
   * @return a {@link UniversalElement}, or {@code null} (if {@code e} is {@code null})
   *
   * @exception NullPointerException if {@code domain} is {@code null}
   *
   * @see #UniversalElement(Element, Domain)
   */
  public static final UniversalElement of(final Element e, final Domain domain) {
    return switch (e) {
    case null -> null;
    case UniversalElement ue -> ue;
    default -> new UniversalElement(e, domain);
    };
  }

}
