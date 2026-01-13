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

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeMirror;

import org.junit.jupiter.api.Test;

import org.microbean.construct.DefaultDomain;

import static javax.lang.model.type.TypeKind.ARRAY;
import static javax.lang.model.type.TypeKind.DECLARED;
import static javax.lang.model.type.TypeKind.VOID;

import static javax.lang.model.util.ElementFilter.methodsIn;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.microbean.construct.element.AnnotationMirrors.get;

@TestTypes.Gorp
final class TestTypes {

  private static final DefaultDomain domain = new DefaultDomain();

  private TestTypes() {
    super();
  }

  @Test
  final void testTypeOfIntArrayClass() {
    final AnnotationMirror am = domain.typeElement(this.getClass().getCanonicalName()).getAnnotationMirrors().get(0);
    final TypeMirror t = (TypeMirror)get(am, "iac");
    assertTrue(t instanceof ArrayType);
    assertSame(ARRAY, t.getKind());
  }

  @Test
  final void testTypeOfVoidClass() {
    final AnnotationMirror am = domain.typeElement(this.getClass().getCanonicalName()).getAnnotationMirrors().get(0);
    final TypeMirror t = (TypeMirror)get(am, "vc");
    assertTrue(t instanceof NoType);
    assertSame(VOID, t.getKind());
  }

  @Test
  final void testAnnotationMirrorRepresentationOfElementReturnTypes() {
    final AnnotationMirror am = domain.typeElement(this.getClass().getCanonicalName()).getAnnotationMirrors().get(0);
    for (final ExecutableElement ee : methodsIn(am.getAnnotationType().asElement().getEnclosedElements())) {
      assertSame(DECLARED, ee.getReturnType().getKind());
    }
  }

  static @interface Gorp {

    Class<?> iac() default int[].class;

    Class<?> vc() default void.class;

  }

}
