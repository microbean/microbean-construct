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
package org.microbean.construct.constant;

import java.lang.constant.ClassDesc;

final class ConstantDescs {

  static final ClassDesc CD_ArrayType = ClassDesc.of("javax.lang.model.type.ArrayType");

  static final ClassDesc CD_CharSequence = ClassDesc.of("java.lang.CharSequence");

  static final ClassDesc CD_DeclaredType = ClassDesc.of("javax.lang.model.type.DeclaredType");

  static final ClassDesc CD_Element = ClassDesc.of("javax.lang.model.element.Element");

  static final ClassDesc CD_ExecutableElement = ClassDesc.of("javax.lang.model.element.ExecutableElement");

  static final ClassDesc CD_ModuleElement = ClassDesc.of("javax.lang.model.element.ModuleElement");

  static final ClassDesc CD_Name = ClassDesc.of("javax.lang.model.element.Name");

  static final ClassDesc CD_NoType = ClassDesc.of("javax.lang.model.type.NoType");

  static final ClassDesc CD_NullType = ClassDesc.of("javax.lang.model.type.NullType");

  static final ClassDesc CD_PackageElement = ClassDesc.of("javax.lang.model.element.PackageElement");

  static final ClassDesc CD_Parameterizable = ClassDesc.of("javax.lang.model.element.Parameterizable");

  static final ClassDesc CD_PrimitiveType = ClassDesc.of("javax.lang.model.type.PrimitiveType");

  static final ClassDesc CD_RecordComponentElement = ClassDesc.of("javax.lang.model.element.RecordComponentElement");
  
  static final ClassDesc CD_TypeElement = ClassDesc.of("javax.lang.model.element.TypeElement");

  static final ClassDesc CD_TypeParameterElement = ClassDesc.of("javax.lang.model.element.TypeParameterElement");

  static final ClassDesc CD_TypeKind = ClassDesc.of("javax.lang.model.type.TypeKind");

  static final ClassDesc CD_TypeMirror = ClassDesc.of("javax.lang.model.type.TypeMirror");

  static final ClassDesc CD_TypeVariable = ClassDesc.of("javax.lang.model.type.TypeVariable");

  static final ClassDesc CD_VariableElement = ClassDesc.of("javax.lang.model.element.VariableElement");
  
  static final ClassDesc CD_WildcardType = ClassDesc.of("javax.lang.model.type.WildcardType");

  private ConstantDescs() {
    super();
  }

}
