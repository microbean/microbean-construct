/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2025 microBean™.
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

// Even though this is in the java.lang.reflect package no reflection takes place; it's just an enum
import java.lang.reflect.AccessFlag;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;

import javax.lang.model.util.Elements.Origin;

import org.microbean.construct.Domain;

/**
 * A utility class for working with access flags.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @spec https://docs.oracle.com/javase/specs/jvms/se23/html/jvms-4.html#jvms-4.5-200-A.1 Java Virtual Machine
 * Specification, section 4.5
 *
 * @spec https://docs.oracle.com/javase/specs/jvms/se23/html/jvms-4.html#jvms-4.6-200-A.1 Java Virtual Machine
 * Specification, section 4.6
 *
 * @spec https://docs.oracle.com/javase/specs/jvms/se23/html/jvms-4.html#jvms-4.7 Java Virtual Machine Specification,
 * section 4.7
 */
public final class AccessFlags {

  // Flags pulled from the ASM library (https://gitlab.ow2.org/asm/asm/-/blob/master/asm/src/main/java/org/objectweb/asm/Opcodes.java):

  private static final int ASM_RECORD =       1 << 16;  // 0x10000 // class // (see for example https://github.com/raphw/byte-buddy/blob/byte-buddy-1.14.5/byte-buddy-dep/src/main/java/net/bytebuddy/pool/TypePool.java#L2949)

  private AccessFlags() {
    super();
  }

  /**
   * Returns the <dfn>access flags</dfn> for the supplied {@link Element}, which is presumed to have come from the
   * supplied {@link Domain}.
   *
   * @param e an {@link Element}; must not be {@code null}
   *
   * @param domain a {@link Domain}; must not be {@code null}
   *
   * @return the access flags for the supplied {@link Element}
   *
   * @exception NullPointerException if either argument is {@code null}
   *
   * @spec https://docs.oracle.com/javase/specs/jvms/se23/html/jvms-4.html#jvms-4.5-200-A.1 Java Virtual Machine
   * Specification, section 4.5
   *
   * @spec https://docs.oracle.com/javase/specs/jvms/se23/html/jvms-4.html#jvms-4.6-200-A.1 Java Virtual Machine
   * Specification, section 4.6
   *
   * @spec https://docs.oracle.com/javase/specs/jvms/se23/html/jvms-4.html#jvms-4.7 Java Virtual Machine Specification,
   * section 4.7
   */
  @SuppressWarnings("try")
  public static final int accessFlags(final Element e, final Domain domain) {
    int accessFlags = 0;
    ElementKind k;
    try (var lock = domain.lock()) {
      for (final Modifier m : e.getModifiers()) { // not sure this actually causes symbol completion; could be hoisted out of lock
        accessFlags |= accessFlagMask(m);
      }
      k = e.getKind();
      switch (k) {
      case METHOD:
        // Handle just bridge and varargs here; other stuff will happen later
        final ExecutableElement ee = (ExecutableElement)e;
        if (domain.bridge(ee)) {
          accessFlags |= AccessFlag.BRIDGE.mask();
        }
        if (ee.isVarArgs()) {
          accessFlags |= AccessFlag.VARARGS.mask();
        }
        break;
      case MODULE:
        // Handle just openness here; other stuff will happen later
        if (((ModuleElement)e).isOpen()) {
          accessFlags |= AccessFlag.OPEN.mask();
        }
        break;
      }
      accessFlags |= accessFlagMask(domain.origin(e));
    }
    accessFlags |= accessFlagMask(k);
    return accessFlags;
  }

  private static final int accessFlagMask(final Modifier m) {
    return switch (m) {
    case null -> throw new NullPointerException("m");
    case ABSTRACT -> AccessFlag.ABSTRACT.mask();
    case FINAL -> AccessFlag.FINAL.mask();
    case NATIVE -> AccessFlag.NATIVE.mask();
    case PRIVATE -> AccessFlag.PRIVATE.mask();
    case PROTECTED -> AccessFlag.PROTECTED.mask();
    case PUBLIC -> AccessFlag.PUBLIC.mask();
    case STATIC -> AccessFlag.STATIC.mask();
    case STRICTFP -> AccessFlag.STRICT.mask();
    case SYNCHRONIZED -> AccessFlag.SYNCHRONIZED.mask();
    case TRANSIENT -> AccessFlag.TRANSIENT.mask();
    case VOLATILE -> AccessFlag.VOLATILE.mask();
    case DEFAULT, NON_SEALED, SEALED -> 0; // pass through
    };
  }

  private static final int accessFlagMask(final ElementKind k) {
    return switch (k) {
    case null -> throw new NullPointerException("k");
    case ANNOTATION_TYPE -> AccessFlag.ANNOTATION.mask() | AccessFlag.INTERFACE.mask(); // see https://docs.oracle.com/javase/specs/jvms/se23/html/jvms-4.html#jvms-4.1-200-E.1
    case ENUM -> AccessFlag.ENUM.mask();
    case ENUM_CONSTANT -> AccessFlag.ENUM.mask(); // perhaps odd but see https://docs.oracle.com/javase/specs/jvms/se23/html/jvms-4.html#jvms-4.5-200-A.1
    case INTERFACE -> AccessFlag.INTERFACE.mask(); // AccessFlag.ABSTRACT.mask() needs to be in play too, but that's covered in accessFlagMask(Modifier)
    case MODULE -> AccessFlag.MODULE.mask();
    case RECORD -> ASM_RECORD; // Some bytecode libraries use this to divine "recordness"; AccessFlag doesn't expose it and the JVM spec doesn't define it
    case
      BINDING_VARIABLE,
      CLASS,
      CONSTRUCTOR,
      EXCEPTION_PARAMETER,
      FIELD,
      INSTANCE_INIT,
      LOCAL_VARIABLE,
      METHOD,
      OTHER,
      PACKAGE,
      PARAMETER,
      RECORD_COMPONENT,
      RESOURCE_VARIABLE,
      STATIC_INIT,
      TYPE_PARAMETER -> 0; // pass through
    };
  }

  private static final int accessFlagMask(final Origin o) {
    return switch (o) {
    case null -> throw new NullPointerException("o");
    case EXPLICIT -> 0;
    case MANDATED -> AccessFlag.MANDATED.mask();
    case SYNTHETIC -> AccessFlag.SYNTHETIC.mask();
    };
  }

}
