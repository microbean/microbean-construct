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

import java.io.StringWriter;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import java.lang.module.ModuleFinder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import java.util.function.Consumer;

import java.util.stream.Stream;

import javax.annotation.processing.ProcessingEnvironment;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

import static java.lang.Boolean.TRUE;

import static java.lang.System.getLogger;
import static java.lang.System.lineSeparator;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.WARNING;

import static java.nio.charset.Charset.defaultCharset;

import static javax.tools.ToolProvider.getSystemJavaCompiler;

final class BlockingCompilationTask implements Runnable {


  /*
   * Static fields.
   */


  private static final Logger LOGGER = getLogger(BlockingCompilationTask.class.getName());


  /*
   * Instance fields.
   */


  private final CountDownLatch processorLatch;

  private final CompletableFuture<? super ProcessingEnvironment> f;

  private final Locale locale;


  /*
   * Constructors.
   */


  BlockingCompilationTask(final CompletableFuture<? super ProcessingEnvironment> f,
                          final CountDownLatch processorLatch) {
    this(f, processorLatch, null);
  }

  BlockingCompilationTask(final CompletableFuture<? super ProcessingEnvironment> f,
                          final CountDownLatch processorLatch,
                          final Locale locale) {
    super();
    this.f = Objects.requireNonNull(f, "f");
    this.processorLatch = Objects.requireNonNull(processorLatch, "processorLatch");
    this.locale = locale;
  }


  /*
   * Instance methods.
   */


  @Override // Runnable
  public final void run() {
    final JavaCompiler jc = getSystemJavaCompiler();
    if (jc == null) {
      final IllegalStateException e = new IllegalStateException("ToolProvider.getSystemJavaCompiler() == null");
      this.f.completeExceptionally(e);
      throw e;
    }

    final List<String> options = new ArrayList<>();

    // Do not actually compile anything; just run annotation processing.
    options.add("-proc:only");

    // Warnings constitute errors.
    options.add("-Werror");

    // Be verbose if necessary. Output will go to this class' Logger.
    if (Boolean.getBoolean("org.microbean.construct.verbose")) {
      options.add("-verbose");
    }

    // Use an appropriate compiler-supplied thread-safe name table.
    installNameTableOptions(options::add);
    // Optional.ofNullable(nameTableOption()).ifPresent(options::add);

    // Propagate the current classpath to the compiler environment.
    options.add("-cp");
    options.add(System.getProperty("java.class.path"));

    // Propagate the current effective module path to the compiler environment, taking into account command line
    // switches and patching. This is actually quite difficult for no good reason.
    final Collection<ReadOnlyModuleLocation> moduleLocations = new ArrayList<>();
    final Set<String> additionalRootModuleNames = additionalRootModuleNames(moduleLocations::add, options::add);

    // Set up the compilation task using all of the above.
    final Locale locale = this.locale == null ? Locale.getDefault() : this.locale;
    final DiagnosticListener<? super JavaFileObject> diagnosticLogger = d -> log(d, locale);
    final CompilationTask task =
      jc.getTask(new LogWriter(), // always wrapped in a PrintWriter by javac
                 new ReadOnlyModularJavaFileManager(jc.getStandardFileManager(diagnosticLogger,
                                                                              locale,
                                                                              defaultCharset()),
                                                    moduleLocations),
                 diagnosticLogger,
                 options,
                 List.of("java.lang.annotation.RetentionPolicy"), // arbitrary, but reads minimal number of classes
                 null); // compilation units
    task.setLocale(locale);
    task.addModules(additionalRootModuleNames);

    // Set the task's annotation processor whose only function will be to return the ProcessingEnvironment supplied to
    // it in its #init(ProcessingEnvironment) method. The supplied latch is used to make this task block forever (unless
    // an error occurs) to keep the ProcessingEnvironment "in scope".
    task.setProcessors(List.of(new Processor(this.f, processorLatch)));

    if (LOGGER.isLoggable(DEBUG)) {
      LOGGER.log(DEBUG, "CompilationTask options: " + options);
      LOGGER.log(DEBUG, "CompilationTask additional root module names: " + additionalRootModuleNames);
      LOGGER.log(DEBUG, "Calling CompilationTask");
    }

    try {
      final Boolean result = task.call(); // blocks forever deliberately unless an error occurs
      if (!TRUE.equals(result)) {
        if (LOGGER.isLoggable(ERROR)) {
          LOGGER.log(ERROR, "Calling CompilationTask failed");
        }
        throw new IllegalStateException("compilationTask.call() == " + result);
      }
    } catch (final Throwable t) {
      this.f.completeExceptionally(t);
      processorLatch.countDown(); // unblock the Processor
      switch (t) {
      case RuntimeException e -> throw e;
      case Error e -> throw e;
      default -> throw new IllegalStateException(t.getMessage(), t);
      }
    }
  }

