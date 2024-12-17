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
package org.microbean.construct.vm;

import java.util.ArrayList;
import java.util.List;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;

import java.lang.invoke.TypeDescriptor;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;

import javax.lang.model.element.TypeElement;

import org.microbean.construct.Domain;

import static java.lang.constant.ConstantDescs.CD_boolean;
import static java.lang.constant.ConstantDescs.CD_byte;
import static java.lang.constant.ConstantDescs.CD_char;
import static java.lang.constant.ConstantDescs.CD_double;
import static java.lang.constant.ConstantDescs.CD_float;
import static java.lang.constant.ConstantDescs.CD_int;
import static java.lang.constant.ConstantDescs.CD_long;
import static java.lang.constant.ConstantDescs.CD_short;
import static java.lang.constant.ConstantDescs.CD_void;

/**
 * A utility class that provides {@link TypeDescriptor}s for {@link TypeMirror}s.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see #typeDescriptor(TypeMirror, Domain)
 *
 * @see TypeDescriptor#descriptorString()
 *
 * @spec https://docs.oracle.com/javase/specs/jvms/se23/html/jvms-4.html#jvms-4.3 Java Virtual Machine Specification,
 * section 4.3
 */
public final class TypeDescriptors {

  private static final ClassDesc[] EMPTY_CLASSDESC_ARRAY = new ClassDesc[0];

  private TypeDescriptors() {
    super();
  }

  @SuppressWarnings("try")
  public static final TypeDescriptor typeDescriptor(final TypeMirror t, final Domain d) {
    try (var lock = d.lock()) {
      return typeDescriptor0(t, d);
    }
  }

  private static final TypeDescriptor typeDescriptor0(final TypeMirror t, final Domain d) {
    // Precondition: under domain lock
    return switch (t.getKind()) {
    case ARRAY -> typeDescriptor0(((ArrayType)t).getComponentType(), d); // recursive
    case BOOLEAN -> CD_boolean;
    case BYTE -> CD_byte;
    case CHAR -> CD_char;
    case DECLARED -> ClassDesc.of(d.toString(d.binaryName((TypeElement)((DeclaredType)t).asElement())));
    case EXECUTABLE -> {
      final ExecutableType et = (ExecutableType)t;
      final List<? extends TypeMirror> pts = et.getParameterTypes();
      if (pts.isEmpty()) {
        yield MethodTypeDesc.of((ClassDesc)typeDescriptor0(et.getReturnType(), d), EMPTY_CLASSDESC_ARRAY);
      }
      final List<ClassDesc> ptcds = new ArrayList<>(pts.size());
      for (final TypeMirror pt : pts) {
        ptcds.add((ClassDesc)typeDescriptor0(pt, d)); // recursive
      }
      yield MethodTypeDesc.of((ClassDesc)typeDescriptor0(et.getReturnType(), d), ptcds); // recursive
    }
    case DOUBLE -> CD_double;
    case FLOAT -> CD_float;
    case INT -> CD_int;
    case LONG -> CD_long;
    case SHORT -> CD_short;
    case TYPEVAR -> typeDescriptor0(d.erasure(t), d); // recursive
    case VOID -> CD_void;
    default -> throw new IllegalArgumentException("t: " + t);
    };
  }

}
