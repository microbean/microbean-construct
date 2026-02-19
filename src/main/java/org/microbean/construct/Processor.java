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

import java.lang.System.Logger;

import java.util.List;
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

import static java.util.Objects.requireNonNull;

final class Processor implements AutoCloseable, javax.annotation.processing.Processor {

  private static final Logger LOGGER = getLogger(Processor.class.getName());

  // @GuardedBy("lock")
  private final Consumer<? super ProcessingEnvironment> cpe;

  // @GuardedBy("lock")
  // run() method invoked under lock
  private final Runnable r;

  private final Lock lock;

  // @GuardedBy("lock")
  private final Condition c;

  // @GuardedBy("lock")
  private boolean closed;

  /**
   * Creates a new {@link Processor}.
   *
   * <p>This {@link Processor} will be invoked by a {@link BlockingCompilationTask} as part of an invocation of its
   * {@link BlockingCompilationTask#run()} method. Its {@link #init(ProcessingEnvironment)} method will be invoked as
   * part of {@linkplain javax.annotation.processing.Processor the standard <code>Processor</code> lifecycle}. It will
   * call the {@link Consumer#accept(Object) accept(ProcessingEnvironment)} method on the supplied {@link
   * Consumer}. Then it will block until {@link #close()} is invoked (from a separate thread, obviously). Before
   * exiting, it will invoke the {@link Runnable#run() run()} method of the supplied {@link Runnable}.</p>
   *
   * @param cpe a {@link Consumer} of {@link ProcessingEnvironment} instances, typically {@link
   * BlockingCompilationTask#complete(Object)}; must not be {@code null}
   *
   * @param r a {@link Runnable} that is invoked at the conclusion of an invocation of the {@link
   * #init(ProcessingEnvironment)} method; may be {@code null}
   *
   * @see #init(ProcessingEnvironment)
   *
   * @see #close()
   *
   * @see BlockingCompilationTask
   *
   * @see javax.annotation.processing.Processor
   */
  Processor(final Consumer<? super ProcessingEnvironment> cpe, // usually BlockingCompliationTask::complete
            final Runnable r) { // usually BlockingCompliationTask::obtrudeException
    super();
    this.cpe = requireNonNull(cpe, "cpe");
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
   * Initializes this {@link Processor} by calling the {@link Consumer#accept(Object) accept(Object)} method on the
   * {@link Consumer} {@linkplain #Processor(Consumer, Runnable) supplied at construction time} with the supplied {@link
   * ProcessingEnvironment}, <strong>and then blocking until another thread invokes the {@link #close()}
   * method</strong>.
   *
   * @param pe a {@link ProcessingEnvironment}; must not be {@code null}
   *
   * @deprecated This method should be called only by a Java compiler in accordance with annotation processing
   * contracts. All other usage will result in undefined behavior.
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

  /**
   * Returns an {@linkplain List#of() empty, immutable, determinate <code>List</code>} when invoked, regardless of
   * arguments.
   *
   * @param element ignored; may be {@code null}
   *
   * @param annotation ignored; may be {@code null}
   *
   * @param member ignored; may be {@code null}
   *
   * @param userText ignored; may be {@code null}
   *
   * @return an {@linkplain List#of() empty, immutable, determinate <code>List</code>} when invoked, regardless of
   * arguments
   */
  @Override // Processor
  public final Iterable<? extends Completion> getCompletions(final Element element,
                                                             final AnnotationMirror annotation,
                                                             final ExecutableElement member,
                                                             final String userText) {
    return List.of();
  }

  /**
   * Returns an {@linkplain Set#of() empty, immutable, determinate <code>Set</code>} when invoked.
   *
   * @return an  {@linkplain Set#of() empty, immutable, determinate <code>Set</code>} when invoked
   */
  @Override // Processor
  public final Set<String> getSupportedAnnotationTypes() {
    return Set.of();
  }

  /**
   * Returns an {@linkplain Set#of() empty, immutable, determinate <code>Set</code>} when invoked.
   *
   * @return an  {@linkplain Set#of() empty, immutable, determinate <code>Set</code>} when invoked
   */
  @Override // Processor
  public final Set<String> getSupportedOptions() {
    return Set.of();
  }

  /**
   * Returns the return value of an invocation of the {@link SourceVersion#latestSupported()} method when invoked.
   *
   * @return the return value of an invocation of the {@link SourceVersion#latestSupported()} method when invoked
   */
  @Override // Processor
  public final SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  /**
   * Returns {@code false} when invoked, regardless of arguments.
   *
   * @param annotations ignored; may be {@code null}
   *
   * @param roundEnvironment ignored; may be {@code null}
   */
  @Override // Processor
  public final boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnvironment) {
    return false;
  }

  // (Invoked only by method reference.)
  private static final void sink() {}

}
