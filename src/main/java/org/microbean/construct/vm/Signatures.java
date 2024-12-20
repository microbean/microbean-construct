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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import org.microbean.construct.Domain;

/**
 * A utility class that provides <dfn>signatures</dfn> for {@link TypeMirror}s and {@link Element}s.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @spec https://docs.oracle.com/javase/specs/jvms/se23/html/jvms-4.html#jvms-4.7.9.1 Java Virtual Machine
 * Specification, section 4.7.9.1
 */
@SuppressWarnings("try")
public final class Signatures {

  private Signatures() {
    super();
  }

  /**
   * Returns a <dfn>signature</dfn> for the supplied {@link Element}.
   *
   * @param e the {@link Element} for which a signature should be returned; must not be {@code null}
   *
   * @param d a {@link Domain} from which the {@link Element} is presumed to have originated; must not be {@code null}
   *
   * @exception NullPointerException if either argument is {@code null}
   *
   * @exception IllegalArgumentException if {@code e} has an {@link ElementKind} that is either {@link
   * ElementKind#ANNOTATION_TYPE}, {@link ElementKind#BINDING_VARIABLE}, {@link ElementKind#EXCEPTION_PARAMETER}, {@link
   * ElementKind#LOCAL_VARIABLE}, {@link ElementKind#MODULE}, {@link ElementKind#OTHER}, {@link ElementKind#PACKAGE},
   * {@link ElementKind#RESOURCE_VARIABLE}, or {@link ElementKind#TYPE_PARAMETER}
   *
   * @return a non-{@code null} signature
   *
   * @see ElementKind
   *
   * @spec https://docs.oracle.com/javase/specs/jvms/se23/html/jvms-4.html#jvms-4.7.9.1 Java Virtual Machine
   * Specification, section 4.7.9.1
   */
  public static final String signature(final Element e, final Domain d) {
    try (var lock = d.lock()) {
      return switch (e.getKind()) {
      case
        CLASS,
        ENUM,
        INTERFACE,
        RECORD -> classSignature((TypeElement)e, d);
      case
        CONSTRUCTOR,
        INSTANCE_INIT,
        METHOD,
        STATIC_INIT -> methodSignature((ExecutableElement)e, d);
      case
        ENUM_CONSTANT,
        FIELD,
        PARAMETER,
        RECORD_COMPONENT -> fieldSignature(e, d);
      case
        ANNOTATION_TYPE,
        BINDING_VARIABLE,
        EXCEPTION_PARAMETER,
        LOCAL_VARIABLE,
        MODULE,
        OTHER,
        PACKAGE,
        RESOURCE_VARIABLE,
        TYPE_PARAMETER ->
        throw new IllegalArgumentException("e: " + e + "; kind: " + e.getKind());
      };
    }
  }

  private static final String classSignature(final TypeElement e, final Domain d) {
    // Precondition: under domain lock
    return switch (e.getKind()) {
    case CLASS, ENUM, INTERFACE, RECORD -> {
      if (!d.generic(e) && ((DeclaredType)e.getSuperclass()).getTypeArguments().isEmpty()) {
        boolean signatureRequired = false;
        for (final TypeMirror iface : e.getInterfaces()) {
          if (!((DeclaredType)iface).getTypeArguments().isEmpty()) {
            signatureRequired = true;
            break;
          }
        }
        if (!signatureRequired) {
          yield null;
        }
      }
      final StringBuilder sb = new StringBuilder();
      classSignature(e, sb, d);
      yield sb.toString();
    }
    default -> throw new IllegalArgumentException("e: " + e + "; kind: " + e.getKind());
    };
  }

  private static final void classSignature(final TypeElement e, final StringBuilder sb, final Domain d) {
    // Precondition: under domain lock
    switch (e.getKind()) {
    case CLASS, ENUM, INTERFACE, RECORD -> { // note: no ANNOTATION_TYPE on purpose
      typeParameters(e.getTypeParameters(), sb, d);
      final List<? extends TypeMirror> directSupertypes = d.directSupertypes(e.asType());
      if (directSupertypes.isEmpty()) {
        assert e.getQualifiedName().contentEquals("java.lang.Object") : "DeclaredType with no supertypes: " + e.asType();
      } else {
        final DeclaredType firstSupertype = (DeclaredType)directSupertypes.get(0);
        assert firstSupertype.getKind() == TypeKind.DECLARED;
        // "For an interface type with no direct super-interfaces, a type mirror representing java.lang.Object is
        // returned." Therefore in all situations, given a non-empty list of direct supertypes, the first element will
        // always be a non-interface class.
        assert !((TypeElement)firstSupertype.asElement()).getKind().isInterface() : "Contract violation";
        superclassSignature(firstSupertype, sb, d);
        superinterfaceSignatures(directSupertypes.subList(1, directSupertypes.size()), sb, d);
      }
      break;
    }
    default -> throw new IllegalArgumentException("e: " + e + "; kind: " + e.getKind());
    }
  }

