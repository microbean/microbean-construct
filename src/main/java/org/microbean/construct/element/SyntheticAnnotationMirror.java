/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2025 microBean™.
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

import java.lang.constant.ClassDesc;
import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodTypeDesc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import java.util.function.Function;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import org.microbean.construct.constant.Constables;

import static java.lang.constant.ConstantDescs.BSM_INVOKE;
import static java.lang.constant.ConstantDescs.CD_Map;
import static java.lang.constant.ConstantDescs.CD_Object;
import static java.lang.constant.ConstantDescs.NULL;

import static java.lang.constant.DirectMethodHandleDesc.Kind.INTERFACE_STATIC;

import static java.lang.constant.MethodHandleDesc.ofConstructor;

import static java.util.Arrays.fill;

import static java.util.Collections.unmodifiableMap;

import static java.util.LinkedHashMap.newLinkedHashMap;

import static java.util.Objects.requireNonNull;

import static javax.lang.model.element.ElementKind.ANNOTATION_TYPE;
import static javax.lang.model.element.ElementKind.METHOD;

/**
 * An <strong>experimental</strong> {@link AnnotationMirror} implementation that is partially or wholly synthetic.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 */
public final class SyntheticAnnotationMirror implements AnnotationMirror, Constable {

  private final TypeElement annotationTypeElement;

  private final Map<ExecutableElement, AnnotationValue> elementValues;

  /**
   * Creates a new {@link SyntheticAnnotationMirror}.
   *
   * @param annotationTypeElement a {@link TypeElement} representing an annotation type; must not be {@code null}; must
   * return {@link javax.lang.model.element.ElementKind#ANNOTATION_TYPE ANNOTATION_TYPE} from its {@link
   * Element#getKind() getKind()} method; {@link SyntheticAnnotationTypeElement} implementations are strongly preferred
   *
   * @param values a {@link Map} of annotation values indexed by annotation element name; must not be {@code null}; must
   * contain only values that are permissible for annotation elements
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @exception IllegalArgumentException if {@code annotationTypeElement} does not return {@link
   * javax.lang.model.element.ElementKind#ANNOTATION_TYPE ANNOTATION_TYPE} from an invocation of its {@link
   * Element#getKind() getKind()} method, or if {@code values} has more entries in it than {@code annotationTypeElement}
   * has {@linkplain Element#getEnclosedElements() anotation elements}
   */
  public SyntheticAnnotationMirror(final TypeElement annotationTypeElement,
                                   final Map<? extends String, ?> values) {
    super();
    if (annotationTypeElement.getKind() != ANNOTATION_TYPE) {
      throw new IllegalArgumentException("annotationTypeElement: " + annotationTypeElement);
    }
    this.annotationTypeElement = annotationTypeElement;
    final LinkedHashMap<ExecutableElement, AnnotationValue> m = newLinkedHashMap(values.size());
    for (final Element e : annotationTypeElement.getEnclosedElements()) {
      if (e.getKind() == METHOD) {
        final Object value = values.get(e.getSimpleName().toString());
        if (value != null) {
          m.put((ExecutableElement)e, value instanceof AnnotationValue av ? av : new SyntheticAnnotationValue(value));
        }
      }
    }
    if (values.size() > m.size()) {
      throw new IllegalArgumentException("values: " + values);
    }
    this.elementValues = unmodifiableMap(m);
  }


  /*
   * Instance methods.
   */

