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

import java.io.IOException;
import java.io.UncheckedIOException;

import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;

import java.lang.System.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

import static java.lang.System.Logger.Level.DEBUG;

final class ReadOnlyModularJavaFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {


    /*
     * Static fields.
     */


    private static final Logger LOGGER = System.getLogger(ReadOnlyModularJavaFileManager.class.getName());

    private static final Set<JavaFileObject.Kind> ALL_KINDS = EnumSet.allOf(JavaFileObject.Kind.class);


    /*
     * Instance fields.
     */


    private final Set<Location> locations;

    private final Map<ModuleReference, Map<String, List<JavaFileRecord>>> maps;


    /*
     * Constructors.
     */


    ReadOnlyModularJavaFileManager(final StandardJavaFileManager fm, final Collection<? extends ReadOnlyModuleLocation> moduleLocations) {
      super(fm);
      this.maps = new ConcurrentHashMap<>();
      this.locations = moduleLocations == null ? Set.of() : Set.copyOf(moduleLocations);
      if (LOGGER.isLoggable(DEBUG)) {
        LOGGER.log(DEBUG, "Module locations: " + this.locations);
      }
    }


    /*
     * Instance methods.
     */


    @Override
    public final void close() throws IOException {
      super.close();
      this.maps.clear();
    }

    @Override
    public final ClassLoader getClassLoader(final Location packageOrientedLocation) {
      assert !packageOrientedLocation.isModuleOrientedLocation();
      if (packageOrientedLocation instanceof ReadOnlyModuleLocation m) {
        return this.getClass().getModule().getLayer().findLoader(m.getName());
      }
      return super.getClassLoader(packageOrientedLocation);
    }

    @Override
    public final JavaFileObject getJavaFileForInput(final Location packageOrientedLocation,
                                                    final String className,
                                                    final JavaFileObject.Kind kind) throws IOException {
      if (packageOrientedLocation instanceof ReadOnlyModuleLocation m) {
        try (final ModuleReader mr = m.moduleReference().open()) {
          return mr.find(className.replace('.', '/') + kind.extension)
            .map(u -> new JavaFileRecord(kind, className, u))
            .orElse(null);
        } catch (final IOException e) {
          throw new UncheckedIOException(e.getMessage(), e);
        }
      }
      return super.getJavaFileForInput(packageOrientedLocation, className, kind);
    }

    @Override
    public final boolean hasLocation(final Location location) {
      return switch (location) {
      case null -> false;
      case StandardLocation s -> switch (s) {
      case CLASS_PATH, MODULE_PATH, PATCH_MODULE_PATH, PLATFORM_CLASS_PATH, SYSTEM_MODULES, UPGRADE_MODULE_PATH -> {
        assert !s.isOutputLocation();
        yield super.hasLocation(s);
      }
      case ANNOTATION_PROCESSOR_MODULE_PATH, ANNOTATION_PROCESSOR_PATH, CLASS_OUTPUT, MODULE_SOURCE_PATH, NATIVE_HEADER_OUTPUT, SOURCE_OUTPUT, SOURCE_PATH -> false;
      };
      case ReadOnlyModuleLocation m -> true;
      default -> !location.isOutputLocation() && super.hasLocation(location);
      };
    }

    @Override
    public final Iterable<JavaFileObject> list(final Location packageOrientedLocation,
                                               final String packageName,
                                               final Set<JavaFileObject.Kind> kinds,
                                               final boolean recurse)
      throws IOException {
      if (packageOrientedLocation instanceof ReadOnlyModuleLocation m) {
        final ModuleReference mref = m.moduleReference();
        if (recurse) {
          // Don't cache anything; not really worth it
          try (final ModuleReader reader = mref.open()) {
            return list(reader, packageName, kinds, true);
          }
        }
        final Map<String, List<JavaFileRecord>> m0 = this.maps.computeIfAbsent(mref, mr -> {
            try (final ModuleReader reader = mr.open();
                 final Stream<String> ss = reader.list()) {
              return
                Collections.unmodifiableMap(ss.filter(s -> !s.endsWith("/"))
                                            .collect(HashMap::new,
                                                     (map, s) -> {
                                                       // s is, e.g., "foo/Bar.class"
                                                       final int lastSlashIndex = s.lastIndexOf('/');
                                                       assert lastSlashIndex != 0;
                                                       final String p0 = lastSlashIndex > 0 ? s.substring(0, lastSlashIndex).replace('/', '.') : "";
                                                       // p0 is now "foo"; list will be class files under package foo
                                                       final List<JavaFileRecord> list = map.computeIfAbsent(p0, p1 -> new ArrayList<>());
                                                       final JavaFileObject.Kind kind = kind(s);
                                                       try {
                                                         list.add(new JavaFileRecord(kind,
                                                                                     kind == JavaFileObject.Kind.CLASS || kind == JavaFileObject.Kind.SOURCE ?
                                                                                     s.substring(0, s.length() - kind.extension.length()).replace('/', '.') :
                                                                                     null,
                                                                                     reader.find(s).orElseThrow()));
                                                       } catch (final IOException ioException) {
                                                         throw new UncheckedIOException(ioException.getMessage(), ioException);
                                                       }
                                                     },
                                                     Map::putAll));
            } catch (final IOException ioException) {
              throw new UncheckedIOException(ioException.getMessage(), ioException);
            }
          });
        List<JavaFileRecord> unfilteredPackageContents = m0.get(packageName);
        if (unfilteredPackageContents == null) {
          return List.of();
        }
        assert !unfilteredPackageContents.isEmpty();
        if (kinds.size() < ALL_KINDS.size()) {
          unfilteredPackageContents = new ArrayList<>(unfilteredPackageContents);
          unfilteredPackageContents.removeIf(f -> !kinds.contains(f.kind()));
        }
        return Collections.unmodifiableList(unfilteredPackageContents);
      }
      return super.list(packageOrientedLocation, packageName, kinds, recurse);
    }

