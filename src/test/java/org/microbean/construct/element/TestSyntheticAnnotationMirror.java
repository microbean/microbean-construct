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

import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import org.junit.jupiter.api.Test;

import org.microbean.construct.DefaultDomain;
import org.microbean.construct.Domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Deprecated(since = "never") // used by a test in this suite; this class is not actually deprecated
final class TestSyntheticAnnotationMirror {

  private TestSyntheticAnnotationMirror() {
    super();
  }

  @Test
  final void test() {
    final AnnotationMirror deprecated =
      new DefaultDomain().typeElement(this.getClass().getCanonicalName()).getAnnotationMirrors().get(0);
    final TypeElement deprecatedTE = (TypeElement)deprecated.getAnnotationType().asElement();

    final List<? extends Element> elements = deprecatedTE.getEnclosedElements();
    assertEquals(2, elements.size()); // since, forRemoval

    ExecutableElement ee = (ExecutableElement)elements.get(0);
    assertTrue(ee.getSimpleName().contentEquals("since")); // declaration order is retained per contract
    ee = (ExecutableElement)elements.get(1);
    assertTrue(ee.getSimpleName().contentEquals("forRemoval"));

    final AnnotationMirror syntheticDeprecated = new SyntheticAnnotationMirror(deprecated);

    final Map<? extends ExecutableElement, ? extends AnnotationValue> originalValues = deprecated.getElementValues();
    final Map<? extends ExecutableElement, ? extends AnnotationValue> syntheticValues = syntheticDeprecated.getElementValues();
    assertEquals(originalValues.size(), syntheticValues.size());
    assertNotEquals(originalValues, syntheticValues);

    final SyntheticAnnotationTypeElement syntheticDeprecatedTE =
      (SyntheticAnnotationTypeElement)syntheticDeprecated.getAnnotationType().asElement();
    assertTrue(deprecatedTE.getQualifiedName().contentEquals(syntheticDeprecatedTE.getQualifiedName()));

    final List<? extends AnnotationMirror> metaAnnotations = syntheticDeprecatedTE.getAnnotationMirrors();
    assertEquals(3, metaAnnotations.size()); // @Documented, @Retention, @Target
    assertEquals(deprecatedTE.getAnnotationMirrors(), metaAnnotations); // note that synthetic copies weren't made

    syntheticDeprecatedTE.getAnnotationMirrors().clear(); // annotations are mutable on synthetics
    assertEquals(0, metaAnnotations.size()); // annotation collections that are returned are thread-safe and mutable
    assertEquals(3, deprecatedTE.getAnnotationMirrors().size()); // this was unaffected
  }

}