  @Override // Constable
  public final Optional<? extends ConstantDesc> describeConstable() {
    return this.annotationTypeElement instanceof Constable c ? c.describeConstable() : Optional.<ConstantDesc>empty()
      .flatMap(elementDesc -> Constables.describe(this.elementValues,
                                                  SyntheticAnnotationMirror::describeExecutableElement,
                                                  SyntheticAnnotationMirror::describeAnnotationValue)
               .map(valuesDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                         ofConstructor(ClassDesc.of(this.getClass().getName()),
                                                                       ClassDesc.of(TypeElement.class.getName()),
                                                                       CD_Map),
                                                         elementDesc,
                                                         valuesDesc)));
  }
  

  @Override // AnnotationMirror
  public final DeclaredType getAnnotationType() {
    return (DeclaredType)this.annotationTypeElement.asType();
  }

  @Override // AnnotationMirror
  public final Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValues() {
    return this.elementValues;
  }

  /**
   * An <strong>experimental</strong> {@link AnnotationValue} implementation that is partially or wholly synthetic.
   *
   * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
   */
  public static final class SyntheticAnnotationValue implements AnnotationValue, Constable {

    // Will be one of:
    //
    // * AnnotationMirror
    // * List<SyntheticAnnotationValue>
    // * TypeMirror
    // * VariableElement (ENUM_CONSTANT)
    // * Boolean
    // * Byte
    // * Character
    // * Double
    // * Float
    // * Integer
    // * Long
    // * Short
    // * String
    private final Object value;

    /**
     * Creates a new {@link SyntheticAnnotationValue}.
     *
     * @param value the value; must not be {@code null}; must be a legal {@link AnnotationValue} type
     *
     * @exception NullPointerException if {@code value} is {@code null}
     *
     * @exception IllegalArgumentException if {@code value} is not a legal {@link AnnotationValue} type
     *
     * @see AnnotationValue
     */
    public SyntheticAnnotationValue(final Object value) {
      super();
      this.value = value(value);
    }

    @Override // AnnotationValue
    @SuppressWarnings("unchecked")
    public final <R, P> R accept(final AnnotationValueVisitor<R, P> v, final P p) {
      return switch (this.getValue()) {
      case null               -> v.visitUnknown(this, p); // ...or AssertionError?
      case AnnotationMirror a -> v.visitAnnotation(a, p);
      case List<?> l          -> v.visitArray((List<? extends AnnotationValue>)l, p);
      case TypeMirror t       -> v.visitType(t, p);
      case VariableElement e  -> v.visitEnumConstant(e, p);
      case Boolean b          -> v.visitBoolean(b, p);
      case Byte b             -> v.visitByte(b, p);
      case Character c        -> v.visitChar(c, p);
      case Double d           -> v.visitDouble(d, p);
      case Float f            -> v.visitFloat(f, p);
      case Integer i          -> v.visitInt(i, p);
      case Long l             -> v.visitLong(l, p);
      case Short s            -> v.visitShort(s, p);
      case String s           -> v.visitString(s, p);
      default                 -> v.visitUnknown(this, p);
      };
    }

    @Override // Constable
    public final Optional<? extends ConstantDesc> describeConstable() {
      return this.value instanceof Constable c ? c.describeConstable() : Optional.<ConstantDesc>empty()
        .map(valueDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                 ofConstructor(ClassDesc.of(this.getClass().getName()),
                                                               CD_Object),
                                                 valueDesc));
    }

    @Override // Object
    public final boolean equals(final Object other) {
      return this == other || switch (other) {
      case null -> false;
      case SyntheticAnnotationValue sav when this.getClass() == sav.getClass() -> this.value.equals(sav.value);
      default -> false;
      };
    }

    @Override // AnnotationValue
    public final Object getValue() {
      return this.value;
    }

    @Override // Object
    public final int hashCode() {
      return this.value.hashCode();
    }

    @Override // Object
    public final String toString() {
      return this.value.toString();
    }

    private static final Object value(final Object value) {
      return switch (value) {
      case null               -> throw new NullPointerException("value");

      case AnnotationValue av -> av.getValue(); // not part of the spec; just good hygiene
      case List<?> l          -> l.stream().map(SyntheticAnnotationValue::new).toList();

      case TypeMirror t       -> switch (t.getKind()) {
      case BOOLEAN, BYTE, CHAR, DECLARED, DOUBLE, FLOAT, INT, LONG, SHORT, VOID /* I think? */ -> t;
      default -> throw new IllegalArgumentException("value: " + value);
      };

      case VariableElement e  -> switch (e.getKind()) {
      case ENUM_CONSTANT -> e;
      default -> throw new IllegalArgumentException("value: " + value);
      };

      case AnnotationMirror a -> a;
      case Boolean b          -> b;
      case Byte b             -> b;
      case Character c        -> c;
      case Double d           -> d;
      case Float f            -> f;
      case Integer i          -> i;
      case Long l             -> l;
      case Short s            -> s;
      case String s           -> s;
      default                 -> throw new IllegalArgumentException("value: " + value);
      };
    }

  }

  private static final Optional<? extends ConstantDesc> describeAnnotationValue(final Object v) {
    return switch (v) {
    case null -> Optional.empty(); // deliberately not Optional.of(NULL); annotation values cannot be null
    case Constable c -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd);
    case List<?> l -> Constables.describe(l, e -> e instanceof Constable c ? c.describeConstable() : Optional.empty());
    default -> Optional.empty();
    };
  }

  private static final Optional<? extends ConstantDesc> describeExecutableElement(final ExecutableElement e) {
    return switch (e) {
    case null -> throw new IllegalStateException();
    case Constable c -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd);
    default -> Optional.empty();
    };
  }

}
