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

import java.util.Objects;
import java.util.Set;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;

import javax.lang.model.SourceVersion;

import javax.lang.model.element.TypeElement;

import static java.lang.System.getLogger;

import static java.lang.System.Logger.Level.DEBUG;

final class Processor extends AbstractProcessor {

  private static final Logger LOGGER = getLogger(Processor.class.getName());

  private final CompletableFuture<? super ProcessingEnvironment> c;

  private final CountDownLatch l;
  
  Processor(final CompletableFuture<? super ProcessingEnvironment> c,
            final CountDownLatch l) {
    super();
    this.c = Objects.requireNonNull(c, "c");
    this.l = Objects.requireNonNull(l, "l");
  }

  
  /*
   * Instance methods.
   */


  @Override // AbstractProcessor (Processor)
  public final void init(final ProcessingEnvironment pe) {
    if (pe == null) {
      final NullPointerException e = new NullPointerException("pe");
      this.c.completeExceptionally(e);
      throw e;
    }
    this.c.complete(pe);
    // Note to future maintainers: you're going to desperately want to move this to the process() method, and you
    // cannot. If you decide to doubt this message, at least comment this out so you don't lose it here. Don't say I
    // didn't warn you.
    try {
      this.l.await(); // NOTE: Blocks forever except in error cases
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Override // AbstractProcessor (Processor)
  public final Set<String> getSupportedAnnotationTypes() {
    return Set.of(); // we claim nothing, although it's moot because we're the only processor in existence
  }

  @Override // AbstractProcessor (Processor)
  public final Set<String> getSupportedOptions() {
    return Set.of();
  }

  @Override // AbstractProcessor (Processor)
  public final SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override // AbstractProcessor (Processor)
  public final boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnvironment) {
    return false; // we don't claim anything, but we're the only processor in existence
  }
  
}
