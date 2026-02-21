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

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Queue;
import java.util.SequencedMap;
import java.util.Set;

import java.util.function.Predicate;

import java.util.stream.Stream;

import javax.lang.model.AnnotatedConstruct;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSequencedMap;
import static java.util.Collections.unmodifiableSet;

import static java.util.LinkedHashMap.newLinkedHashMap;

import static java.util.HashSet.newHashSet;

import static java.util.function.Function.identity;

import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.iterate;

import static javax.lang.model.element.ElementKind.ANNOTATION_TYPE;
import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.ElementKind.METHOD;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;

import static javax.lang.model.type.TypeKind.ARRAY;
import static javax.lang.model.type.TypeKind.DECLARED;

import static javax.lang.model.util.ElementFilter.methodsIn;

/**
 * A utility class for working with annotations as represented by {@link AnnotationMirror}s, {@link ExecutableElement}s,
 * and {@link AnnotationValue}s.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 */
public final class AnnotationMirrors {

  private static final Set<ElementType> EMPTY_ELEMENT_TYPES = unmodifiableSet(EnumSet.noneOf(ElementType.class));

  private static final SequencedMap<?, ?> EMPTY_MAP = unmodifiableSequencedMap(newLinkedHashMap(0));

  private AnnotationMirrors() {
    super();
  }

  /**
   * Returns a determinate, non-{@code null}, immutable {@link List} of {@link AnnotationMirror}s <dfn>present</dfn> on
   * the supplied {@link Element}.
   *
   * <p>This method is a more capable, better-typed replacement of the {@link
   * javax.lang.model.util.Elements#getAllAnnotationMirrors(Element)} method, and should be preferred.</p>
   *
   * @param e an {@link Element}; must not be {@code null}
   *
   * @return a determinate, non-{@code null}, immutable {@link List} of {@link AnnotationMirror}s <dfn>present</dfn> on
   * the supplied {@link Element}
   *
   * @exception NullPointerException if {@code e} is {@code null}
   *
   * @see javax.lang.model.util.Elements#getAllAnnotationMirrors(Element)
   */
  public static final List<? extends AnnotationMirror> allAnnotationMirrors(Element e) {
    final List<AnnotationMirror> allAnnotations = new LinkedList<>(e.getAnnotationMirrors());
    WHILE_LOOP:
    while (e.getKind() == CLASS && e instanceof TypeElement te) {
      final TypeMirror sct = te.getSuperclass();
      if (sct.getKind() != DECLARED || !(sct instanceof DeclaredType)) {
        break;
      }
      e = ((DeclaredType)sct).asElement();
      final List<? extends AnnotationMirror> superclassAnnotations = e.getAnnotationMirrors();
      if (!superclassAnnotations.isEmpty()) {
        int added = 0;
        for (final AnnotationMirror superclassAnnotation : superclassAnnotations) {
          if (inherited(superclassAnnotation)) {
            for (final AnnotationMirror a : allAnnotations.subList(added, allAnnotations.size())) {
              if (((QualifiedNameable)superclassAnnotation.getAnnotationType().asElement()).getQualifiedName().contentEquals(((QualifiedNameable)a.getAnnotationType().asElement()).getQualifiedName())) {
                continue WHILE_LOOP;
              }
            }
            // javac prepends superclass annotations, resulting in a strage order; we duplicate it
            allAnnotations.addFirst(superclassAnnotation);
            ++added;
          }
        }
      }
    }
    return allAnnotations.isEmpty() ? List.of() : unmodifiableList(allAnnotations);
  }

  /**
   * For the supplied {@link AnnotationMirror}, returns an immutable, determinate {@link SequencedMap} of {@link
   * AnnotationValue} instances indexed by the {@link ExecutableElement}s to which they apply.
   *
   * <p>Each {@link ExecutableElement} represents an <a
   * href="https://docs.oracle.com/javase/specs/jls/se25/html/jls-9.html#jls-9.6.1">annotation interface element</a> and
   * meets the requirements of such an element.</p>
   *
   * <p>Each {@link AnnotationValue} represents the value of an annotation interface element and meets the requirements
   * of such a value.</p>
   *
   * <p>This method is a more capable, better-typed replacement of the {@link
   * javax.lang.model.util.Elements#getElementValuesWithDefaults(AnnotationMirror)} method, and should be preferred.</p>
   *
   * @param a an {@link AnnotationMirror}; may be {@code null} in which case an empty, immutable, determinate {@link
   * SequencedMap} will be returned
   *
   * @return an immutable, determinate {@link Map} of {@link AnnotationValue} instances indexed by {@link
   * ExecutableElement}s
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se25/html/jls-9.html#jls-9.6.1 Java Language Specification, section
   * 9.6.1
   *
   * @see javax.lang.model.util.Elements#getElementValuesWithDefaults(AnnotationMirror)
   */
  public static final SequencedMap<ExecutableElement, AnnotationValue> allAnnotationValues(final AnnotationMirror a) {
    if (a == null) {
      return emptySequencedMap();
    }
    final Collection<? extends ExecutableElement> elements = methodsIn(a.getAnnotationType().asElement().getEnclosedElements());
    if (elements.isEmpty()) {
      return emptySequencedMap();
    }
    final SequencedMap<ExecutableElement, AnnotationValue> m = newLinkedHashMap(elements.size());
    final Map<? extends ExecutableElement, ? extends AnnotationValue> explicitValues = a.getElementValues();
    for (final ExecutableElement ee : elements) {
      // You're going to want to use getOrDefault() here. Go ahead and try but you'll run into typing issues.
      m.put(ee, explicitValues.containsKey(ee) ? explicitValues.get(ee) : ee.getDefaultValue());
    }
    return m.isEmpty() ? emptySequencedMap() : unmodifiableSequencedMap(m);
  }

