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
 * @see #get()
 *
 * @see #of()
 *
 * @see #close()
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


  /**
   * Closes this {@link RuntimeProcessingEnvironmentSupplier}, which invalidates all {@link ProcessingEnvironment}s
   * {@linkplain #get() supplied} by it.
   *
   * <p>A subsequent call to {@link #get()} will reset this {@link RuntimeProcessingEnvironmentSupplier}.</p>
   *
   * <p>Closing a {@link RuntimeProcessingEnvironmentSupplier} that has already been closed has no effect.</p>
   *
   * <p>Most users should not have a need to call this method. {@link RuntimeProcessingEnvironmentSupplier} instances do
   * not have to be closed.</p>
   *
   * @see #get()
   */
  @Override // AutoCloseable
  public final void close() {
    this.r.get().cancel(true);
  }

  /**
   * Returns a non-{@code null}, {@link ProcessingEnvironment} suitable for runtime use.
   *
   * <p>{@link ProcessingEnvironment} instances are not guaranteed to be thread-safe.</p>
   *
   * @return a non-{@code null} {@link ProcessingEnvironment}
   *
   * @see Domain
   */
  @Override // Supplier<ProcessingEnvironment>
  public final ProcessingEnvironment get() {
    final BlockingCompilationTask f = this.r.get();
    return
      (f.isCompletedExceptionally() && f.exceptionNow() instanceof ClosedProcessorException ? install(this.r::set) : f)
      .join();
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
   * Returns a non-{@code null} {@link RuntimeProcessingEnvironmentSupplier}.
   *
   * @return a non-{@code null} {@link RuntimeProcessingEnvironmentSupplier}
   *
   * @see ProcessingEnvironment
   *
   * @see Domain
   */
  public static final RuntimeProcessingEnvironmentSupplier of() {
    return INSTANCE;
  }

}
