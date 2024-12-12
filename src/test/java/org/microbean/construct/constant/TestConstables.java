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
package org.microbean.construct.constant;

import java.lang.constant.ConstantDesc;

import java.lang.invoke.MethodHandles;

import javax.lang.model.element.Name;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.microbean.construct.DefaultDomain;
import org.microbean.construct.Domain;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestConstables {

  private static final Domain domain = new DefaultDomain();
  
  private TestConstables() {
    super();
  }

  @Test
  final void testDescribeName() throws ReflectiveOperationException {
    Name n = domain.name("foo");
    assertNotNull(n);
    final Optional<? extends ConstantDesc> o = Constables.describe(n, domain);
    assertTrue(o.isPresent());
    assertTrue(n.contentEquals((Name)o.orElse(null).resolveConstantDesc(MethodHandles.lookup())));
  }

}
