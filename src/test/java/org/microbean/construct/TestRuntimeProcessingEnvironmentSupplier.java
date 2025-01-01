/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2024–2025 microBean™.
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

import java.util.function.Supplier;

import javax.annotation.processing.ProcessingEnvironment;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Isolated
final class TestRuntimeProcessingEnvironmentSupplier {


  /*
   * Static fields.
   */


  static Supplier<? extends ProcessingEnvironment> pe;


  /*
   * Constructors.
   */


  private TestRuntimeProcessingEnvironmentSupplier() {
    super();
  }


  /*
   * Harness methods.
   */


  @BeforeEach
  final void rpe() {
    pe = RuntimeProcessingEnvironmentSupplier.of();
  }

  @AfterEach
  final void close() {
    ((RuntimeProcessingEnvironmentSupplier)pe).close();
    pe = null;
  }


  /*
   * Test methods.
   */


  // This whole test rig shows you can close the single static RuntimeProcessingEnvironmentSupplier and reload it.
  @Test
  final void testPe0() {
    assertNotNull(pe.get());
  }
  @Test
  final void testPe1() {
    assertNotNull(pe.get());
  }

}
