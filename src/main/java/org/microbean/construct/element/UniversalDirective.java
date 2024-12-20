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
import java.util.Objects;

import java.util.function.Supplier;

import javax.lang.model.element.ModuleElement.Directive;
import javax.lang.model.element.ModuleElement.DirectiveKind;
import javax.lang.model.element.ModuleElement.DirectiveVisitor;
import javax.lang.model.element.ModuleElement.ExportsDirective;
import javax.lang.model.element.ModuleElement.OpensDirective;
import javax.lang.model.element.ModuleElement.ProvidesDirective;
import javax.lang.model.element.ModuleElement.RequiresDirective;
import javax.lang.model.element.ModuleElement.UsesDirective;

import org.microbean.construct.Domain;

/**
 * A {@link Directive} implementation.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see Directive#getKind()
 */
public final class UniversalDirective
  implements ExportsDirective, OpensDirective, ProvidesDirective, RequiresDirective, UsesDirective {

  private final Domain domain;

  // volatile not needed
  private Supplier<? extends Directive> delegateSupplier;

  /**
   * Creates a new {@link UniversalDirective}.
   *
   * @param delegate a {@link Directive} to which operations will be delegated; must not be {@code null}
   *
   * @param domain a {@link Domain} from which the supplied {@code delegate} is presumed to have originated; must not be
   * {@code null}
   *
   * @exception NullPointerException if either argument is {@code null}
   */
  @SuppressWarnings("try")
  public UniversalDirective(final Directive delegate, final Domain domain) {
    super();
    this.domain = Objects.requireNonNull(domain, "domain");
    final Directive unwrappedDelegate = unwrap(Objects.requireNonNull(delegate, "delegate"));
    final Runnable symbolCompleter = unwrappedDelegate::getKind;
    this.delegateSupplier = () -> {
      try (var lock = domain.lock()) {
        symbolCompleter.run();
        this.delegateSupplier = () -> unwrappedDelegate;
      }
      return unwrappedDelegate;
    };
  }

  @Override // Directive
  public final <R, P> R accept(final DirectiveVisitor<R, P> v, final P p) {
    return switch (this.getKind()) {
    case EXPORTS -> v.visitExports(this, p);
    case OPENS -> v.visitOpens(this, p);
    case PROVIDES -> v.visitProvides(this, p);
    case REQUIRES -> v.visitRequires(this, p);
    case USES -> v.visitUses(this, p);
    };
  }

  /**
   * Returns the delegate to which operations are delegated.
   *
   * @return a non-{@code null} delegate
   *
   * @see Directive
   */
  public final Directive delegate() {
    return this.delegateSupplier.get();
  }

  @Override // Object
  public final boolean equals(final Object other) {
    return other == this || switch (other) {
    case null -> false;
    case UniversalDirective her -> this.delegate().equals(her.delegate());
    case Directive her -> this.delegate().equals(her);
    default -> false;
    };
  }

  @Override // RequiresDirective
  public final UniversalElement getDependency() {
    return switch(this.getKind()) {
    case REQUIRES -> UniversalElement.of(((RequiresDirective)this.delegate()).getDependency(), this.domain);
    case EXPORTS, OPENS, PROVIDES, USES -> null;
    };
  }

  @Override // ProvidesDirective
  public final List<? extends UniversalElement> getImplementations() {
    return switch (this.getKind()) {
    case PROVIDES -> UniversalElement.of(((ProvidesDirective)this.delegate()).getImplementations(), this.domain);
    case EXPORTS, OPENS, REQUIRES, USES -> List.of();
    };
  }

  @Override // Directive
  public final DirectiveKind getKind() {
    return this.delegate().getKind();
  }

  @Override // ExportsDirective, OpensDirective
  public final UniversalElement getPackage() {
    return switch (this.getKind()) {
    case EXPORTS -> UniversalElement.of(((ExportsDirective)this.delegate()).getPackage(), this.domain);
    case OPENS -> UniversalElement.of(((OpensDirective)this.delegate()).getPackage(), this.domain);
    case PROVIDES, REQUIRES, USES -> null;
    };
  }

  @Override // ProvidesDirective
  public final UniversalElement getService() {
    return switch (this.getKind()) {
    case PROVIDES -> UniversalElement.of(((ProvidesDirective)this.delegate()).getService(), this.domain);
    case USES -> UniversalElement.of(((UsesDirective)this.delegate()).getService(), this.domain);
    case EXPORTS, OPENS, REQUIRES -> null;
    };
  }

  @Override // ExportsDirective, OpensDirective
  public final List<? extends UniversalElement> getTargetModules() {
    return switch (this.getKind()) {
    case EXPORTS -> UniversalElement.of(((ExportsDirective)this.delegate()).getTargetModules(), this.domain);
    case OPENS -> UniversalElement.of(((OpensDirective)this.delegate()).getTargetModules(), this.domain);
    default -> List.of();
    };
  }

  @Override // Object
  public final int hashCode() {
    return this.delegate().hashCode();
  }

  @Override // RequiresDirective
  public final boolean isStatic() {
    return switch (this.getKind()) {
    case REQUIRES -> ((RequiresDirective)this.delegate()).isStatic();
    case EXPORTS, OPENS, PROVIDES, USES -> false;
    };
  }

  @Override // RequiresDirective
  public final boolean isTransitive() {
    return switch (this.getKind()) {
    case REQUIRES -> ((RequiresDirective)this.delegate()).isTransitive();
    case EXPORTS, OPENS, PROVIDES, USES -> false;
    };
  }

  @Override // Object
  @SuppressWarnings("try")
  public final String toString() {
    try (var lock = this.domain.lock()) {
      return this.delegate().toString();
    }
  }


  /*
   * Static methods.
   */


  /**
   * Returns a {@link UniversalDirective} that is either the supplied {@link Directive} (if it itself is {@code null} or is
   * a {@link UniversalDirective}) or one that wraps it.
   *
   * @param d an {@link Directive}; may be {@code null} in which case {@code null} will be returned
   *
   * @param domain a {@link Domain}; must not be {@code null}
   *
   * @return a {@link UniversalDirective}, or {@code null} (if {@code e} is {@code null})
   *
   * @exception NullPointerException if {@code domain} is {@code null}
   *
   * @see #UniversalDirective(Directive, Domain)
   */
  public static final UniversalDirective of(final Directive d, final Domain domain) {
    return switch (d) {
    case null -> null;
    case UniversalDirective ud -> ud;
    default -> new UniversalDirective(d, domain);
    };
  }

  /**
   * Returns a non-{@code null}, immutable {@link List} of {@link UniversalDirective}s whose elements wrap the supplied
   * {@link List}'s elements.
   *
   * @param es a {@link Collection} of {@link Directive}s; must not be {@code null}
   *
   * @param domain a {@link Domain}; must not be {@code null}
   *
   * @return a non-{@code null}, immutable {@link List} of {@link UniversalDirective}s
   *
   * @exception NullPointerException if either argument is {@code null}
   */
  public static final List<? extends UniversalDirective> of(final Collection<? extends Directive> es, final Domain domain) {
    if (es.isEmpty()) {
      return List.of();
    }
    final List<UniversalDirective> newEs = new ArrayList<>(es.size());
    for (final Directive e : es) {
      newEs.add(of(e, domain));
    }
    return Collections.unmodifiableList(newEs);
  }

  /**
   * <dfn>Unwraps</dfn> the supplied {@link Directive} implementation such that the returned value is not an
   * instance of {@link UniversalDirective}.
   *
   * @param <T> a {@link Directive} subtype
   *
   * @param t a {@link Directive}; may be {@code null}
   *
   * @return an object of the appropriate type that is guaranteed not to be an instance of {@link UniversalDirective}
   *
   * @see #delegate()
   */
  @SuppressWarnings("unchecked")
  public static final <T extends Directive> T unwrap(T t) {
    while (t instanceof UniversalDirective ud) {
      t = (T)ud.delegate();
    }
    return t;
  }

}
