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

import javax.lang.model.element.AnnotationMirror;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

final class TestCyclicAnnotationMirrors {

  private TestCyclicAnnotationMirrors() {
    super();
  }

  @Test
  final void test() {
    // java.lang.annotation.Documented, to take one arbitrary example, is annotated
    // with @java.lang.annotation.Documented. Prove you can do this with synthetic annotation constructs. This
    // relies on some degree of mutability; I've chosen to locate it in getAnnotationMirrors(), which returns a
    // thread-safe, mutable List.
    final SyntheticAnnotationTypeElement documented = new SyntheticAnnotationTypeElement("java.lang.annotation.Documented");
    final SyntheticAnnotationMirror documentedAnnotation = new SyntheticAnnotationMirror(documented);
    assertSame(documented, documentedAnnotation.getAnnotationType().asElement());
    final List<? extends AnnotationMirror> ams = documented.getAnnotationMirrors();
    assertEquals(0, ams.size());
    documented.getAnnotationMirrors().add(documentedAnnotation);
    assertSame(ams, documented.getAnnotationMirrors());
    assertEquals(1, ams.size());
    assertSame(documentedAnnotation, ams.get(0));
  }

}
