/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2026 microBean™.
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

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;

import java.util.function.Predicate;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;

import javax.lang.model.util.AbstractAnnotationValueVisitor14;

import static javax.lang.model.element.ElementKind.ENUM;
import static javax.lang.model.element.ElementKind.ENUM_CONSTANT;

import static javax.lang.model.type.TypeKind.ARRAY;
import static javax.lang.model.type.TypeKind.DECLARED;
import static javax.lang.model.type.TypeKind.VOID;

import static org.microbean.construct.element.AnnotationMirrors.allAnnotationValues;

/**
 * An {@link AbstractAnnotationValueVisitor14} that determines if the otherwise opaque values {@linkplain
 * AnnotationValue#getValue() represented} by two {@link AnnotationValue} implementations are to be considered the
 * <dfn>same</dfn>.
 *
 * <p>This class implements the rules described by the {@link java.lang.annotation.Annotation#equals(Object)} contract,
 * for want of a more authoritative source. This contract appears to define in a mostly agnostic manner what it means
 * for two annotations to be "the same".</p>
 *
 * <p>Unlike some other annotation-processing-related facilities, the relation represented by this {@link
 * SameAnnotationValueVisitor} does not require that the values being logically compared originate from {@link
 * AnnotationValue} instances from the same vendor or toolkit.</p>
 *
 * <p>The second argument passed to {@link #visit(AnnotationValue, Object)} is expected to be either {@code null}, an
 * {@link AnnotationValue}, or the result of an invocation of an {@link AnnotationValue}'s {@link
 * AnnotationValue#getValue() getValue()} method.</p>
 *
 * <p>Any two {@link TypeElement}s encountered during traversal are considered equal if their {@linkplain
 * TypeElement#getQualifiedName() qualified names} have {@linkplain
 * javax.lang.model.element.Name#contentEquals(CharSequence) equal contents}.</p>
 *
 * <p>Any two {@link VariableElement}s representing enum constants encountered during traversal are considered equal if
 * they belong to the same enum class and their {@linkplain VariableElement#getSimpleName() simple names} have
 * {@linkplain javax.lang.model.element.Name#contentEquals(CharSequence) equal contents}.</p>
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see AnnotationValue#accept(javax.lang.model.element.AnnotationValueVisitor, Object)
 *
 * @see AnnotationMirrors#allAnnotationValues(AnnotationMirror)
 *
 * @see AnnotationValueHashcodeVisitor
 *
 * @see java.lang.annotation.Annotation#equals(Object)
 */
public final class SameAnnotationValueVisitor extends AbstractAnnotationValueVisitor14<Boolean, Object> {



  /*
   * Instance fields.
   */


  private final Predicate<? super ExecutableElement> p;



  /*
   * Constructors.
   */


  /**
   * Creates a new {@link SameAnnotationValueVisitor}.
   *
   * @see #SameAnnotationValueVisitor(Predicate)
   */
  public SameAnnotationValueVisitor() {
    this(null);
  }

  /**
   * Creates a new {@link SameAnnotationValueVisitor}.
   *
   * @param p a {@link Predicate} that returns {@code true} if a given {@link ExecutableElement}, representing an
   * annotation interface element, is to be included in the computation; may be {@code null} in which case it is as if
   * {@code ()-> true} were supplied instead
   */
  public SameAnnotationValueVisitor(final Predicate<? super ExecutableElement> p) {
    super();
    this.p = p == null ? ee -> true : p;
  }


  /*
   * Instance methods.
   */