  @SuppressWarnings("fallthrough")
  private static final String methodSignature(final ExecutableElement e, final Domain d) {
    // Precondition: under domain lock
    if (e.getKind().isExecutable()) {
      boolean throwsClauseRequired = false;
      for (final TypeMirror exceptionType : e.getThrownTypes()) {
        if (exceptionType.getKind() == TypeKind.TYPEVAR) {
          throwsClauseRequired = true;
          break;
        }
      }
      if (!throwsClauseRequired && !d.generic(e)) {
        final TypeMirror returnType = e.getReturnType();
        switch (returnType.getKind()) {
        case TYPEVAR:
          break;
        case DECLARED:
          if (!((DeclaredType)returnType).getTypeArguments().isEmpty()) {
            break;
          }
          // fall through
        default:
          boolean signatureRequired = false;
          for (final VariableElement p : e.getParameters()) {
            final TypeMirror parameterType = p.asType();
            switch (parameterType.getKind()) {
            case DECLARED:
              signatureRequired = !((DeclaredType)parameterType).getTypeArguments().isEmpty();
              break;
            case TYPEVAR:
              signatureRequired = true;
              break;
            default:
              break;
            }
          }
          if (!signatureRequired) {
            return null;
          }
        }
      }
      final StringBuilder sb = new StringBuilder();
      methodSignature(e, sb, throwsClauseRequired, d);
      return sb.toString();
    } else {
      throw new IllegalArgumentException("e: " + e + "; kind: " + e.getKind());
    }
  }

  private static final void methodSignature(final ExecutableElement e,
                                            final StringBuilder sb,
                                            final boolean throwsClauseRequired,
                                            final Domain d) {
    // Precondition: under domain lock
    if (!e.getKind().isExecutable()) {
      throw new IllegalArgumentException("e: " + e + "; kind: " + e.getKind());
    }
    typeParameters(e.getTypeParameters(), sb, d);
    sb.append('(');
    parameterSignatures(e.getParameters(), sb, d);
    sb.append(')');
    final TypeMirror returnType = e.getReturnType();
    if (returnType.getKind() == TypeKind.VOID) {
      sb.append('V');
    } else {
      typeSignature(returnType, sb, d);
    }
    if (throwsClauseRequired) {
      throwsSignatures(e.getThrownTypes(), sb, d);
    }
  }

  @SuppressWarnings("fallthrough")
  private static final String fieldSignature(final Element e, final Domain d) {
    // Precondition: under domain lock
    return switch (e.getKind()) {
    case ENUM_CONSTANT, FIELD, PARAMETER, RECORD_COMPONENT -> {
      final TypeMirror t = e.asType();
      switch (t.getKind()) {
      case DECLARED:
        if (((DeclaredType)t).getTypeArguments().isEmpty()) {
          // TODO: is this sufficient? Or do we, for example, have to examine the type's supertypes to see if *they*
          // "use" a parameterized type? Maybe we have to look at the enclosing type too? But if so, why only here, and
          // why not the same sort of thing for the return type of a method (see above)?
          yield null;
        }
        // fall through
      case TYPEVAR:
        final StringBuilder sb = new StringBuilder();
        fieldSignature(e, sb, d);
        yield sb.toString();
      default:
        // TODO: is this sufficient? Or do we, for example, have to examine the type's supertypes to see if *they*
        // "use" a parameterized type? Maybe we have to look at the enclosing type too? But if so, why only here, and
        // why not the same sort of thing for the return type of a method (see above)?
        yield null;
      }
    }
    default -> throw new IllegalArgumentException("e: " + e + "; kind: " + e.getKind());
    };
  }

