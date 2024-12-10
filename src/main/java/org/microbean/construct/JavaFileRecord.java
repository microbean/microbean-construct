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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import java.net.URI;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;

import javax.tools.JavaFileObject;

final record JavaFileRecord(JavaFileObject.Kind kind, String binaryName, URI uri) implements JavaFileObject {

  @Override
  public final URI toUri() {
    return this.uri();
  }

  @Override
  public final NestingKind getNestingKind() {
    return null;
  }

  @Override
  public final Modifier getAccessLevel() {
    return null;
  }

  @Override
  public final long getLastModified() {
    return 0L;
  }

  @Override
  public final Reader openReader(final boolean ignoreEncodingErrors) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final OutputStream openOutputStream() {
    throw new UnsupportedOperationException();
  }

  @Override
  public final CharSequence getCharContent(final boolean ignoreEncodingErrors) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final Writer openWriter() {
    throw new UnsupportedOperationException();
  }

  @Override
  public final JavaFileObject.Kind getKind() {
    return this.kind();
  }

  private final String path() {
    final String path = this.uri().getPath();
    if (path == null) {
      // Probably a jar: URI
      final String ssp = this.uri().getSchemeSpecificPart();
      return ssp.substring(ssp.lastIndexOf('!') + 1);
    }
    return path;
  }

  @Override
  public final String getName() {
    return this.path();
  }

  @Override
  public final boolean isNameCompatible(final String simpleName, final JavaFileObject.Kind kind) {
    if (kind != this.kind()) {
      return false;
    }
    final String basename = simpleName + kind.extension;
    final String path = this.path();
    return path.equals(basename) || path.endsWith("/" + basename);
  }

  @Override
  public final boolean delete() {
    return false;
  }

  @Override
  public final InputStream openInputStream() throws IOException {
    return this.uri().toURL().openConnection().getInputStream();
  }

}
