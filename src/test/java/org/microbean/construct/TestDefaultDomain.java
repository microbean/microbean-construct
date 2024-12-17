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

import java.util.List;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;

import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.microbean.construct.element.UniversalElement;

import org.microbean.construct.type.UniversalType;

final class TestDefaultDomain {

  private static final DefaultDomain domain = new DefaultDomain();

  private TestDefaultDomain() {
    super();
  }

  @Test
  final void testAssignablePathological() {
    assertThrows(NullPointerException.class, () -> domain.assignable(null, null));
    assertThrows(IllegalArgumentException.class,
                 () -> domain.assignable(domain.executableElement(domain.typeElement(this.getClass().getName()),
                                                                  domain.noType(TypeKind.VOID),
                                                                  "testAssignablePathological").asType(),
                                         domain.nullType()));
  }
  
  @Test
  final void testBoxPathological() {
    assertThrows(NullPointerException.class, () -> domain.typeElement((PrimitiveType)null));
    final UniversalType t = domain.declaredType("java.lang.Object");
    // javax.lang.model.util.Types#boxedClass(PrimitiveType) just does a blind cast (undocumented).
    assertThrows(ClassCastException.class, () -> domain.typeElement(t));
  }
  
  @Test
  final void testAsMemberOfPathological() {
    assertThrows(NullPointerException.class, () -> domain.asMemberOf(null, domain.javaLangObject()));
    assertThrows(NullPointerException.class, () -> domain.asMemberOf(null, null));
  }
  
  @Test
  final void testJavaLangObject() {
    final UniversalElement e = domain.javaLangObject();
    assertTrue(e.getQualifiedName().contentEquals("java.lang.Object"));
  }

  @Test
  final void testJavaLangString() {
    final UniversalElement e = domain.typeElement("java.lang.String");
    assertTrue(e.getQualifiedName().contentEquals("java.lang.String"));
  }

  /*
    With shared name table:
    org.opentest4j.AssertionFailedError: expected: <true> but was: <false>
    at org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
    at org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
    at org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
    at org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
    at org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:31)
    at org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:183)
    at org.microbean.construct.TestDefaultDomain.testName(TestDefaultDomain.java:83)
    at java.base/java.lang.reflect.Method.invoke(Method.java:580)
    at java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:507)
    at java.base/java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1458)
    at java.base/java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:2034)
    at java.base/java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:189)

    With unshared name table:
    org.opentest4j.AssertionFailedError: expected: <true> but was: <false>
    at org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
    at org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
    at org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
    at org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
    at org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:31)
    at org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:183)
    at org.microbean.construct.TestDefaultDomain.testName(TestDefaultDomain.java:99)
    at java.base/java.lang.reflect.Method.invoke(Method.java:580)
    at java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:507)
    at java.base/java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1458)
    at java.base/java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:2034)
    at java.base/java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:189)
  */
  @Test
  final void testName() {
    final Name name = domain.name("Hello");
    assertTrue(name.contentEquals("Hello"));
  }

  @Test
  final void testListString() {
    final UniversalType t = domain.declaredType(domain.typeElement("java.util.List"),
                                                domain.declaredType("java.lang.String"));
    assertSame(TypeKind.DECLARED, t.getKind());
    assertTrue(t.asElement().getQualifiedName().contentEquals("java.util.List"));
    final List<? extends UniversalType> typeArguments = t.getTypeArguments();
    assertEquals(1, typeArguments.size());
    assertTrue(domain.sameType(domain.declaredType("java.lang.String"), typeArguments.get(0)));
  }

  @Test
  final void testInt() {
    final UniversalType t = domain.primitiveType(TypeKind.INT);
    assertSame(TypeKind.INT, t.getKind());
  }

  @Test
  final void testBox() {
    UniversalElement e = domain.typeElement(domain.primitiveType(TypeKind.INT));
    assertSame(ElementKind.CLASS, e.getKind());
    assertTrue(e.getQualifiedName().contentEquals("java.lang.Integer"));
    e = domain.typeElement(TypeKind.INT);
    assertSame(ElementKind.CLASS, e.getKind());
    assertTrue(e.getQualifiedName().contentEquals("java.lang.Integer"));
  }

  @Test
  final void testUnbox() {
    UniversalType t = domain.primitiveType(domain.typeElement("java.lang.Integer"));
    assertSame(TypeKind.INT, t.getKind());
    t = domain.primitiveType("java.lang.Integer");
    assertSame(TypeKind.INT, t.getKind());
  }

  @Test
  final void testSameElements() {
    final UniversalElement e0 = domain.typeElement("java.lang.String");
    final UniversalElement e1 = domain.typeElement("java.lang.String");
    assertNotSame(e0, e1);
    assertEquals(e0, e1);
  }

  @Test
  final void testSameTypes() {
    final UniversalType t0 = domain.declaredType("java.lang.String");
    final UniversalType t1 = domain.declaredType("java.lang.String");
    assertNotSame(t0, t1);
    assertEquals(t0, t1);
    assertTrue(domain.sameType(t0, t1));
  }
  
}