    // Returns package-oriented locations (or output locations if the given moduleOrientedOrOutputLocation is an output
    // location).
    @Override
    public final Iterable<Set<Location>> listLocationsForModules(final Location moduleOrientedOrOutputLocation) throws IOException {
      return
        moduleOrientedOrOutputLocation == StandardLocation.MODULE_PATH ?
        List.of(this.locations) :
        super.listLocationsForModules(moduleOrientedOrOutputLocation);
    }

    @Override
    public final String inferBinaryName(final Location packageOrientedLocation, final JavaFileObject file) {
      return file instanceof JavaFileRecord f ? f.binaryName() : super.inferBinaryName(packageOrientedLocation, file);
    }

    @Override
    public final String inferModuleName(final Location packageOrientedLocation) throws IOException {
      if (packageOrientedLocation instanceof ReadOnlyModuleLocation m) {
        assert m.getName() != null : "m.getName() == null: " + m;
        return m.getName();
      }
      return super.inferModuleName(packageOrientedLocation);
    }

    @Override
    public final boolean contains(final Location packageOrModuleOrientedLocation, final FileObject fo) throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public final FileObject getFileForInput(final Location packageOrientedLocation,
                                            final String packageName,
                                            final String relativeName)
      throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public final FileObject getFileForOutput(final Location outputLocation,
                                             final String packageName,
                                             final String relativeName,
                                             final FileObject sibling)
      throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public final FileObject getFileForOutputForOriginatingFiles(final Location outputLocation,
                                                                final String packageName,
                                                                final String relativeName,
                                                                final FileObject... originatingFiles)
      throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public final JavaFileObject getJavaFileForOutput(final Location packageOrientedLocation,
                                                     final String className,
                                                     final JavaFileObject.Kind kind,
                                                     final FileObject sibling)
      throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public final JavaFileObject getJavaFileForOutputForOriginatingFiles(final Location packageOrientedLocation,
                                                                        final String className,
                                                                        final JavaFileObject.Kind kind,
                                                                        final FileObject... originatingFiles)
      throws IOException {
      throw new UnsupportedOperationException();
    }

    // Returns a module-oriented location or an output location.
    @Override
    public final Location getLocationForModule(final Location moduleOrientedOrOutputLocation,
                                               final String moduleName) throws IOException {
      throw new UnsupportedOperationException();
    }

    // Returns a module-oriented location or an output location.
    @Override
    public final Location getLocationForModule(final Location moduleOrientedOrOutputLocation,
                                               final JavaFileObject fileObject)
      throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public final boolean isSameFile(final FileObject a, final FileObject b) {
      throw new UnsupportedOperationException();
    }


    /*
     * Static methods.
     */


    private static final JavaFileObject.Kind kind(final String s) {
      return kind(ALL_KINDS, s);
    }

    private static final JavaFileObject.Kind kind(final Iterable<? extends JavaFileObject.Kind> kinds, final String s) {
      for (final JavaFileObject.Kind kind : kinds) {
        if (kind != JavaFileObject.Kind.OTHER && s.endsWith(kind.extension)) {
          return kind;
        }
      }
      return JavaFileObject.Kind.OTHER;
    }

    private static final Iterable<JavaFileObject> list(final ModuleReader mr,
                                                       final String packageName,
                                                       final Set<JavaFileObject.Kind> kinds,
                                                       final boolean recurse)
      throws IOException {
      final String p = packageName.replace('.', '/');
      final int packagePrefixLength = p.length() + 1;
      try (final Stream<String> ss = mr.list()) {
        return ss
          .filter(s ->
                  !s.endsWith("/") &&
                  s.startsWith(p) &&
                  isAKind(kinds, s) &&
                  (recurse || s.indexOf('/', packagePrefixLength) < 0))
          .map(s -> {
              final JavaFileObject.Kind kind = kind(kinds, s);
              try {
                return
                  new JavaFileRecord(kind,
                                     kind == JavaFileObject.Kind.CLASS || kind == JavaFileObject.Kind.SOURCE ?
                                     s.substring(0, s.length() - kind.extension.length()).replace('/', '.') :
                                     null,
                                     mr.find(s).orElseThrow());
              } catch (final IOException ioException) {
                throw new UncheckedIOException(ioException.getMessage(), ioException);
              }
            })
          .collect(Collectors.toUnmodifiableList());
      }
    }

    private static final boolean isAKind(final Set<JavaFileObject.Kind> kinds, final String moduleResource) {
      for (final JavaFileObject.Kind k : kinds) {
        if (moduleResource.endsWith(k.extension)) {
          return true;
        }
      }
      return false;
    }

  }
