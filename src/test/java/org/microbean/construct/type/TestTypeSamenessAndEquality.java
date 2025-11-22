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
package org.microbean.construct.type;

import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import org.junit.jupiter.api.Test;

import org.microbean.construct.DefaultDomain;
import org.microbean.construct.Domain;

import org.microbean.construct.element.UniversalElement;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestTypeSamenessAndEquality {

  private static final DefaultDomain domain = new DefaultDomain();

  private TestTypeSamenessAndEquality() {
    super();
  }

  @Test
  final void testObjectPrototypicalTypeIsEqualToTypeUsage() {

    final TypeElement object0 = (TypeElement)domain.typeElement("java.lang.Object").delegate();
    assertSame(object0, domain.typeElement("java.lang.Object").delegate());

    final TypeMirror objectPrototypicalType0 = object0.asType();
    assertSame(objectPrototypicalType0, object0.asType());
    
    // No matter how you get the prototypical type, it's the same (new instances aren't created; UniversalType's cache
    // doesn't affect things)
    assertSame(objectPrototypicalType0, domain.typeElement("java.lang.Object").asType().delegate());
    assertSame(objectPrototypicalType0, domain.typeElement("java.lang.Object").asType().delegate());

    assertSame(object0, ((DeclaredType)objectPrototypicalType0).asElement());

    final TypeMirror objectDeclaredType0 = domain.declaredType(object0).delegate();
    assertSame(objectDeclaredType0, domain.declaredType(object0).delegate());
    assertSame(object0, ((DeclaredType)objectDeclaredType0).asElement());

    // No matter what, the declared type (usage) and the prototypical type are not equal.
    assertNotSame(objectPrototypicalType0, objectDeclaredType0);
    assertNotEquals(objectPrototypicalType0, objectDeclaredType0);

    // They are, however, "the same".
    assertTrue(domain.sameType(objectPrototypicalType0, objectDeclaredType0));
    assertTrue(domain.sameType(objectDeclaredType0, objectPrototypicalType0));

  }

}
