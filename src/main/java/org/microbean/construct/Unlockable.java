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

/**
 * An {@link AutoCloseable} extension whose {@link #close()} method throws no checked exceptions and unlocks something
 * that may have been previously locked in some unspecified manner.
 *
 * @author <a href="https://about.m/lairdnelson" target="_top">Laird Nelson</a>
 * 
 * @see #close()
 *
 * @see AutoCloseable
 */
public interface Unlockable extends AutoCloseable {

  /**
   * Unlocks this {@link Unlockable}, which normally has been semantically locked already in some unspecified manner.
   *
   * <p>Implementations of this method must be idempotent.</p>
   *
   * @exception IllegalMonitorStateException if this {@link Unlockable} is implemented directly or indirectly by a
   * {@link java.util.concurrent.locks.ReentrantLock} of some kind, and if that backing {@link
   * java.util.concurrent.locks.ReentrantLock}'s {@link java.util.concurrent.locks.ReentrantLock#unlock() unlock()}
   * method throws this exception
   *
   * @see java.util.concurrent.locks.ReentrantLock#unlock()
   */
  @Override
  public void close();

}
