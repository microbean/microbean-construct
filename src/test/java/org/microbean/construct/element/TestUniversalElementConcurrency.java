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

import org.microbean.construct.DefaultDomain;
import org.microbean.construct.Domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.parallel.Execution;

import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * A test suite that runs several operations in parallel to ensure {@link UniversalElement}s are thread safe.
 */
@Execution(CONCURRENT)
final class TestUniversalElementConcurrency {

  private Domain domain;

  private TestUniversalElementConcurrency() {
    super();
  }

  @BeforeEach
  final void setup() {
    this.domain = new DefaultDomain();
  }

  @Test
  final void test0() {
    this.domain.typeElement("java.lang.Integer").getEnclosedElements();
  }

  @Test
  final void test1() {
    this.domain.typeElement("java.lang.Float").getEnclosedElements();
  }

  @Test
  final void test2() {
    this.domain.typeElement("java.lang.String").getEnclosedElements();
  }

  @Test
  final void test3() {
    this.domain.typeElement("java.lang.Short").getInterfaces();
  }

  @Test
  final void test4() {
    this.domain.typeElement("java.lang.Long").getInterfaces();
  }

  @Test
  final void test5() {
    this.domain.typeElement("java.lang.constant.ConstantDesc").getPermittedSubclasses();
  }

  @Test
  final void test6() {
    this.domain.typeElement("java.lang.constant.MethodHandleDesc").getPermittedSubclasses();
  }

}
