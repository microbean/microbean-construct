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

import java.lang.System.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.annotation.processing.ProcessingEnvironment;

import static java.lang.System.getLogger;

import static java.lang.System.Logger.Level.ERROR;

/**
 * A utility class that can {@linkplain #get() supply} a {@link ProcessingEnvironment} suitable for use at runtime.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see #get()
 *
 * @see ProcessingEnvironment
 *
 * @see Domain
 */
public final class RuntimeProcessingEnvironment {

  private static final Logger LOGGER = getLogger(RuntimeProcessingEnvironment.class.getName());

  private static volatile ProcessingEnvironment pe;

  private RuntimeProcessingEnvironment() {
    super();
  }

  /**
   * Returns a non-{@code null} {@link ProcessingEnvironment} suitable for use at runtime.
   *
   * <p>The returned {@link ProcessingEnvironment} and the objects it supplies are not guaranteed to be safe for
   * concurrent use by multiple threads.</p>
   *
   * @return a non-{@code null} {@link ProcessingEnvironment}
   *
   * @exception IllegalStateException if an error occurs
   *
   * @see ProcessingEnvironment
   *
   * @see Domain
   */
  public static final ProcessingEnvironment get() {
    if (pe == null) { // volatile read
      final CompletableFuture<ProcessingEnvironment> f = new CompletableFuture<>();
      // Use a virtual thread since it will spend its entire time blocked/parked once it has completed the future.
      Thread.ofVirtual()
        .name(RuntimeProcessingEnvironment.class.getName())
        .uncaughtExceptionHandler((t, e) -> {
            f.completeExceptionally(e);
            if (LOGGER.isLoggable(ERROR)) {
              LOGGER.log(ERROR, e.getMessage(), e);
            }
          })
        .start(new BlockingCompilationTask(f));
      try {
        pe = f.get(); // volatile write
      } catch (final ExecutionException e) {
        throw new IllegalStateException(e.getMessage(), e);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(e.getMessage(), e);
      }
    }
    return pe; // volatile read
  }

}