  private static final void fieldSignature(final Element e, final StringBuilder sb, final Domain d) {
    // Precondition: under domain lock
    switch (e.getKind()) {
    case ENUM_CONSTANT, FIELD, PARAMETER, RECORD_COMPONENT -> typeSignature(e.asType(), sb, d);
    default -> throw new IllegalArgumentException("e: " + e);
    }
  }

  private static final void parameterSignatures(final List<? extends VariableElement> ps,
                                                final StringBuilder sb,
                                                final Domain d) {
    // Precondition: under domain lock
    for (final VariableElement p : ps) {
      if (p.getKind() != ElementKind.PARAMETER) {
        throw new IllegalArgumentException("ps: " + ps);
      }
      typeSignature(p.asType(), sb, d);
    }
  }

  private static final void throwsSignatures(final List<? extends TypeMirror> ts, final StringBuilder sb, final Domain d) {
    // Precondition: under domain lock
    for (final TypeMirror t : ts) {
      sb.append(switch (t.getKind()) {
        case DECLARED, TYPEVAR -> '^';
        default -> throw new IllegalArgumentException("ts: " + ts);
        });
      typeSignature(t, sb, d);
    }
  }

  private static final void typeParameters(final List<? extends TypeParameterElement> tps,
                                           final StringBuilder sb,
                                           final Domain d) {
    if (!tps.isEmpty()) {
      sb.append('<');
      // Precondition: under domain lock
      for (final TypeParameterElement tp : tps) {
        switch (tp.getKind()) {
        case TYPE_PARAMETER -> typeParameter(tp, sb, d);
        default -> throw new IllegalArgumentException("tps: " + tps);
        }
      }
      sb.append('>');
    }
  }

  private static final void typeParameter(final TypeParameterElement e, final StringBuilder sb, final Domain d) {
    // Precondition: under domain lock
    if (e.getKind() != ElementKind.TYPE_PARAMETER) {
      throw new IllegalArgumentException("e: " + e);
    }
    final List<? extends TypeMirror> bounds = e.getBounds();
    sb.append(e.getSimpleName()).append(':');
    if (bounds.isEmpty()) {
      sb.append("java.lang.Object");
    } else {
      classBound(bounds.get(0), sb, d);
    }
    interfaceBounds(bounds.subList(1, bounds.size()), sb, d);
  }

  private static final void classBound(final TypeMirror t, final StringBuilder sb, final Domain d) {
    // Precondition: under domain lock
    if (t.getKind() != TypeKind.DECLARED) {
      throw new IllegalArgumentException("t: " + t);
    }
    typeSignature(t, sb, d);
  }

  private static final void interfaceBounds(final List<? extends TypeMirror> ts, final StringBuilder sb, final Domain d) {
    // Precondition: under domain lock
    for (final TypeMirror t : ts) {
      interfaceBound(t, sb, d);
    }
  }

