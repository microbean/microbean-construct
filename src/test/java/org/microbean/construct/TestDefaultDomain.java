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
package org.microbean.construct;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.microbean.construct.element.UniversalElement;

final class TestDefaultDomain {

  private static DefaultDomain domain;

  private TestDefaultDomain() {
    super();
  }

  @BeforeAll
  private static final void createDomain() {
    domain = new DefaultDomain();
  }
  
  @Test
  final void testJavaLangObject() {
    final UniversalElement e = domain.javaLangObject();
    assertTrue(e.getQualifiedName().contentEquals("java.lang.Object"));
  }
  
}
