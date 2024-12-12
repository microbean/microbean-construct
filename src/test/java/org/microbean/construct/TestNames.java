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

import javax.lang.model.element.Name;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

final class TestNames {

  private static final DefaultDomain domain = new DefaultDomain();

  private TestNames() {
    super();
  }

  // Ugly little test to expose bad name tables in the Oracle/Sun javac compiler. The only one that appears to work in
  // the context of multiple threads is the unshared name table. See
  // https://github.com/microbean/microbean-construct/issues/1.
  @Test
  final void testNameTableImplementationIsThreadSafe() {
    final Throwable[] t = new Throwable[1];
    for (int i = 0; i < 1_000; i++) {
      final String s = String.valueOf(i);
      switch (t[0]) {
      case null ->
        Thread.ofVirtual()
          .name("Thrasher-", 1L)
          .uncaughtExceptionHandler((x, e) -> t[0] = e)
          .start(() -> domain.name(s));
      case RuntimeException e -> throw e;
      case Error e -> throw e;
      case Throwable throwable -> fail(throwable);
      }
    }
  }

}