  @Override // AbstractAnnotationValueVisitor14
  public final Boolean visitAnnotation(final AnnotationMirror am0, final Object v1) {
    return am0 == v1 || am0 != null && switch (v1) {
    case null -> false;
    case AnnotationValue av1 -> this.visitAnnotation(am0, av1.getValue());
    case AnnotationMirror am1 -> {
      if (!((QualifiedNameable)am0.getAnnotationType().asElement()).getQualifiedName().contentEquals(((QualifiedNameable)am1.getAnnotationType().asElement()).getQualifiedName())) {
        yield false;
      }
      final Iterator<Entry<ExecutableElement, AnnotationValue>> i0 = allAnnotationValues(am0).entrySet().iterator();
      final Iterator<Entry<ExecutableElement, AnnotationValue>> i1 = allAnnotationValues(am1).entrySet().iterator();
      while (i0.hasNext()) {
        if (!i1.hasNext()) {
          yield false;
        }
        final Entry<ExecutableElement, AnnotationValue> e0 = i0.next();
        final Entry<ExecutableElement, AnnotationValue> e1 = i1.next();
        final ExecutableElement ee0 = e0.getKey();
        if (!ee0.getSimpleName().contentEquals(e1.getKey().getSimpleName()) ||
            this.p.test(ee0) && !this.visit(e0.getValue(), e1.getValue().getValue())) {
          yield false;
        }
      }
      yield !i1.hasNext();
    }
    default -> false;
    };
  }

  @Override // AbstractAnnotationValueVisitor14
  public final Boolean visitArray(final List<? extends AnnotationValue> l0, final Object v1) {
    return l0 == v1 || l0 != null && switch (v1) {
    case null -> false;
    case AnnotationValue av1 -> this.visitArray(l0, av1.getValue());
    case List<?> l1 -> {
      final int size = l0.size();
      if (size != l1.size()) {
        yield false;
      }
      for (int i = 0; i < size; i++) {
        // Yes, order is important (!)
        if (!(l1.get(i) instanceof AnnotationValue av1) || !this.visit(av1, l0.get(i).getValue())) {
          yield false;
        }
      }
      yield true;
    }
    default -> false;
    };
  }

  @Override // AbstractAnnotationValueVisitor14
  public final Boolean visitBoolean(final boolean b0, final Object v1) {
    return switch (v1) {
    case null -> false;
    case AnnotationValue av1 -> this.visitBoolean(b0, av1.getValue());
    case Boolean b1 -> b0 && b1.booleanValue();
    default -> false;
    };
  }

  @Override // AbstractAnnotationValueVisitor14
  public final Boolean visitByte(final byte b0, final Object v1) {
    return switch (v1) {
    case null -> false;
    case AnnotationValue av1 -> this.visitByte(b0, av1.getValue());
    case Byte b1 -> b0 == b1.byteValue();
    default -> false;
    };
  }

  @Override // AbstractAnnotationValueVisitor14
  public final Boolean visitChar(final char c0, final Object v1) {
    return switch (v1) {
    case null -> false;
    case AnnotationValue av1 -> this.visitChar(c0, av1.getValue());
    case Character c1 -> c0 == c1.charValue();
    default -> false;
    };
  }

  @Override // AbstractAnnotationValueVisitor14
  public final Boolean visitDouble(final double d0, final Object v1) {
    return v1 instanceof AnnotationValue av1 ? this.visitDouble(d0, av1.getValue()) : Double.valueOf(d0).equals(v1);
  }

  @Override // AbstractAnnotationValueVisitor14
  public final Boolean visitEnumConstant(final VariableElement ve0, final Object v1) {
    return ve0 == v1 || ve0!= null && switch (v1) {
    case null -> false;
    case AnnotationValue av1 -> this.visitEnumConstant(ve0, av1.getValue());
    case VariableElement ve1 when ve0.getKind() == ENUM_CONSTANT && ve1.getKind() == ENUM_CONSTANT -> {
      final TypeElement te0 = (TypeElement)ve0.getEnclosingElement();
      final TypeElement te1 = (TypeElement)ve1.getEnclosingElement();
      yield switch (te0.getKind()) {
      case ENUM -> te1.getKind() == ENUM && ve0.getSimpleName().contentEquals(ve1.getSimpleName()) && te0.getQualifiedName().contentEquals(te1.getQualifiedName());
      default -> false;
      };
    }
    default -> false;
    };
  }

