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

import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;

import java.util.Objects;

import javax.tools.JavaFileManager.Location;

final class ReadOnlyModuleLocation implements Location {

  private final ModuleReference moduleReference;

  ReadOnlyModuleLocation(final Module module) {
    this(module.getLayer().configuration().findModule(module.getName()).map(ResolvedModule::reference).orElse(null));
  }

  private ReadOnlyModuleLocation(final ModuleReference moduleReference) {
    super();
    this.moduleReference = Objects.requireNonNull(moduleReference, "moduleReference");
  }

  @Override // Location
  public final String getName() {
    return this.moduleReference.descriptor().name();
  }

  @Override // Location
  public final boolean isModuleOrientedLocation() {
    return false;
  }

  @Override // Location
  public final boolean isOutputLocation() {
    return false;
  }

  final ModuleReference moduleReference() {
    return this.moduleReference;
  }

  @Override // Object
  public final int hashCode() {
    return this.moduleReference.descriptor().name().hashCode();
  }

  @Override // Object
  public final boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other != null && other.getClass() == this.getClass()) {
      return Objects.equals(this.getName(), ((ReadOnlyModuleLocation)other).getName());
    } else {
      return false;
    }
  }

  @Override // Object
  public final String toString() {
    return this.moduleReference.toString();
  }

}
