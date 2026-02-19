/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2024–2026 microBean™.
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

import java.util.concurrent.locks.ReentrantLock;

/**
 * A class holding a {@link ReentrantLock} that should be used to serialize <dfn>symbol completion</dfn> and <dfn>name
 * expansion</dfn>.
 *
 * <p>Most users should simply {@linkplain DefaultDomain#DefaultDomain() use an appropriate <code>DefaultDomain</code>}
 * instead of working directly with instances of this class.</p>
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see #INSTANCE
 *
 * @see Domain#lock()
 *
 * @see Domain#toString(CharSequence)
 *
 * @see DefaultDomain#DefaultDomain()
 */
public final class SymbolCompletionLock {

  /**
   * A non-{@code null} {@link ReentrantLock} that should be used to serialize symbol completion and name expansion.
   *
   * @see Domain#lock()
   */
  public static final ReentrantLock INSTANCE = new ReentrantLock();

  private SymbolCompletionLock() {
    super();
  }

}