  /**
   * Returns {@code true} if and only if the supplied {@link Collection} of {@link AnnotationMirror}s contains an {@link
   * AnnotationMirror} that is {@linkplain #sameAnnotation(AnnotationMirror, AnnotationMirror, Predicate) the same} as
   * the supplied {@link AnnotationMirror}.
   *
   * @param c a non-{@code null} {@link Collection} of {@link AnnotationMirror}s
   *
   * @param a a non-{@code null} {@link AnnotationMirror}
   *
   * @return {@code true} if and only if the supplied {@link Collection} of {@link AnnotationMirror}s contains an {@link
   * AnnotationMirror} that is {@linkplain #sameAnnotation(AnnotationMirror, AnnotationMirror, Predicate) the same} as
   * the supplied {@link AnnotationMirror}
   *
   * @exception NullPointerException if {@code c} or {@code a} is {@code null}
   *
   * @see #contains(collection, AnnotationMirror, Predicate)
   */
  public static final boolean contains(final Collection<? extends AnnotationMirror> c,
                                       final AnnotationMirror a) {
    return contains(c, a, null);
  }

  /**
   * Returns {@code true} if and only if the supplied {@link Collection} of {@link AnnotationMirror}s contains an {@link
   * AnnotationMirror} that is {@linkplain #sameAnnotation(AnnotationMirror, AnnotationMirror, Predicate) the same} as
   * the supplied {@link AnnotationMirror}.
   *
   * @param c a non-{@code null} {@link Collection} of {@link AnnotationMirror}s
   *
   * @param a a non-{@code null} {@link AnnotationMirror}
   *
   * @param p a {@link Predicate} that returns {@code true} if a given {@link ExecutableElement}, representing an
   * annotation element, is to be included in comparison operations; may be {@code null} in which case it is as if
   * {@code e -> true} were supplied instead
   *
   * @return {@code true} if and only if the supplied {@link Collection} of {@link AnnotationMirror}s contains an {@link
   * AnnotationMirror} that is {@linkplain #sameAnnotation(AnnotationMirror, AnnotationMirror, Predicate) the same} as
   * the supplied {@link AnnotationMirror}
   *
   * @exception NullPointerException if {@code c} or {@code a} is {@code null}
   *
   * @see #sameAnnotation(AnnotationMirror, AnnotationMirror, Predicate)
   *
   * @see #containsAll(collection, Collection, Predicate)
   */
  public static final boolean contains(final Collection<? extends AnnotationMirror> c,
                                       final AnnotationMirror a,
                                       final Predicate<? super ExecutableElement> p) {
    for (final AnnotationMirror ca : c) {
      if (sameAnnotation(ca, a, p)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns {@code true} if and only if {@code c0} contains all {@linkplain #sameAnnotation(AnnotationMirror,
   * AnnotationMirror, Predicate) the same} {@link AnnotationMirror}s as are found in {@code c1},
   *
   * @param c0 a non-{@code null} {@link Collection} of {@link AnnotationMirror}s
   *
   * @param c1 a non-{@code null} {@link Collection} of {@link AnnotationMirror}s
   *
   * @return {@code true} if and only if {@code c0} contains all {@linkplain #sameAnnotation(AnnotationMirror,
   * AnnotationMirror, Predicate) the same} {@link AnnotationMirror}s as are found in {@code c1}
   *
   * @exception NullPointerException if either {@code c0} or {@code c1} is {@code null}
   *
   * @see #containsAll(Collection, Collection, Predicate)
   */
  public static final boolean containsAll(final Collection<? extends AnnotationMirror> c0,
                                          final Collection<? extends AnnotationMirror> c1) {
    return containsAll(c0, c1, null);
  }

  /**
   * Returns {@code true} if and only if {@code c0} contains all {@linkplain #sameAnnotation(AnnotationMirror,
   * AnnotationMirror, Predicate) the same} {@link AnnotationMirror}s as are found in {@code c1},
   *
   * @param c0 a non-{@code null} {@link Collection} of {@link AnnotationMirror}s
   *
   * @param c1 a non-{@code null} {@link Collection} of {@link AnnotationMirror}s
   *
   * @param p a {@link Predicate} that returns {@code true} if a given {@link ExecutableElement}, representing an
   * annotation element, is to be included in comparison operations; may be {@code null} in which case it is as if
   * {@code e -> true} were supplied instead
   *
   * @return {@code true} if and only if {@code c0} contains all {@linkplain #sameAnnotation(AnnotationMirror,
   * AnnotationMirror, Predicate) the same} {@link AnnotationMirror}s as are found in {@code c1}
   *
   * @exception NullPointerException if either {@code c0} or {@code c1} is {@code null}
   *
   * @see #contains(Collection, AnnotationMirror, Predicate)
   *
   * @see #sameAnnotation(AnnotationMirror, AnnotationMirror, Predicate)
   */
  public static final boolean containsAll(final Collection<? extends AnnotationMirror> c0,
                                          final Collection<? extends AnnotationMirror> c1,
                                          final Predicate<? super ExecutableElement> p) {
    if (c0.size() < c1.size()) {
      return false;
    }
    for (final AnnotationMirror a1 : c1) {
      if (!contains(c0, a1, p)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns the (determinate) {@link AnnotationMirror} <em>directly present</em> on the supplied {@link
   * AnnotatedConstruct} whose {@linkplain AnnotationMirror#getAnnotationType() annotation type}'s {@link
   * javax.lang.model.type.DeclaredType#asElement() TypeElement} {@linkplain TypeElement#getQualifiedName() bears} the
   * supplied {@code fullyQualifiedName}, or {@code null} if no such {@link AnnotationMirror} exists.
   *
   * @param ac an {@link AnnotatedConstruct}; must not be {@code null}
   *
   * @param name a {@link CharSequence}; may be {@code null} in which case {@code null} will be returned
   *
   * @return an {@link AnnotationMirror}, or {@code null}
   *
   * @exception NullPointerException if {@code ac} is {@code null}
   *
   * @see #streamBreadthFirst(AnnotatedConstruct)
   *
   * @see #streamDepthFirst(AnnotatedConstruct)
   *
   * @see AnnotationMirror#getAnnotationType()
   *
   * @see javax.lang.model.type.DeclaredType#asElement()
   *
   * @see TypeElement#getQualifiedName()
   *
   * @see javax.lang.model.element.Name#contentEquals(CharSequence)
   */
  public static final AnnotationMirror get(final AnnotatedConstruct ac, final CharSequence name) {
    if (name == null) {
      return null;
    }
    for (final AnnotationMirror am : ac.getAnnotationMirrors()) {
      final TypeElement e = (TypeElement)am.getAnnotationType().asElement();
      if (e.getQualifiedName().contentEquals(name)) {
        return am;
      }
    }
    return null;
  }

  /**
   * A convenience method that returns a value for an annotation interface element {@linkplain
   * ExecutableElement#getSimpleName() named} by the supplied {@link CharSequence} and logically owned by the supplied
   * {@link AnnotationMirror}, or {@code null} if no such value exists.
   *
   * @param am an {@link AnnotationMirror}; must not be {@code null}
   *
   * @param name a {@link CharSequence}; may be {@code null} in which case {@code null} will be returned
   *
   * @return the result of invoking {@link AnnotationValue#getValue() getValue()} on an {@link AnnotationValue}, or
   * {@code null}
   *
   * @exception NullPointerException if {@code am} is {@code null}
   *
   * @see AnnotationValue
   *
   * @see #allAnnotationValues(AnnotationMirror)
   *
   * @see #get(Map, CharSequence)
   */
  public static final Object get(final AnnotationMirror am, final CharSequence name) {
    return name == null ? null : get(allAnnotationValues(am), name);
  }

  /**
   * A convenience method that returns a value for an annotation interface element {@linkplain
   * ExecutableElement#getSimpleName() named} by the supplied {@link CharSequence} and found in the supplied {@link
   * Map}, or {@code null} if no such value exists.
   *
   * @param values a {@link Map} as returned by the {@link #allAnnotationValues(AnnotationMirror)} method; must not be
   * {@code null}
   *
   * @param name a {@link CharSequence}; may be {@code null} in which case {@code null} will be returned
   *
   * @return the result of invoking {@link AnnotationValue#getValue() getValue()} on a suitable {@link AnnotationValue}
   * found in the supplied {@link Map}, or {@code null}
   *
   * @exception NullPointerException if {@code values} is {@code null}
   *
   * @see AnnotationValue
   *
   * @see #allAnnotationValues(AnnotationMirror)
   */
  public static final Object get(final Map<? extends ExecutableElement, ? extends AnnotationValue> values,
                                 final CharSequence name) {
    if (name == null) {
      return null;
    }
    for (final Entry<? extends Element, ? extends AnnotationValue> e : values.entrySet()) {
      if (e.getKey().getSimpleName().contentEquals(name)) {
        final AnnotationValue av = e.getValue();
        return av == null ? null : av.getValue();
      }
    }
    return null;
  }

  /**
   * An <strong>experimental</strong> method that returns a hashcode for an {@link AnnotationMirror} according as much
   * as possible to the rules described in the {@link java.lang.annotation.Annotation#hashCode()} contract.
   *
   * @param a an {@link AnnotationMirror} for which a hashcode should be computed; may be {@code null} in which case
   * {@code 0} will be returned
   *
   * @return a hashcode for the supplied {@link AnnotationMirror} computed according to the rules described in the
   * {@link java.lang.annotation.Annotation#hashCode()} contract
   *
   * @see #hashCode(AnnotationMirror, Predicate)
   *
   * @see java.lang.annotation.Annotation#hashCode()
   */
  public static final int hashCode(final AnnotationMirror a) {
    return hashCode(a, null);
  }

  /**
   * An <strong>experimental</strong> method that returns a hashcode for an {@link AnnotationMirror} according as much
   * as possible to the rules described in the {@link java.lang.annotation.Annotation#hashCode()} contract.
   *
   * @param a an {@link AnnotationMirror} for which a hashcode should be computed; may be {@code null} in which case
   * {@code 0} will be returned
   *
   * @param p a {@link Predicate} that accepts a (non-{@code null}) {@link ExecutableElement} representing an annotation
   * element and returns {@code true} if and only if the element should be included; may be {@code null} in which case a
   * {@link Predicate} semantically identical to {@code ee -> true} will be used instead
   *
   * @return a hashcode for the supplied {@link AnnotationMirror} computed according to the rules described in the
   * {@link java.lang.annotation.Annotation#hashCode()} contract
   *
   * @see java.lang.annotation.Annotation#hashCode()
   */
  public static final int hashCode(final AnnotationMirror a, final Predicate<? super ExecutableElement> p) {
    return a == null ? 0 : new AnnotationValueHashcodeVisitor().visitAnnotation(a, p == null ? ee -> true : p).intValue();
  }

  /**
   * Returns {@code true} if and only if the supplied {@link AnnotationMirror}'s {@linkplain
   * AnnotationMirror#getAnnotationType() annotation type} is {@linkplain javax.lang.model.type.DeclaredType#asElement()
   * declared by} an element that has been (meta-) {@linkplain Element#getAnnotationMirrors() annotated} with {@link
   * java.lang.annotation.Inherited}.
   *
   * @param a an {@link AnnotationMirror}; must not be {@code null}
   *
   * @return {@code true} if and only if the supplied {@link AnnotationMirror}'s {@linkplain
   * AnnotationMirror#getAnnotationType() annotation type} is {@linkplain javax.lang.model.type.DeclaredType#asElement()
   * declared by} an element that has been (meta-) {@linkplain Element#getAnnotationMirrors() annotated} with {@link
   * java.lang.annotation.Inherited}
   *
   * @exception NullPointerException if {@code a} is {@code null}
   *
   * @see #inherited(TypeElement)
   */
  public static final boolean inherited(final AnnotationMirror a) {
    return inherited((TypeElement)a.getAnnotationType().asElement());
  }

  /**
   * Returns {@code true} if and only if the supplied {@link TypeElement} represents an {@linkplain
   * javax.lang.model.element.ElementKind#ANNOTATION_TYPE annotation interface} and if it has been (meta-) annotated
   * with {@link java.lang.annotation.Inherited}.
   *
   * @param annotationInterface a {@link TypeElement} representing an {@linkplain
   * javax.lang.model.element.ElementKind#ANNOTATION_TYPE annotation interface}; must not be {@code null}
   *
   * @return {@code true} if and only if the supplied {@link TypeElement} represents an {@linkplain
   * javax.lang.model.element.ElementKind#ANNOTATION_TYPE annotation interface} and if it has been (meta-) annotated
   * with {@link java.lang.annotation.Inherited}
   *
   * @exception NullPointerException if {@code annotationInterface} is {@code null}
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se25/html/jls-9.html#jls-9.6.4.3 Java Language Specification,
   * section 9.6.4.3
   */
  public static final boolean inherited(final TypeElement annotationInterface) {
    if (annotationInterface.getKind() == ANNOTATION_TYPE) {
      for (final AnnotationMirror ma : annotationInterface.getAnnotationMirrors()) {
        if (((QualifiedNameable)ma.getAnnotationType().asElement()).getQualifiedName().contentEquals("java.lang.annotation.Inherited")) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns a {@link RetentionPolicy} for the supplied {@link AnnotationMirror}, or {@link RetentionPolicy#CLASS} if,
   * for any reason, a retention policy cannot be found or computed.
   *
   * @param a an {@link AnnotationMirror}; must not be {@code null}
   *
   * @return the {@link RetentionPolicy} for the supplied {@link AnnotationMirror}; never {@code null}
   *
   * @exception NullPointerException if {@code a} is {@code null}
   *
   * @see #retentionPolicy(TypeElement)
   */
  public static final RetentionPolicy retentionPolicy(final AnnotationMirror a) {
    return retentionPolicy((TypeElement)a.getAnnotationType().asElement());
  }

  /**
   * Returns a {@link RetentionPolicy} for the supplied {@link TypeElement} representing an {@linkplain
   * javax.lang.model.element.ElementKind#ANNOTATION_TYPE annotation interface}, or {@link RetentionPolicy#CLASS} if,
   * for any reason, a retention policy cannot be found or computed.
   *
   * @param annotationInterface a {@link TypeElement}; must be non-{@code null} and should represent an {@linkplain
   * javax.lang.model.element.ElementKind#ANNOTATION_TYPE annotation interface}
   *
   * @return the {@link RetentionPolicy} for the supplied {@link TypeElement}; never {@code null}
   *
   * @exception NullPointerException if {@code annotationInterface} is {@code null}
   *
   * @see RetentionPolicy
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se25/html/jls-9.html#jls-9.6.4.2 Java Language Specification,
   * section 9.6.4.2
   */
  public static final RetentionPolicy retentionPolicy(final TypeElement annotationInterface) {
    if (annotationInterface.getKind() == ANNOTATION_TYPE) {
      for (final AnnotationMirror ma : annotationInterface.getAnnotationMirrors()) {
        if (((QualifiedNameable)ma.getAnnotationType().asElement()).getQualifiedName().contentEquals("java.lang.annotation.Retention")) {
          return RetentionPolicy.valueOf(((VariableElement)get(ma, "value")).getSimpleName().toString());
        }
      }
    }
    return RetentionPolicy.CLASS;
  }

  /**
   * Determines whether the two {@link AnnotationMirror}s represent the same (otherwise opaque) annotation.
   *
   * @param am0 an {@link AnnotationMirror}; may be {@code null}
   *
   * @param am1 an {@link AnnotationMirror}; may be {@code null}
   *
   * @return {@code true} if the supplied {@link AnnotationMirror}s represent the same (otherwise opaque) annotation;
   * {@code false} otherwise
   *
   * @see #sameAnnotation(AnnotationMirror, AnnotationMirror, Predicate)
   */
  public static final boolean sameAnnotation(final AnnotationMirror am0, final AnnotationMirror am1) {
    return sameAnnotation(am0, am1, null);
  }

  /**
   * Determines whether the two {@link AnnotationMirror}s represent the same (underlying, otherwise opaque) annotation.
   *
   * @param am0 an {@link AnnotationMirror}; may be {@code null}
   *
   * @param am1 an {@link AnnotationMirror}; may be {@code null}
   *
   * @param p a {@link Predicate} that returns {@code true} if a given {@link ExecutableElement}, representing an
   * annotation interface element, is to be included in the computation; may be {@code null} in which case it is as if
   * {@code e -> true} were supplied instead
   *
   * @return {@code true} if the supplied {@link AnnotationMirror}s represent the same (underlying, otherwise opaque)
   * annotation; {@code false} otherwise
   *
   * @see SameAnnotationValueVisitor
   *
   * @see SameAnnotationValueVisitor#visitAnnotation(AnnotationMirror, Object)
   *
   * @see #allAnnotationValues(AnnotationMirror)
   */
  public static final boolean sameAnnotation(final AnnotationMirror am0,
                                             final AnnotationMirror am1,
                                             final Predicate<? super ExecutableElement> p) {
    return am0 == am1 || new SameAnnotationValueVisitor(p).visitAnnotation(am0, am1);
  }

  /**
   * Returns {@code true} if {@code c0} has all the {@linkplain #sameAnnotation(AnnotationMirror, AnnotationMirror,
   * Predicate) same annotations} as {@code c1}, and if {@code c1} has all the {@linkplain
   * #sameAnnotation(AnnotationMirror, AnnotationMirror, Predicate) same annotations} as {@code c0}.
   *
   * @param c0 a non-{@code null} {@link Collection} of {@link AnnotationMirror}s
   *
   * @param c1 a non-{@code null} {@link Collection} of {@link AnnotationMirror}s
   *
   * @return {@code true} if {@code c0} has all the {@linkplain #sameAnnotation(AnnotationMirror, AnnotationMirror,
   * Predicate) same annotations} as {@code c1}, and if {@code c1} has all the {@linkplain
   * #sameAnnotation(AnnotationMirror, AnnotationMirror, Predicate) same annotations} as {@code c0}
   *
   * @exception NullPointerException if either {@code c0} or {@code c1} is {@code null}
   *
   * @see #sameAnnotations(Collection, Collection, Predicate)
   */
  public static final boolean sameAnnotations(final Collection<? extends AnnotationMirror> c0,
                                              final Collection<? extends AnnotationMirror> c1) {
    return sameAnnotations(c0, c1, null);
  }

  /**
   * Returns {@code true} if {@code c0} has all the {@linkplain #sameAnnotation(AnnotationMirror, AnnotationMirror,
   * Predicate) same annotations} as {@code c1}, and if {@code c1} has all the {@linkplain
   * #sameAnnotation(AnnotationMirror, AnnotationMirror, Predicate) same annotations} as {@code c0}.
   *
   * @param c0 a non-{@code null} {@link Collection} of {@link AnnotationMirror}s
   *
   * @param c1 a non-{@code null} {@link Collection} of {@link AnnotationMirror}s
   *
   * @param p a {@link Predicate} that returns {@code true} if a given {@link ExecutableElement}, representing an
   * annotation interface element, is to be included in the computation; may be {@code null} in which case it is as if
   * {@code e -> true} were supplied instead
   *
   * @return {@code true} if {@code c0} has all the {@linkplain #sameAnnotation(AnnotationMirror, AnnotationMirror,
   * Predicate) same annotations} as {@code c1}, and if {@code c1} has all the {@linkplain
   * #sameAnnotation(AnnotationMirror, AnnotationMirror, Predicate) same annotations} as {@code c0}
   *
   * @exception NullPointerException if either {@code c0} or {@code c1} is {@code null}
   *
   * @see #containsAll(Collection, Collection, Predicate)
   *
   * @see #sameAnnotation(AnnotationMirror, AnnotationMirror, Predicate)
   */
  public static final boolean sameAnnotations(final Collection<? extends AnnotationMirror> c0,
                                              final Collection<? extends AnnotationMirror> c1,
                                              final Predicate<? super ExecutableElement> p) {
    return c0.size() == c1.size() && containsAll(c0, c1, p) && containsAll(c1, c0, p);
  }

  /**
   * Returns a non-{@code null}, sequential {@link Stream} that traverses the supplied {@link AnnotatedConstruct}'s
   * {@linkplain AnnotatedConstruct#getAnnotationMirrors() annotations}, and their (meta-) annotations, in breadth-first
   * order.
   *
   * <p>Cycles and duplicates are avoided via usage of the {@link #sameAnnotation(AnnotationMirror, AnnotationMirror)}
   * method. Consequently the returned {@link Stream} yields only semantically distinct elements.</p>
   *
   * @param ac an {@link AnnotatedConstruct}; must not be {@code null}
   *
   * @return a non-{@code null} {@link Stream} of {@link AnnotationMirror}s
   *
   * @exception NullPointerException if {@code ac} is {@code null}
   *
   * @see #sameAnnotation(AnnotationMirror, AnnotationMirror)
   */
  public static final Stream<AnnotationMirror> streamBreadthFirst(final AnnotatedConstruct ac) {
    return streamBreadthFirst(ac, AnnotationMirrors::returnTrue);
  }

  /**
   * Returns a non-{@code null}, sequential {@link Stream} that traverses the supplied {@link AnnotatedConstruct}'s
   * {@linkplain AnnotatedConstruct#getAnnotationMirrors() annotations}, and their (meta-) annotations, in breadth-first
   * order.
   *
   * <p>Cycles and duplicates are avoided via usage of the {@link #sameAnnotation(AnnotationMirror, AnnotationMirror,
   * Predicate)} method. Consequently the returned {@link Stream} yields only semantically distinct elements.</p>
   *
   * @param ac an {@link AnnotatedConstruct}; must not be {@code null}
   *
   * @param p a {@link Predicate} that returns {@code true} if a given {@link ExecutableElement}, representing an
   * annotation interface element, is to be included in comparison operations; may be {@code null} in which case it is
   * as if {@code e -> true} were supplied instead
   *
   * @return a non-{@code null} {@link Stream} of {@link AnnotationMirror}s
   *
   * @exception NullPointerException if {@code ac} is {@code null}
   *
   * @see #streamBreadthFirst(Collection, Predicate)
   *
   * @see #sameAnnotation(AnnotationMirror, AnnotationMirror)
   */
  public static final Stream<AnnotationMirror> streamBreadthFirst(final AnnotatedConstruct ac,
                                                                  final Predicate<? super ExecutableElement> p) {
    return streamBreadthFirst(ac.getAnnotationMirrors(), p);
  }

  /**
   * Returns a non-{@code null}, sequential {@link Stream} that traverses the supplied {@link AnnotationMirror}s, and
   * their (meta-) annotations, in breadth-first order.
   *
   * <p>Cycles and duplicates are avoided via usage of the {@link #sameAnnotation(AnnotationMirror, AnnotationMirror)}
   * method. Consequently the returned {@link Stream} yields only semantically distinct elements.</p>
   *
   * @param ams a {@link Collection} of {@link AnnotationMirror}s; must not be {@code null}
   *
   * @return a non-{@code null} {@link Stream} of (distinct) {@link AnnotationMirror}s
   *
   * @exception NullPointerException if {@code ams} is {@code null}
   *
   * @see #sameAnnotation(AnnotationMirror, AnnotationMirror)
   */
  public static final Stream<AnnotationMirror> streamBreadthFirst(final Collection<? extends AnnotationMirror> ams) {
    return streamBreadthFirst(ams, AnnotationMirrors::returnTrue);
  }

  /**
   * Returns a non-{@code null}, sequential {@link Stream} that traverses the supplied {@link AnnotationMirror}s, and
   * their (meta-) annotations, in breadth-first order.
   *
   * <p>Cycles and duplicates are avoided via usage of the {@link #sameAnnotation(AnnotationMirror, AnnotationMirror,
   * Predicate)} method. Consequently the returned {@link Stream} yields only semantically distinct elements.</p>
   *
   * @param ams a {@link Collection} of {@link AnnotationMirror}s; must not be {@code null}
   *
   * @param p a {@link Predicate} that returns {@code true} if a given {@link ExecutableElement}, representing an
   * annotation element, is to be included in comparison operations; may be {@code null} in which case it is as if
   * {@code e -> true} were supplied instead
   *
   * @return a non-{@code null} {@link Stream} of (distinct) {@link AnnotationMirror}s
   *
   * @exception NullPointerException if {@code ams} is {@code null}
   *
   * @see #sameAnnotation(AnnotationMirror, AnnotationMirror, Predicate)
   */
  public static final Stream<AnnotationMirror> streamBreadthFirst(final Collection<? extends AnnotationMirror> ams,
                                                                  final Predicate<? super ExecutableElement> p) {
    if (ams.isEmpty()) {
      return empty();
    }
    final Collection<AnnotationMirror> seen = new ArrayList<>();
    final Queue<AnnotationMirror> q = new ArrayDeque<>();
    DEDUP_LOOP_0:
    for (final AnnotationMirror a0 : ams) {
      for (final AnnotationMirror a1 : seen) {
        if (sameAnnotation(a0, a1, p)) {
          continue DEDUP_LOOP_0;
        }
      }
      q.add(a0);
      seen.add(a0);
    }
    return
      iterate(q.poll(),
              Objects::nonNull,
              a0 -> {
                DEDUP_LOOP_1:
                for (final AnnotationMirror a1 : a0.getAnnotationType().asElement().getAnnotationMirrors()) {
                  for (final AnnotationMirror a2 : seen) {
                    if (sameAnnotation(a1, a2, p)) {
                      continue DEDUP_LOOP_1;
                    }
                  }
                  q.add(a1);
                  seen.add(a1);
                }
                return q.poll();
              });
  }

  /**
   * Returns a non-{@code null}, sequential {@link Stream} that traverses the supplied {@link AnnotatedConstruct}'s
   * {@linkplain AnnotatedConstruct#getAnnotationMirrors() annotations}, and their (meta-) annotations, in depth-first
   * order.
   *
   * <p>Cycles and duplicates are avoided via usage of the {@link #sameAnnotation(AnnotationMirror, AnnotationMirror)}
   * method. Consequently the returned {@link Stream} yields only semantically distinct elements.</p>
   *
   * @param ac an {@link AnnotatedConstruct}; must not be {@code null}
   *
   * @return a non-{@code null} {@link Stream} of {@link AnnotationMirror}s
   *
   * @exception NullPointerException if {@code ac} is {@code null}
   *
   * @see #streamDepthFirst(AnnotatedConstruct, Predicate)
   *
   * @see #sameAnnotation(AnnotationMirror, AnnotationMirror)
   */
  public static final Stream<AnnotationMirror> streamDepthFirst(final AnnotatedConstruct ac) {
    return streamDepthFirst(ac, AnnotationMirrors::returnTrue); // 17 == arbitrary
  }

  /**
   * Returns a non-{@code null}, sequential {@link Stream} that traverses the supplied {@link AnnotatedConstruct}'s
   * {@linkplain AnnotatedConstruct#getAnnotationMirrors() annotations}, and their (meta-) annotations, in depth-first
   * order.
   *
   * <p>Cycles and duplicates are avoided via usage of the {@link #sameAnnotation(AnnotationMirror, AnnotationMirror,
   * Predicate)} method. Consequently the returned {@link Stream} yields only semantically distinct elements.</p>
   *
   * @param ac an {@link AnnotatedConstruct}; must not be {@code null}
   *
   * @param p a {@link Predicate} that returns {@code true} if a given {@link ExecutableElement}, representing an
   * annotation element, is to be included in comparison operations; may be {@code null} in which case it is as if
   * {@code e -> true} were supplied instead
   *
   * @return a non-{@code null}, sequential {@link Stream} of {@link AnnotationMirror}s
   *
   * @exception NullPointerException if {@code ac} is {@code null}
   *
   * @see #sameAnnotation(AnnotationMirror, AnnotationMirror)
   */
  public static final Stream<AnnotationMirror> streamDepthFirst(final AnnotatedConstruct ac,
                                                                final Predicate<? super ExecutableElement> p) {
    return streamDepthFirst(ac, new ArrayList<>(17), p); // 17 == arbitrary
  }

  /**
   * Returns a non-{@code null}, sequential {@link Stream} that traverses the supplied {@link AnnotationMirror}s, and
   * their (meta-) annotations, in depth-first order.
   *
   * <p>Cycles and duplicates are avoided via usage of the {@link #sameAnnotation(AnnotationMirror, AnnotationMirror)}
   * method. Consequently the returned {@link Stream} yields only semantically distinct elements.</p>
   *
   * @param ams a non-{@code null} {@link Collection} of {@link AnnotationMirror}s
   *
   * @return a non-{@code null}, seqwuential {@link Stream} of {@link AnnotationMirror}s
   *
   * @exception NullPointerException if {@code ams} is {@code null}
   *
   * @see #streamDepthFirst(Collection, Predicate)
   *
   * @see #sameAnnotation(AnnotationMirror, AnnotationMirror)
   */
  public static final Stream<AnnotationMirror> streamDepthFirst(final Collection<? extends AnnotationMirror> ams) {
    return streamDepthFirst(ams, AnnotationMirrors::returnTrue); // 17 == arbitrary
  }

  /**
   * Returns a non-{@code null}, sequential {@link Stream} that traverses the supplied {@link AnnotationMirror}s, and
   * their (meta-) annotations, in depth-first order.
   *
   * <p>Cycles and duplicates are avoided via usage of the {@link #sameAnnotation(AnnotationMirror, AnnotationMirror,
   * Predicate)} method. Consequently the returned {@link Stream} yields only semantically distinct elements.</p>
   *
   * @param ams a non-{@code null} {@link Collection} of {@link AnnotationMirror}s
   *
   * @param p a {@link Predicate} that returns {@code true} if a given {@link ExecutableElement}, representing an
   * annotation element, is to be included in comparison operations; may be {@code null} in which case it is as if
   * {@code e -> true} were supplied instead
   *
   * @return a non-{@code null}, sequential {@link Stream} of {@link AnnotationMirror}s
   *
   * @exception NullPointerException if {@code ams} is {@code null}
   *
   * @see #sameAnnotation(AnnotationMirror, AnnotationMirror)
   */
  public static final Stream<AnnotationMirror> streamDepthFirst(final Collection<? extends AnnotationMirror> ams,
                                                                final Predicate<? super ExecutableElement> p) {
    return ams.isEmpty() ? empty() : streamDepthFirst(ams, new ArrayList<>(17), p); // 17 == arbitrary
  }


  /**
   * Returns a non-{@code null}, determinate, immutable {@link Set} of {@link ElementType}s describing restrictions
   * concerning where the annotation interface {@linkplain AnnotationMirror#getAnnotationType() represented} by the
   * supplied {@link AnnotationMirror} may be applied.
   *
   * @param a an {@link AnnotationMirror}; must not be {@code null}
   *
   * @return a non-{@code null}, determinate, immutable {@link Set} of {@link ElementType}s describing restrictions
   * concerning where the annotation interface {@linkplain AnnotationMirror#getAnnotationType() represented} by the
   * supplied {@link AnnotationMirror} may be applied
   *
   * @exception NullPointerException if {@code a} is {@code null}
   */
  public static final Set<ElementType> targetElementTypes(final AnnotationMirror a) {
    return targetElementTypes((TypeElement)a.getAnnotationType().asElement());
  }

  /**
   * Returns a non-{@code null}, determinate, immutable {@link Set} of {@link ElementType}s describing restrictions
   * concerning where the supplied annotation interface may be applied.
   *
   * @param annotationInterface a {@link TypeElement} representing an {@linkplain
   * javax.lang.model.element.ElementKind#ANNOTATION_TYPE annotation interface}; must not be {@code null}
   *
   * @return a non-{@code null}, determinate, immutable {@link Set} of {@link ElementType}s describing restrictions
   * concerning where the supplied annotation interface may be applied
   *
   * @exception NullPointerException if {@code annotationInterface} is {@code null}
   *
   * @see java.lang.annotation.ElementType
   *
   * @see java.lang.annotation.Target
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se25/html/jls-9.html#jls-9.6.4.1 Java Language Specification,
   * section 9.6.4.1
   */
  public static final Set<ElementType> targetElementTypes(final TypeElement annotationInterface) {
    if (annotationInterface.getKind() == ANNOTATION_TYPE) {
      for (final AnnotationMirror ma : annotationInterface.getAnnotationMirrors()) {
        if (((QualifiedNameable)ma.getAnnotationType().asElement()).getQualifiedName().contentEquals("java.lang.annotation.Target")) {
          @SuppressWarnings("unchecked")
          final List<? extends AnnotationValue> elementTypes = (List<? extends AnnotationValue>)get(ma, "value");
          if (elementTypes.isEmpty()) {
            break;
          }
          final Set<ElementType> s = EnumSet.noneOf(ElementType.class);
          for (final AnnotationValue av : elementTypes) {
            s.add(ElementType.valueOf(((VariableElement)av.getValue()).getSimpleName().toString()));
          }
          return unmodifiableSet(s);
        }
      }
    }
    return EMPTY_ELEMENT_TYPES;
  }

  /**
   * Returns {@code true} if and only if the supplied {@link ExecutableElement} represents a valid <dfn>annotation
   * interface element</dfn> as defined by <a
   * href="https://docs.oracle.com/javase/specs/jls/se25/html/jls-9.html#jls-9.6.1">the Java Language Specification</a>.
   *
   * @param e an {@link ExecutableElement}; must not be {@code null}
   *
   * @return {@code true} if and only if the supplied {@link ExecutableElement} represents a valid <dfn>annotation
   * interface element</dfn> as defined by <a
   * href="https://docs.oracle.com/javase/specs/jls/se25/html/jls-9.html#jls-9.6.1">the Java Language Specification</a>
   *
   * @exception NullPointerException if {@code e} is {@code null}
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se25/html/jls-9.html#jls-9.6.1 Java Language Specification, section
   * 9.6.1
   */
  public static final boolean validAnnotationInterfaceElement(final ExecutableElement e) {
    return
      e.getKind() == METHOD &&
      e.getEnclosingElement().getKind() == ANNOTATION_TYPE &&
      e.getTypeParameters().isEmpty() &&
      e.getParameters().isEmpty() &&
      e.getThrownTypes().isEmpty() &&
      validAnnotationInterfaceElementType(e.getReturnType()) &&
      (e.getModifiers().isEmpty() ||
       e.getModifiers().equals(EnumSet.of(ABSTRACT)) ||
       e.getModifiers().equals(EnumSet.of(PUBLIC)));
  }

  /**
   * Returns {@code true} if and only if the supplied {@link TypeMirror} is a valid type for an <dfn>annotation
   * interface element</dfn> as defined by <a
   * href="https://docs.oracle.com/javase/specs/jls/se25/html/jls-9.html#jls-9.6.1">the Java Language Specification</a>.
   *
   * @param t a {@link TypeMirror}; must not be {@code null}
   *
   * @return {@code true} if and only if the supplied {@link TypeMirror} is a valid type for an <dfn>annotation
   * interface element</dfn> as defined by <a
   * href="https://docs.oracle.com/javase/specs/jls/se25/html/jls-9.html#jls-9.6.1">the Java Language Specification</a>
   *
   * @exception NullPointerException if {@code t} is {@code null}
   *
   * @spec https://docs.oracle.com/javase/specs/jls/se25/html/jls-9.html#jls-9.6.1 Java Language Specification, section
   * 9.6.1
   */
  public static final boolean validAnnotationInterfaceElementType(final TypeMirror t) {
    return switch (t) {
    case null -> throw new NullPointerException("t");
    case ArrayType at when at.getKind() == ARRAY -> validAnnotationInterfaceElementScalarType(at.getComponentType());
    default -> validAnnotationInterfaceElementScalarType(t);
    };
  }

  static final boolean validAnnotationInterfaceElementScalarType(final TypeMirror t) {
    return switch (t) {
    case null -> throw new NullPointerException("t");
    case PrimitiveType pt when pt.getKind().isPrimitive() -> true;
    case DeclaredType dt when dt.getKind() == DECLARED -> {
      final TypeElement te = (TypeElement)dt.asElement();
      yield switch (te.getKind()) {
      case ANNOTATION_TYPE, ENUM -> true;
      case CLASS -> {
        final Name fqn = te.getQualifiedName();
        yield fqn.contentEquals("java.lang.Class") || fqn.contentEquals("java.lang.String");
      }
      default -> false;
      };
    }
    default -> false;
    };
  }

  @SuppressWarnings("unchecked")
  private static final <K, V> SequencedMap<K, V> emptySequencedMap() {
    return (SequencedMap<K, V>)EMPTY_MAP;
  }

  private static final <X> boolean returnTrue(final X ignored) {
    return true;
  }

  private static final Stream<AnnotationMirror> streamDepthFirst(final AnnotatedConstruct ac,
                                                                 final Collection<AnnotationMirror> seen,
                                                                 final Predicate<? super ExecutableElement> p) {
    return streamDepthFirst(ac.getAnnotationMirrors(), seen, p);
  }

  private static final Stream<AnnotationMirror> streamDepthFirst(final Collection<? extends AnnotationMirror> ams,
                                                                 final Collection<AnnotationMirror> seen,
                                                                 final Predicate<? super ExecutableElement> p) {
    if (ams.isEmpty()) {
      return empty();
    }
    // See https://www.techempower.com/blog/2016/10/19/efficient-multiple-stream-concatenation-in-java/
    return
      Stream.of(ams
                .stream()
                .sequential()
                .flatMap(a0 -> {
                    for (final AnnotationMirror a1 : seen) {
                      if (sameAnnotation(a0, a1, p)) {
                        return empty();
                      }
                    }
                    seen.add(a0);
                    return streamDepthFirst(a0.getAnnotationType().asElement().getAnnotationMirrors(), seen, p);
                  }))
      .flatMap(identity());
  }

}
