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

import java.lang.System.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RunnableFuture;

import java.util.concurrent.atomic.AtomicReference;

import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.processing.ProcessingEnvironment;

import static java.lang.System.getLogger;

import static java.lang.System.Logger.Level.ERROR;

/**
 * A utility class that can {@linkplain #of() supply} a {@link ProcessingEnvironment} suitable for use at runtime.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see #of()
 *
 * @see ProcessingEnvironment
 *
 * @see Domain
 */
public final class RuntimeProcessingEnvironmentSupplier implements AutoCloseable, Supplier<ProcessingEnvironment> {


  /*
   * Static fields.
   */


  private static final Logger LOGGER = getLogger(RuntimeProcessingEnvironmentSupplier.class.getName());
  
  private static final RuntimeProcessingEnvironmentSupplier INSTANCE = new RuntimeProcessingEnvironmentSupplier();


  /*
   * Instance fields.
   */


  private final AtomicReference<BlockingCompilationTask> r;


  /*
   * Constructors.
   */


  private RuntimeProcessingEnvironmentSupplier() {
    super();
    this.r = new AtomicReference<>();
    install(this.r::set);
  }


  /*
   * Instance methods.
   */


  @Override // AutoCloseable
  public final void close() {
    this.r.get().cancel(true);
  }

  @Override // Supplier<ProcessingEnvironment>
  public final ProcessingEnvironment get() {
    final BlockingCompilationTask f = this.r.get();
    if (f.isCompletedExceptionally()) {
      final Throwable t = f.exceptionNow();
      if (t instanceof IllegalStateException) {
        return install(this.r::set).join();
      }
    }
    return f.join();
  }


  /*
   * Static methods.
   */


  private static final BlockingCompilationTask install(final Consumer<? super BlockingCompilationTask> c) {
    final BlockingCompilationTask f = new BlockingCompilationTask();
    c.accept(f);
    Thread.ofVirtual()
      .name(RuntimeProcessingEnvironmentSupplier.class.getName())
      .uncaughtExceptionHandler((thread, exception) -> {
          f.completeExceptionally(exception);
          if (LOGGER.isLoggable(ERROR)) {
            LOGGER.log(ERROR, exception.getMessage(), exception);
          }
        })
      .start(f);
    return f;
  }

  /**
   * Returns a non-{@code null} {@link Supplier} of a {@link ProcessingEnvironment} suitable for use at runtime.
   *
   * <p>The {@link ProcessingEnvironment} available from the returned {@link Supplier} and the objects it supplies are
   * intended to be safe for concurrent use by multiple threads.</p>
   *
   * @return a non-{@code null}, thread-safe {@link Supplier} of a non-{@code null}, thread-safe {@link
   * ProcessingEnvironment} suitable for use at runtime
   *
   * @see ProcessingEnvironment
   *
   * @see Domain
   */
  public static final Supplier<? extends ProcessingEnvironment> of() {
    return INSTANCE;
  }

}