  @Override // AbstractAnnotationValueVisitor14
  public final Boolean visitFloat(final float f0, final Object v1) {
    return v1 instanceof AnnotationValue av1 ? this.visitFloat(f0, av1.getValue()) : Float.valueOf(f0).equals(v1);
  }

  @Override // AbstractAnnotationValueVisitor14
  public final Boolean visitInt(final int i0, final Object v1) {
    return switch (v1) {
    case null -> false;
    case AnnotationValue av1 -> this.visitInt(i0, av1.getValue());
    case Integer i1 -> i0 == i1.intValue();
    default -> false;
    };
  }

  @Override // AbstractAnnotationValueVisitor14
  public final Boolean visitLong(final long l0, final Object v1) {
    return switch (v1) {
    case null -> false;
    case AnnotationValue av1 -> this.visitLong(l0, av1.getValue());
    case Long l1 -> l0 == l1.longValue();
    default -> false;
    };
  }

  @Override // AbstractAnnotationValueVisitor14
  public final Boolean visitShort(final short s0, final Object v1) {
    return switch (v1) {
    case null -> false;
    case AnnotationValue av1 -> this.visitShort(s0, av1.getValue());
    case Short s1 -> s0 == s1.shortValue();
    default -> false;
    };
  }

  @Override // AbstractAnnotationValueVisitor14
  public final Boolean visitString(final String s0, final Object v1) {
    return v1 instanceof AnnotationValue av1 ? this.visitString(s0, av1.getValue()) : Objects.equals(s0, v1);
  }

  @Override // AbstractAnnotationValueVisitor14
  public final Boolean visitType(final TypeMirror t0, final Object v1) {
    return t0 == v1 || v1 != null && switch (t0) {
    case null -> false;
    case ArrayType a0 when a0.getKind() == ARRAY -> this.privateVisitArrayType(a0, v1); // e.g. Object[].class
    case DeclaredType dt0 when dt0.getKind() == DECLARED -> this.privateVisitDeclaredType(dt0, v1); // e.g. Foo.class
    case PrimitiveType p0 when p0.getKind().isPrimitive() -> this.privateVisitPrimitiveType(p0, v1); // e.g. int.class
    case NoType n0 when n0.getKind() == VOID -> this.privateVisitNoType(n0, v1); // e.g. void.class
    default -> t0.equals(v1);
    };
  }


  /*
   * Private instance methods.
   */


  private final Boolean privateVisitArrayType(final ArrayType a0, final Object v1) {
    assert a0.getKind() == ARRAY;
    return switch (v1) {
    case AnnotationValue av1 -> this.privateVisitArrayType(a0, av1.getValue());
    case ArrayType a1 when a1.getKind() == ARRAY -> this.visitType(a0.getComponentType(), a1.getComponentType());
    default -> false;
    };
  }

  private final Boolean privateVisitDeclaredType(final DeclaredType dt0, final Object v1) {
    assert dt0.getKind() == DECLARED;
    return switch (v1) {
    case AnnotationValue av1 -> this.privateVisitDeclaredType(dt0, av1.getValue());
    case DeclaredType dt1 when dt1.getKind() == DECLARED -> ((QualifiedNameable)dt0.asElement()).getQualifiedName().contentEquals((((QualifiedNameable)dt1.asElement()).getQualifiedName()));
    default -> false;
    };
  }

  private final Boolean privateVisitNoType(final NoType n0, final Object v1) {
    assert n0.getKind() == VOID;
    return switch (v1) {
    case AnnotationValue av1 -> this.privateVisitNoType(n0, av1.getValue());
    case NoType n1 -> n1.getKind() == VOID;
    default -> false;
    };
  }

  private final Boolean privateVisitPrimitiveType(final PrimitiveType p0, final Object v1) {
    assert p0.getKind().isPrimitive();
    return switch (v1) {
    case AnnotationValue av1 -> this.privateVisitPrimitiveType(p0, av1.getValue());
    case PrimitiveType p1 -> p1.getKind() == p0.getKind();
    default -> false;
    };
  }

}
