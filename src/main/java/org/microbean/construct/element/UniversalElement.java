/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2024–2026 microBean™.
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
import java.util.List;
import java.util.Set;

import java.util.function.Supplier;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.TypeKind;

import org.microbean.construct.UniversalConstruct;
import org.microbean.construct.PrimordialDomain;

import org.microbean.construct.type.UniversalType;

import static java.util.Collections.unmodifiableList;

/**
 * An {@link Element} and {@link UniversalConstruct} implementation.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see Element#getKind()
 *
 * @see UniversalConstruct
 */
@SuppressWarnings("preview") // isUnnamed() usage
public final class UniversalElement
  extends UniversalConstruct<Element, UniversalElement>
  implements ExecutableElement,
             ModuleElement,
             PackageElement,
             RecordComponentElement,
             TypeElement,
             TypeParameterElement,
             VariableElement {

  // volatile not needed
  private Supplier<? extends List<? extends UniversalElement>> enclosedElementsSupplier;

  /**
   * Creates a new {@link UniversalElement}.
   *
   * @param delegate an {@link Element} to which operations will be delegated; must not be {@code null}
   *
   * @param domain a {@link PrimordialDomain} from which the supplied {@code delegate} is presumed to have originated;
   * must not be {@code null}
   *
   * @exception NullPointerException if either argument is {@code null}
   */
  public UniversalElement(final Element delegate, final PrimordialDomain domain) {
    this(delegate, null, domain);
  }

  /**
   * Creates a new {@link UniversalElement}.
   *
   * @param delegate an {@link Element} to which operations will be delegated; must not be {@code null}
   *
   * @param annotations a {@link List} of {@link AnnotationMirror} instances representing annotations, often {@linkplain
   * SyntheticAnnotationMirror synthetic}, that this {@link UniversalElement} should bear; may be {@code null} in which
   * case only the annotations from the supplied {@code delegate} will be used
   *
   * @param domain a {@link PrimordialDomain} from which the supplied {@code delegate} is presumed to have originated;
   * must not be {@code null}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @see #delegate()
   */
  @SuppressWarnings("try")
  private UniversalElement(final Element delegate,
                           final List<? extends AnnotationMirror> annotations,
                           final PrimordialDomain domain) {
    super(delegate, annotations, domain);
    this.enclosedElementsSupplier = () -> {
      final List<? extends UniversalElement> ees;
      try (var lock = domain.lock()) {
        ees = this.wrap(this.delegate().getEnclosedElements());
        this.enclosedElementsSupplier = () -> ees;
      }
      return ees;
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
      FIELD,
      LOCAL_VARIABLE,
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

  @Override // UniversalConstruct<Element, UniversalElement<Element>>
  protected final UniversalElement annotate(final List<? extends AnnotationMirror> replacementAnnotations) {
    return new UniversalElement(this.delegate(), replacementAnnotations, this.domain());
  }

  @Override // Element
  public final UniversalType asType() {
    return UniversalType.of(this.delegate().asType(), this.domain());
  }

  /**
   * Returns {@code true} if and only if this {@link UniversalElement} is a <dfn>generic class declaration</dfn>.
   *
   * @return {@code true} if and only if this {@link UniversalElement} is a <dfn>generic class declaration</dfn>
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se23/html/jls-8.html#jls-8.1.2 Java Language Specification, section
   * 8.1.2
   */
  public final boolean generic() {
    return switch (this.getKind()) {
    case CLASS, CONSTRUCTOR, ENUM, INTERFACE, METHOD, RECORD -> !this.getTypeParameters().isEmpty();
    default -> false;
    };
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
  public final UniversalAnnotationValue getDefaultValue() {
    return switch (this.getKind()) {
      // See
      // https://github.com/openjdk/jdk/blob/jdk-25%2B3/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Symbol.java#L2287-L2290;
      // no concurrent Name access so no lock needed
    case METHOD -> UniversalAnnotationValue.of(((ExecutableElement)this.delegate()).getDefaultValue(), this.domain());
    default -> null;
    };
  }

  @Override // ModuleElement
  public final List<? extends UniversalDirective> getDirectives() {
    return switch (this.getKind()) {
    case MODULE -> UniversalDirective.of(((ModuleElement)this.delegate()).getDirectives(), this.domain());
    default -> List.of();
    };
  }

  /*
    java.lang.AssertionError: Filling jrt:/java.base/java/lang/String$CaseInsensitiveComparator.class during DirectoryFileObject[/modules/java.base:java/lang/Integer$IntegerCache.class]
    at jdk.compiler/com.sun.tools.javac.util.Assert.error(Assert.java:162)
    at jdk.compiler/com.sun.tools.javac.code.ClassFinder.fillIn(ClassFinder.java:366)
    at jdk.compiler/com.sun.tools.javac.code.ClassFinder.complete(ClassFinder.java:302)
    at jdk.compiler/com.sun.tools.javac.code.Symbol.complete(Symbol.java:687)
    at jdk.compiler/com.sun.tools.javac.code.Symbol$ClassSymbol.complete(Symbol.java:1455)
    at jdk.compiler/com.sun.tools.javac.code.Symbol.apiComplete(Symbol.java:693)
    at jdk.compiler/com.sun.tools.javac.code.Symbol$TypeSymbol.getEnclosedElements(Symbol.java:864)
    at jdk.compiler/com.sun.tools.javac.code.Symbol$ClassSymbol.getEnclosedElements(Symbol.java:1420)
    at jdk.compiler/com.sun.tools.javac.code.Symbol$ClassSymbol.getEnclosedElements(Symbol.java:1264)
    at org.microbean.construct@0.0.10-SNAPSHOT/org.microbean.construct.element.UniversalElement.getEnclosedElements(UniversalElement.java:195)
  */
  // See https://github.com/microbean/microbean-construct/issues/18
  @Override // Element
  public final List<? extends UniversalElement> getEnclosedElements() {
    return this.enclosedElementsSupplier.get();
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

  /**
   * A convenience method that returns {@code true} if this {@link UniversalElement} represents the class declaration
   * for {@code java.lang.Object}.
   *
   * @return {@code true} if this {@link UniversalElement} is the class declaration for {@code java.lang.Object}
   */
  public final boolean javaLangObject() {
    return switch (this.getKind()) {
    case CLASS -> this.getQualifiedName().contentEquals("java.lang.Object");
    default -> false;
    };
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
   * @param domain a {@link PrimordialDomain}; must not be {@code null}
   *
   * @return a non-{@code null}, immutable {@link List} of {@link UniversalElement}s
   *
   * @exception NullPointerException if either argument is {@code null}
   */
  public static final List<? extends UniversalElement> of(final Collection<? extends Element> es, final PrimordialDomain domain) {
    if (es.isEmpty()) {
      return List.of();
    }
    final List<UniversalElement> newEs = new ArrayList<>(es.size());
    for (final Element e : es) {
      newEs.add(of(e, domain));
    }
    return unmodifiableList(newEs);
  }

  /**
   * Returns a {@link UniversalElement} that is either the supplied {@link Element} (if it itself is {@code null} or is
   * a {@link UniversalElement}) or one that wraps it.
   *
   * @param e an {@link Element}; may be {@code null} in which case {@code null} will be returned
   *
   * @param domain a {@link PrimordialDomain}; must not be {@code null}
   *
   * @return a {@link UniversalElement}, or {@code null} (if {@code e} is {@code null})
   *
   * @exception NullPointerException if {@code domain} is {@code null}
   *
   * @see #UniversalElement(Element, PrimordialDomain)
   */
  public static final UniversalElement of(final Element e, final PrimordialDomain domain) {
    return switch (e) {
    case null -> null;
    case UniversalElement ue -> ue;
    default -> new UniversalElement(e, domain);
    };
  }

}
