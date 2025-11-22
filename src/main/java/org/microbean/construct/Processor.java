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

import java.util.List;
import java.util.Objects;
import java.util.Set;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import java.util.function.Consumer;

import javax.annotation.processing.Completion;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;

import javax.lang.model.SourceVersion;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import static java.lang.System.getLogger;

import static java.lang.System.Logger.Level.DEBUG;

final class Processor implements AutoCloseable, javax.annotation.processing.Processor {

  private static final Logger LOGGER = getLogger(Processor.class.getName());

  private final Consumer<? super ProcessingEnvironment> cpe;

  // run() method invoked under lock
  private final Runnable r;

  private final Lock lock;

  private final Condition c;

  // @GuardedBy("lock")
  private boolean closed;

  Processor(final Consumer<? super ProcessingEnvironment> cpe,
            final Runnable r) {
    super();
    this.cpe = Objects.requireNonNull(cpe, "cpe");
    this.r = r == null ? Processor::sink : r;
    this.lock = new ReentrantLock();
    this.c = this.lock.newCondition();
  }


  /*
   * Instance methods.
   */


  @Override // AutoCloseable
  public final void close() {
    this.lock.lock();
    try {
      if (!this.closed) {
        this.closed = true;
        this.c.signal();
      }
    } finally {
      this.lock.unlock();
    }
  }

  /**
   * Initializes this {@link Processor}.
   *
   * @param pe a {@link ProcessingEnvironment}; must not be {@code null}
   *
   * @deprecated This method should be called only by a Java compiler in accordance with annotation processing
   * contracts.
   */
  @Deprecated // to be called only by a Java compiler in accordance with annotation processing contracts
  @Override // Processor;
  public final void init(final ProcessingEnvironment pe) {
    this.lock.lock();
    try {
      if (this.closed) {
        this.closed = false;
      }
      this.cpe.accept(pe);
      while (!this.closed) {
        this.c.awaitUninterruptibly();
      }
    } finally {
      this.r.run();
      this.lock.unlock();
    }
  }

  @Override // Processor
  public final Iterable<? extends Completion> getCompletions(final Element element,
                                                             final AnnotationMirror annotation,
                                                             final ExecutableElement member,
                                                             final String userText) {
    return List.of();
  }

  @Override // Processor
  public final Set<String> getSupportedAnnotationTypes() {
    return Set.of();
  }

  @Override // Processor
  public final Set<String> getSupportedOptions() {
    return Set.of();
  }

  @Override // Processor
  public final SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override // Processor
  public final boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnvironment) {
    return false;
  }

  private static final void sink() {}

}