  @SuppressWarnings("fallthrough")
  private static final void interfaceBound(final TypeMirror t, final StringBuilder sb, final Domain d) {
    // Precondition: under domain lock
    switch (t.getKind()) {
    case DECLARED:
      if (((DeclaredType)t).asElement().getKind().isInterface()) {
        sb.append(':');
        typeSignature(t, sb, d);
        return;
      }
      // fall through
    default:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  private static final void superclassSignature(final TypeMirror t, final StringBuilder sb, final Domain d) {
    classTypeSignature(t, sb, d);
  }

  private static final void superinterfaceSignatures(final List<? extends TypeMirror> ts,
                                                     final StringBuilder sb,
                                                     final Domain d) {
    // Precondition: under domain lock
    for (final TypeMirror t : ts) {
      superinterfaceSignature(t, sb, d);
    }
  }

  @SuppressWarnings("fallthrough")
  private static final void superinterfaceSignature(final TypeMirror t, final StringBuilder sb, final Domain d) {
    // Precondition: under domain lock
    switch (t.getKind()) {
    case DECLARED:
      if (((DeclaredType)t).asElement().getKind().isInterface()) {
        classTypeSignature(t, sb, d);
        return;
      }
      // fall through
    default:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  /**
   * Returns a <dfn>signature</dfn> for the supplied {@link TypeMirror}.
   *
   * @param t the {@link TypeMirror} for which a signature should be returned; must not be {@code null}
   *
   * @param d a {@link Domain} from which the {@link TypeMirror} is presumed to have originated; must not be {@code
   * null}
   *
   * @exception NullPointerException if either argument is {@code null}
   *
   * @exception IllegalArgumentException if {@code t} has an {@link TypeKind} that is either {@link TypeKind#ERROR},
   * {@link TypeKind#EXECUTABLE}, {@link TypeKind#INTERSECTION}, {@link TypeKind#MODULE}, {@link TypeKind#NONE}, {@link
   * TypeKind#NULL}, {@link TypeKind#OTHER}, {@link TypeKind#PACKAGE}, {@link TypeKind#UNION}, {@link TypeKind#VOID}, or
   * {@link TypeKind#WILDCARD}
   *
   * @return a non-{@code null} signature
   *
   * @see TypeKind
   *
   * @spec https://docs.oracle.com/javase/specs/jvms/se23/html/jvms-4.html#jvms-4.7.9.1 Java Virtual Machine
   * Specification, section 4.7.9.1
   */
  public static final String signature(final TypeMirror t, final Domain d) {
    final StringBuilder sb = new StringBuilder();
    try (var lock = d.lock()) {
      typeSignature(t, sb, d);
    }
    return sb.toString();
  }

  private static final void typeSignature(final TypeMirror t, final StringBuilder sb, final Domain d) {
    // Precondition: under domain lock
    switch (t.getKind()) {
    case ARRAY    -> typeSignature(((ArrayType)t).getComponentType(), sb.append('['), d); // recursive
    case BOOLEAN  -> sb.append('Z');
    case BYTE     -> sb.append('B');
    case CHAR     -> sb.append('C');
    case DECLARED -> classTypeSignature((DeclaredType)t, sb, d);
    case DOUBLE   -> sb.append('D');
    case FLOAT    -> sb.append('F');
    case INT      -> sb.append('I');
    case LONG     -> sb.append('J');
    case SHORT    -> sb.append('S');
    case TYPEVAR  -> sb.append('T').append(((TypeVariable)t).asElement().getSimpleName()).append(';');
    default       -> throw new IllegalArgumentException("t: " + t);
    }
  }

  private static final void classTypeSignature(final TypeMirror t, final StringBuilder sb, final Domain d) {
    // Precondition: under domain lock
    switch (t.getKind()) {
    case NONE:
      return;
    case DECLARED:
      break;
    default:
      throw new IllegalArgumentException("t: " + t);
    }
    final DeclaredType dt = (DeclaredType)t;

    // Build a deque of elements from the package to the (possibly inner or nested) class.
    final Deque<Element> dq = new ArrayDeque<>();
    Element e = dt.asElement();
    while (e != null && e.getKind() != ElementKind.MODULE) {
      dq.push(e);
      e = e.getEnclosingElement();
    }

    sb.append('L');

    final Iterator<Element> i = dq.iterator();
    while (i.hasNext()) {
      e = i.next();
      switch (e.getKind()) {
      case PACKAGE:
        // java.lang becomes java/lang
        sb.append(((PackageElement)e).getQualifiedName().toString().replace('.', '/'));
        assert i.hasNext();
        sb.append('/');
        break;
      case ANNOTATION_TYPE:
      case CLASS:
      case ENUM:
      case INTERFACE:
      case RECORD:
        // Outer.Inner remains Outer.Inner (i.e. not Outer$Inner or Outer/Inner)
        sb.append(e.getSimpleName());
        if (i.hasNext()) {
          sb.append('.');
        }
        break;
      default:
        // note that a method could fall in here; we just skip it
        break;
      }
      i.remove();
    }
    assert dq.isEmpty();

    // Now for the type arguments
    final List<? extends TypeMirror> typeArguments = dt.getTypeArguments();
    if (!typeArguments.isEmpty()) {
      sb.append('<');
      for (final TypeMirror ta : typeArguments) {
        switch (ta.getKind()) {
        case WILDCARD:
          final WildcardType w = (WildcardType)ta;
          final TypeMirror superBound = w.getSuperBound();
          if (superBound == null) {
            final TypeMirror extendsBound = w.getExtendsBound();
            if (extendsBound == null) {
              sb.append('*'); // I guess?
            } else {
              sb.append('+');
              typeSignature(extendsBound, sb, d);
            }
          } else {
            sb.append('-');
            typeSignature(superBound, sb, d);
          }
          break;
        default:
          typeSignature(ta, sb, d);
          break;
        }
      }
      sb.append('>');
    }

    sb.append(';');
  }

}