  private final Set<String> additionalRootModuleNames(final Consumer<? super ReadOnlyModuleLocation> moduleLocations,
                                                      final Consumer<? super String> options) {
    final ModuleLayer moduleLayer = this.getClass().getModule().getLayer();
    if (moduleLayer == null) {
      return Set.of();
    }
    Set<String> additionalRootModuleNames = new HashSet<>();
    final ModuleFinder smf = ModuleFinder.ofSystem();
    final Module unnamedModule = this.getClass().getClassLoader().getUnnamedModule();
    try (final Stream<Module> s = moduleLayer.modules().stream().sequential()) {
      s
        // Figure out which runtime modules are named and not system modules. That set will be added to the compilation
        // task via its addModules(Set) method.
        .filter(m -> m.isNamed() && smf.find(m.getName()).isEmpty())
        .forEach(m -> {
            additionalRootModuleNames.add(m.getName());
            if (m.canRead(unnamedModule)) {
              options.accept("--add-reads");
              options.accept(m.getName() + "=ALL-UNNAMED");
            }
            moduleLocations.accept(new ReadOnlyModuleLocation(m));
          });
    }
    return Collections.unmodifiableSet(additionalRootModuleNames);
  }


  /*
   * Static methods.
   */


  private static final void log(final Diagnostic<? extends JavaFileObject> d, final Locale l) {
    switch (d.getKind()) {
    case ERROR:
      if (LOGGER.isLoggable(Level.ERROR)) {
        LOGGER.log(Level.ERROR, d.getMessage(l));
      }
      break;
    case MANDATORY_WARNING:
    case WARNING:
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.log(Level.WARNING, d.getMessage(l));
      }
      break;
    default:
      if (LOGGER.isLoggable(Level.INFO)) {
        LOGGER.log(Level.INFO, d.getMessage(l));
      }
      break;
    }
  }

  private static final void installNameTableOptions(final Consumer<? super String> c) {
    // See
    // https://github.com/openjdk/jdk/blob/jdk-21%2B35/src/jdk.compiler/share/classes/com/sun/tools/javac/util/Names.java#L430-L436
    // in JDK 21; JDK 22+ changes things dramatically.
    //
    // This turns out to be a critical option to get right. The default name table in javac (21 and earlier) is shared
    // and unsynchronized such that, given a Name, calling its toString() method may involve reading a shared byte array
    // that is being updated in another thread (by symbol completion?). By contrast, the unshared name table creates
    // Names whose contents are not shared, so toString() invocations on them are not problematic.
    //
    // In JDK 22+, there is a String-based name table that is used by default; see
    // https://github.com/openjdk/jdk/pull/15470. However note that this is also not thread-safe:
    // https://github.com/openjdk/jdk/blob/jdk-22%2B36/src/jdk.compiler/share/classes/com/sun/tools/javac/util/StringNameTable.java#L65
    // It is also not thread-safe.
    if (Runtime.version().feature() >= 22 && Boolean.getBoolean("useStringTable")) {
      if (LOGGER.isLoggable(WARNING)) {
        LOGGER.log(WARNING, "Using string name table");
      }
      c.accept("-XDuseStringTable");
      if (Boolean.getBoolean("internStringTable")) {
        if (LOGGER.isLoggable(DEBUG)) {
          LOGGER.log(DEBUG, "Interning string name table strings");
        }
        c.accept("-XDinternStringTable");
      }
      return;
    } else if (Boolean.getBoolean("useSharedTable")) {
      // Yikes
      if (LOGGER.isLoggable(WARNING)) {
        LOGGER.log(WARNING, "Using shared name table");
      }
      c.accept("-XDuseSharedTable");
      return;
    } else if (Boolean.parseBoolean(System.getProperty("useUnsharedTable", "true"))) {
      if (LOGGER.isLoggable(DEBUG)) {
        LOGGER.log(DEBUG, "Using unshared name table");
      }
      c.accept("-XDuseUnsharedTable");
      return;
    }
    // The user explicitly said don't use anything (?!). Log that we're using the default, whatever it might be.
    if (LOGGER.isLoggable(DEBUG)) {
      LOGGER.log(DEBUG, "Using default name table");
    }
  }


  /*
   * Inner and nested classes.
   */


  // (Always wrapped by javac in a PrintWriter.)
  //
  // Probably slower than dirt. Should only be needed when things go wrong or verbose output is on.
  private static final class LogWriter extends StringWriter {

    private static final int lsLength = lineSeparator().length();

    private LogWriter() {
      super();
    }

    @Override // StringWriter (Writer)
    public final void flush() {
      super.flush();
      final StringBuffer buffer = this.getBuffer();
      if (LOGGER.isLoggable(DEBUG)) {
        // Chop off the line separator that, thanks to the wrapping PrintWriter, absolutely will be at the end of the
        // buffer (like Perl's chop())
        LOGGER.log(DEBUG, buffer.subSequence(0, buffer.length() - lsLength));
      }
      buffer.setLength(0);
    }

  }


}
