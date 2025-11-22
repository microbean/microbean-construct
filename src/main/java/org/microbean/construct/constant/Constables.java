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
import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;

import java.util.List;
import java.util.Optional;

import javax.lang.model.AnnotatedConstruct;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import org.microbean.construct.Domain;

import static java.lang.constant.ConstantDescs.BSM_INVOKE;
import static java.lang.constant.ConstantDescs.NULL;

import static java.lang.constant.DirectMethodHandleDesc.Kind.VIRTUAL;

import static org.microbean.construct.constant.ConstantDescs.CD_ArrayType;
import static org.microbean.construct.constant.ConstantDescs.CD_CharSequence;
import static org.microbean.construct.constant.ConstantDescs.CD_DeclaredType;
import static org.microbean.construct.constant.ConstantDescs.CD_Element;
import static org.microbean.construct.constant.ConstantDescs.CD_ExecutableElement;
import static org.microbean.construct.constant.ConstantDescs.CD_ModuleElement;
import static org.microbean.construct.constant.ConstantDescs.CD_Name;
import static org.microbean.construct.constant.ConstantDescs.CD_NoType;
import static org.microbean.construct.constant.ConstantDescs.CD_NullType;
import static org.microbean.construct.constant.ConstantDescs.CD_PackageElement;
import static org.microbean.construct.constant.ConstantDescs.CD_Parameterizable;
import static org.microbean.construct.constant.ConstantDescs.CD_PrimitiveType;
import static org.microbean.construct.constant.ConstantDescs.CD_RecordComponentElement;
import static org.microbean.construct.constant.ConstantDescs.CD_TypeElement;
import static org.microbean.construct.constant.ConstantDescs.CD_TypeKind;
import static org.microbean.construct.constant.ConstantDescs.CD_TypeParameterElement;
import static org.microbean.construct.constant.ConstantDescs.CD_TypeMirror;
import static org.microbean.construct.constant.ConstantDescs.CD_TypeVariable;
import static org.microbean.construct.constant.ConstantDescs.CD_WildcardType;

/**
 * A utility class that returns nominal descriptors for constructs.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see #describe(Element, Domain)
 *
 * @see #describe(TypeMirror, Domain)
 */
@SuppressWarnings("try")
public final class Constables {

  private Constables() {
    super();
  }

  /**
   * Returns a nominal descriptor for the supplied argument, presuming it to have originated from the supplied {@link
   * Domain}, or an {@linkplain Optional#empty() empty} {@link Optional} if the supplied argument cannot be described.
   *
   * @param n the argument; may be {@code null}
   *
   * @param d the {@link Domain} from which the argument originated; must not be {@code null}
   *
   * @return a non-{@code null} {@link Optional}
   *
   * @exception NullPointerException if {@code d} is {@code null}
   */
  public static final Optional<? extends ConstantDesc> describe(final Name n, final Domain d) {
    return switch (n) {
    case null -> Optional.of(NULL);
    case Constable c -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd); // future proofing?
    default -> (d instanceof Constable c ? c.describeConstable() : Optional.<ConstantDesc>empty())
      .map(domainDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                MethodHandleDesc.ofMethod(VIRTUAL,
                                                                          ClassDesc.of(Domain.class.getName()),
                                                                          "name",
                                                                          MethodTypeDesc.of(CD_Name,
                                                                                            CD_CharSequence)),
                                                domainDesc,
                                                d.toString(n)));
    };
  }

  /**
   * Returns a nominal descriptor for the supplied argument, presuming it to have originated from the supplied {@link
   * Domain}, or an {@linkplain Optional#empty() empty} {@link Optional} if the supplied argument cannot be described.
   *
   * @param ac the argument; may be {@code null}
   *
   * @param d the {@link Domain} from which the argument originated; must not be {@code null}
   *
   * @return a non-{@code null} {@link Optional}
   *
   * @exception NullPointerException if {@code d} is {@code null}
   */
  public static final Optional<? extends ConstantDesc> describe(final AnnotatedConstruct ac, final Domain d) {
    return switch (ac) {
    case null -> Optional.of(NULL);
    case Constable c -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd); // future proofing?
    case Element e -> describe(e, d);
    case TypeMirror t -> describe(t, d);
    default -> Optional.empty();
    };
  }

  /**
   * Returns a nominal descriptor for the supplied argument, presuming it to have originated from the supplied {@link
   * Domain}, or an {@linkplain Optional#empty() empty} {@link Optional} if the supplied argument cannot be described.
   *
   * @param e the argument; may be {@code null}
   *
   * @param d the {@link Domain} from which the argument originated; must not be {@code null}
   *
   * @return a non-{@code null} {@link Optional}
   *
   * @exception NullPointerException if {@code d} is {@code null}
   */
  public static final Optional<? extends ConstantDesc> describe(final Element e, final Domain d) {
    return switch (e) {
    case null -> Optional.of(NULL);
    case Constable c -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd); // future proofing?
    default -> {
      try (var lock = d.lock()) {
        yield switch (e.getKind()) {
        case ANNOTATION_TYPE, CLASS, ENUM, INTERFACE, RECORD -> describe((TypeElement)e, d);
        case BINDING_VARIABLE, EXCEPTION_PARAMETER, LOCAL_VARIABLE, OTHER, RESOURCE_VARIABLE ->
          // No way to get these from javax.lang.model.Elements
          Optional.empty();
        case CONSTRUCTOR, INSTANCE_INIT, METHOD, STATIC_INIT -> describe((ExecutableElement)e, d);
        case ENUM_CONSTANT, FIELD, PARAMETER -> describe((VariableElement)e, d);
        case MODULE -> describe((ModuleElement)e, d);
        case PACKAGE -> describe((PackageElement)e, d);
        case RECORD_COMPONENT -> describe((RecordComponentElement)e, d);
        case TYPE_PARAMETER -> describe((TypeParameterElement)e, d);
        };
      }
    }
    };
  }

  /**
   * Returns a nominal descriptor for the supplied argument, presuming it to have originated from the supplied {@link
   * Domain}, or an {@linkplain Optional#empty() empty} {@link Optional} if the supplied argument cannot be described.
   *
   * @param e the argument; may be {@code null}
   *
   * @param d the {@link Domain} from which the argument originated; must not be {@code null}
   *
   * @return a non-{@code null} {@link Optional}
   *
   * @exception NullPointerException if {@code d} is {@code null}
   */
  public static final Optional<? extends ConstantDesc> describe(final ExecutableElement e, final Domain d) {
    return switch (e) {
    case null -> Optional.of(NULL);
    case Constable c -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd); // future proofing?
    default -> {
      final ConstantDesc domainDesc = d instanceof Constable c ? c.describeConstable().orElse(null) : null;
      if (domainDesc == null) {
        yield Optional.empty();
      }
      try (var lock = d.lock()) {
        // Trying to do this via flatMap etc. simply will not work because type inference does not work properly, even
        // with hints/coercion. Feel free to try again, but make sure you keep this implementation around to revert
        // back to.
        final List<? extends VariableElement> parameters = e.getParameters();
        final int parameterCount = parameters.size();
        final ConstantDesc[] args = new ConstantDesc[5 + parameterCount];
        args[0] =
          MethodHandleDesc.ofMethod(VIRTUAL,
                                    ClassDesc.of(Domain.class.getName()),
                                    "executableElement",
                                    MethodTypeDesc.of(CD_ExecutableElement,
                                                      CD_TypeElement,
                                                      CD_TypeMirror,
                                                      CD_CharSequence,
                                                      CD_TypeMirror.arrayType()));
        args[1] = domainDesc;
        args[2] = describe(e.getEnclosingElement(), d).orElse(null);
        if (args[2] == null) {
          yield Optional.empty();
        }
        args[3] = describe(e.getReturnType(), d).orElse(null);
        if (args[3] == null) {
          yield Optional.empty();
        }
        args[4] = describe(e.getSimpleName(), d).orElse(null);
        if (args[4] == null) {
          yield Optional.empty();
        }
        for (int i = 0; i < parameterCount; i++) {
          int index = i + 5;
          args[index] = describe(parameters.get(i).asType(), d).orElse(null);
          if (args[index] == null) {
            yield Optional.empty();
          }
        }
        yield Optional.of(DynamicConstantDesc.of(BSM_INVOKE, args));
      }
    }
    };
  }

  /**
   * Returns a nominal descriptor for the supplied argument, presuming it to have originated from the supplied {@link
   * Domain}, or an {@linkplain Optional#empty() empty} {@link Optional} if the supplied argument cannot be described.
   *
   * @param e the argument; may be {@code null}
   *
   * @param d the {@link Domain} from which the argument originated; must not be {@code null}
   *
   * @return a non-{@code null} {@link Optional}
   *
   * @exception NullPointerException if {@code d} is {@code null}
   */
  public static final Optional<? extends ConstantDesc> describe(final ModuleElement e, final Domain d) {
    return switch (e) {
    case null -> Optional.of(NULL);
    case Constable c -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd); // future proofing?
    default -> describe(e.getQualifiedName(), d) // getQualifiedName() does not cause symbol completion
      .map(nameDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                              MethodHandleDesc.ofMethod(VIRTUAL,
                                                                        ClassDesc.of(Domain.class.getName()),
                                                                        "moduleElement",
                                                                        MethodTypeDesc.of(CD_ModuleElement,
                                                                                          CD_CharSequence)),
                                              ((Constable)d).describeConstable().orElseThrow(),
                                              nameDesc));
    };
  }

  /**
   * Returns a nominal descriptor for the supplied argument, presuming it to have originated from the supplied {@link
   * Domain}, or an {@linkplain Optional#empty() empty} {@link Optional} if the supplied argument cannot be described.
   *
   * @param e the argument; may be {@code null}
   *
   * @param d the {@link Domain} from which the argument originated; must not be {@code null}
   *
   * @return a non-{@code null} {@link Optional}
   *
   * @exception NullPointerException if {@code d} is {@code null}
   */
  public static final Optional<? extends ConstantDesc> describe(final PackageElement e, final Domain d) {
    return switch (e) {
    case null -> Optional.of(NULL);
    case Constable c -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd); // future proofing?
    default -> describe(e.getQualifiedName(), d) // getQualifiedName() does not cause symbol completion
      .map(nameDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                              MethodHandleDesc.ofMethod(VIRTUAL,
                                                                        ClassDesc.of(Domain.class.getName()),
                                                                        "packageElement",
                                                                        MethodTypeDesc.of(CD_PackageElement,
                                                                                          CD_CharSequence)),
                                              ((Constable)d).describeConstable().orElseThrow(),
                                              nameDesc));
    };
  }

  /**
   * Returns a nominal descriptor for the supplied argument, presuming it to have originated from the supplied {@link
   * Domain}, or an {@linkplain Optional#empty() empty} {@link Optional} if the supplied argument cannot be described.
   *
   * @param e the argument; may be {@code null}
   *
   * @param d the {@link Domain} from which the argument originated; must not be {@code null}
   *
   * @return a non-{@code null} {@link Optional}
   *
   * @exception NullPointerException if {@code d} is {@code null}
   */
  public static final Optional<? extends ConstantDesc> describe(final TypeElement e, final Domain d) {
    return switch (e) {
    case null -> Optional.of(NULL);
    case Constable c -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd); // future proofing?
    default -> describe(e.getQualifiedName(), d) // getQualifiedName() does not cause symbol completion
      .map(nameDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                              MethodHandleDesc.ofMethod(VIRTUAL,
                                                                        ClassDesc.of(Domain.class.getName()),
                                                                        "typeElement",
                                                                        MethodTypeDesc.of(CD_TypeElement,
                                                                                          CD_CharSequence)),
                                              ((Constable)d).describeConstable().orElseThrow(),
                                              nameDesc));
    };
  }

  /**
   * Returns a nominal descriptor for the supplied argument, presuming it to have originated from the supplied {@link
   * Domain}, or an {@linkplain Optional#empty() empty} {@link Optional} if the supplied argument cannot be described.
   *
   * @param e the argument; may be {@code null}
   *
   * @param d the {@link Domain} from which the argument originated; must not be {@code null}
   *
   * @return a non-{@code null} {@link Optional}
   *
   * @exception NullPointerException if {@code d} is {@code null}
   */
  public static final Optional<? extends ConstantDesc> describe(final TypeParameterElement e, final Domain d) {
    return switch (e) {
    case null -> Optional.of(NULL);
    case Constable c -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd); // future proofing?
    default -> {
      try (var lock = d.lock()) {
        yield describe(e.getEnclosingElement(), d)
          .flatMap(parameterizableDesc -> describe(e.getSimpleName(), d)
                   .map(nameDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                           MethodHandleDesc.ofMethod(VIRTUAL,
                                                                                     ClassDesc.of(Domain.class.getName()),
                                                                                     "typeParameterElement",
                                                                                     MethodTypeDesc.of(CD_TypeParameterElement,
                                                                                                       CD_Parameterizable,
                                                                                                       CD_Name)),
                                                           ((Constable)d).describeConstable().orElseThrow(),
                                                           parameterizableDesc,
                                                           nameDesc)));
      }
    }
    };
  }

  /**
   * Returns a nominal descriptor for the supplied argument, presuming it to have originated from the supplied {@link
   * Domain}, or an {@linkplain Optional#empty() empty} {@link Optional} if the supplied argument cannot be described.
   *
   * @param e the argument; may be {@code null}
   *
   * @param d the {@link Domain} from which the argument originated; must not be {@code null}
   *
   * @return a non-{@code null} {@link Optional}
   *
   * @exception NullPointerException if {@code d} is {@code null}
   */
  public static final Optional<? extends ConstantDesc> describe(final RecordComponentElement e, final Domain d) {
    return switch (e) {
    case null -> Optional.of(NULL);
    case Constable c -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd); // future proofing?
    default -> {
      try (var lock = d.lock()) {
        yield describe((TypeElement)e.getEnclosingElement(), d)
          .map(executableDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                        MethodHandleDesc.ofMethod(VIRTUAL,
                                                                                  ClassDesc.of(Domain.class.getName()),
                                                                                  "recordComponentElement",
                                                                                  MethodTypeDesc.of(CD_RecordComponentElement,
                                                                                                    CD_ExecutableElement)),
                                                        ((Constable)d).describeConstable().orElseThrow(),
                                                        executableDesc));
      }
    }
    };
  }

  /**
   * Returns a nominal descriptor for the supplied argument, presuming it to have originated from the supplied {@link
   * Domain}, or an {@linkplain Optional#empty() empty} {@link Optional} if the supplied argument cannot be described.
   *
   * @param e the argument; may be {@code null}
   *
   * @param d the {@link Domain} from which the argument originated; must not be {@code null}
   *
   * @return a non-{@code null} {@link Optional}
   *
   * @exception NullPointerException if {@code d} is {@code null}
   */
  public static final Optional<? extends ConstantDesc> describe(final VariableElement e, final Domain d) {
    return switch (e) {
    case null -> Optional.of(NULL);
    case Constable c -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd); // future proofing?
    default -> {
      try (var lock = d.lock()) {
        yield describe(e.getSimpleName(), d)
          .flatMap(nameDesc -> describe(e.getEnclosingElement(), d)
                   .map(enclosingElementDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                                       MethodHandleDesc.ofMethod(VIRTUAL,
                                                                                                 ClassDesc.of(Domain.class.getName()),
                                                                                                 "variableElement",
                                                                                                 MethodTypeDesc.of(CD_Element,
                                                                                                                   CD_CharSequence)),
                                                                       ((Constable)d).describeConstable().orElseThrow(),
                                                                       nameDesc)));
      }
    }
    };
  }

  /**
   * Returns a nominal descriptor for the supplied argument, presuming it to have originated from the supplied {@link
   * Domain}, or an {@linkplain Optional#empty() empty} {@link Optional} if the supplied argument cannot be described.
   *
   * @param t the argument; may be {@code null}
   *
   * @param d the {@link Domain} from which the argument originated; must not be {@code null}
   *
   * @return a non-{@code null} {@link Optional}
   *
   * @exception NullPointerException if {@code d} is {@code null}
   */
  public static final Optional<? extends ConstantDesc> describe(final TypeMirror t, final Domain d) {
    return switch (t) {
    case null -> Optional.of(NULL);
    case Constable c -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd); // future proofing?
    default -> {
      try (var lock = d.lock()) {
        yield switch (t.getKind()) {
        case ARRAY -> describe((ArrayType)t, d);
        case BOOLEAN, BYTE, CHAR, DOUBLE, FLOAT, INT, LONG, SHORT -> describe((PrimitiveType)t, d);
        case DECLARED -> describe((DeclaredType)t, d);
        case EXECUTABLE, INTERSECTION, UNION -> Optional.empty(); // No way to get these from javax.lang.model.util.Types
        case ERROR, OTHER -> Optional.empty();
        case MODULE, NONE, PACKAGE, VOID -> describe((NoType)t, d);
        case NULL -> describe((NullType)t, d);
        case TYPEVAR -> describe((TypeVariable)t, d); // Prefer working with TypeParameterElement instead
        case WILDCARD -> describe((WildcardType)t, d);
        };
      }
    }
    };
  }

  /**
   * Returns a nominal descriptor for the supplied argument, presuming it to have originated from the supplied {@link
   * Domain}, or an {@linkplain Optional#empty() empty} {@link Optional} if the supplied argument cannot be described.
   *
   * @param t the argument; may be {@code null}
   *
   * @param d the {@link Domain} from which the argument originated; must not be {@code null}
   *
   * @return a non-{@code null} {@link Optional}
   *
   * @exception NullPointerException if {@code d} is {@code null}
   */
  public static final Optional<? extends ConstantDesc> describe(final ArrayType t, final Domain d) {
    return switch (t) {
    case null -> Optional.of(NULL);
    case Constable c -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd); // future proofing?
    default -> {
      try (var lock = d.lock()) {
        yield describe(t.getComponentType(), d)
          .map(componentTypeDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                           MethodHandleDesc.ofMethod(VIRTUAL,
                                                                                     ClassDesc.of(Domain.class.getName()),
                                                                                     "arrayTypeOf",
                                                                                     MethodTypeDesc.of(CD_ArrayType,
                                                                                                       CD_TypeMirror)),
                                                           ((Constable)d).describeConstable().orElseThrow(),
                                                           componentTypeDesc));
      }
    }
    };
  }

  /**
   * Returns a nominal descriptor for the supplied argument, presuming it to have originated from the supplied {@link
   * Domain}, or an {@linkplain Optional#empty() empty} {@link Optional} if the supplied argument cannot be described.
   *
   * @param t the argument; may be {@code null}
   *
   * @param d the {@link Domain} from which the argument originated; must not be {@code null}
   *
   * @return a non-{@code null} {@link Optional}
   *
   * @exception NullPointerException if {@code d} is {@code null}
   */
  public static final Optional<? extends ConstantDesc> describe(final DeclaredType t, final Domain d) {
    return switch (t) {
    case null -> Optional.of(NULL);
    case Constable c -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd); // future proofing?
    default -> {
      final ConstantDesc domainDesc = d instanceof Constable constableDomain ? constableDomain.describeConstable().orElse(null) : null;
      if (domainDesc == null) {
        yield Optional.empty();
      }
      try (var lock = d.lock()) {
        yield switch (t.getKind()) {
        case DECLARED -> {
          final List<? extends TypeMirror> typeArguments = t.getTypeArguments();
          final int typeArgumentCount = typeArguments.size();
          final ConstantDesc[] args = new ConstantDesc[4 + typeArgumentCount];
          final TypeMirror enclosingType = t.getEnclosingType();
          // Call 
          args[0] = MethodHandleDesc.ofMethod(VIRTUAL,
                                              ClassDesc.of(Domain.class.getName()),
                                              "declaredType",
                                              MethodTypeDesc.of(CD_DeclaredType,
                                                                CD_DeclaredType,
                                                                CD_TypeElement,
                                                                CD_TypeMirror.arrayType()));
          args[1] = domainDesc;
          args[2] = enclosingType.getKind() == TypeKind.NONE ? NULL : describe(enclosingType, d).orElse(null);
          if (args[2] == null) {
            yield Optional.empty();
          }
          args[3] = describe((TypeElement)t.asElement(), d).orElse(null);
          if (args[3] == null) {
            yield Optional.empty();
          }
          for (int i = 0; i < typeArgumentCount; i++) {
            final int index = i + 4;
            args[index] = describe(typeArguments.get(i), d).orElse(null);
            if (args[index] == null) {
              yield Optional.empty();
            }
          }
          yield Optional.of(DynamicConstantDesc.of(BSM_INVOKE, args));
        }
        default -> Optional.empty(); // could be an error type
        };
      }
    }
    };
  }

  /**
   * Returns a nominal descriptor for the supplied argument, presuming it to have originated from the supplied {@link
   * Domain}, or an {@linkplain Optional#empty() empty} {@link Optional} if the supplied argument cannot be described.
   *
   * @param t the argument; may be {@code null}
   *
   * @param d the {@link Domain} from which the argument originated; must not be {@code null}
   *
   * @return a non-{@code null} {@link Optional}
   *
   * @exception NullPointerException if {@code d} is {@code null}
   */
  public static final Optional<? extends ConstantDesc> describe(final NoType t, final Domain d) {
    return switch (t) {
    case null -> Optional.of(NULL);
    case Constable c -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd); // future proofing?
    default -> {
      final ConstantDesc domainDesc = d instanceof Constable c ? c.describeConstable().orElse(null) : null;
      if (domainDesc == null) {
        yield Optional.empty();
      }
      try (var lock = d.lock()) {
        yield t.getKind().describeConstable()
          .map(typeKindDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                      MethodHandleDesc.ofMethod(VIRTUAL,
                                                                                ClassDesc.of(Domain.class.getName()),
                                                                                "noType",
                                                                                MethodTypeDesc.of(CD_NoType,
                                                                                                  CD_TypeKind)),
                                                      domainDesc,
                                                      typeKindDesc));
      }
    }
    };
  }

  /**
   * Returns a nominal descriptor for the supplied argument, presuming it to have originated from the supplied {@link
   * Domain}, or an {@linkplain Optional#empty() empty} {@link Optional} if the supplied argument cannot be described.
   *
   * @param t the argument; may be {@code null}
   *
   * @param d the {@link Domain} from which the argument originated; must not be {@code null}
   *
   * @return a non-{@code null} {@link Optional}
   *
   * @exception NullPointerException if {@code d} is {@code null}
   */
  public static final Optional<? extends ConstantDesc> describe(final NullType t, final Domain d) {
    return switch (t) {
    case null -> Optional.of(NULL);
    case Constable c -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd); // future proofing?
    default -> (d instanceof Constable c ? c.describeConstable() : Optional.<ConstantDesc>empty())
      .map(domainDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                MethodHandleDesc.ofMethod(VIRTUAL,
                                                                          ClassDesc.of(Domain.class.getName()),
                                                                          "nullType",
                                                                          MethodTypeDesc.of(CD_NullType)),
                                                domainDesc));
    };
  }

  /**
   * Returns a nominal descriptor for the supplied argument, presuming it to have originated from the supplied {@link
   * Domain}, or an {@linkplain Optional#empty() empty} {@link Optional} if the supplied argument cannot be described.
   *
   * @param t the argument; may be {@code null}
   *
   * @param d the {@link Domain} from which the argument originated; must not be {@code null}
   *
   * @return a non-{@code null} {@link Optional}
   *
   * @exception NullPointerException if {@code d} is {@code null}
   */
  public static final Optional<? extends ConstantDesc> describe(final PrimitiveType t, final Domain d) {
    return switch (t) {
    case null -> Optional.of(NULL);
    case Constable c -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd); // future proofing?
    default -> {
      final ConstantDesc domainDesc = d instanceof Constable constableDomain ? constableDomain.describeConstable().orElse(null) : null;
      if (domainDesc == null) {
        yield Optional.empty();
      }
      try (var lock = d.lock()) {
        yield t.getKind().describeConstable()
          .map(typeKindDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                      MethodHandleDesc.ofMethod(VIRTUAL,
                                                                                ClassDesc.of(Domain.class.getName()),
                                                                                "primitiveType",
                                                                                MethodTypeDesc.of(CD_PrimitiveType,
                                                                                                  CD_TypeKind)),
                                                      domainDesc,
                                                      typeKindDesc));
      }
    }
    };
  }

  /**
   * Returns a nominal descriptor for the supplied argument, presuming it to have originated from the supplied {@link
   * Domain}, or an {@linkplain Optional#empty() empty} {@link Optional} if the supplied argument cannot be described.
   *
   * @param t the argument; may be {@code null}
   *
   * @param d the {@link Domain} from which the argument originated; must not be {@code null}
   *
   * @return a non-{@code null} {@link Optional}
   *
   * @exception NullPointerException if {@code d} is {@code null}
   */
  public static final Optional<? extends ConstantDesc> describe(final TypeVariable t, final Domain d) {
    return switch (t) {
    case null -> Optional.of(NULL);
    case Constable c -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd); // future proofing?
    default -> {
      final ConstantDesc domainDesc = d instanceof Constable constableDomain ? constableDomain.describeConstable().orElse(null) : null;
      if (domainDesc == null) {
        yield Optional.empty();
      }
      try (var lock = d.lock()) {
        final TypeParameterElement e = (TypeParameterElement)t.asElement();
        final ConstantDesc parameterizableDesc = describe(e.getEnclosingElement(), d).orElse(null);
        if (parameterizableDesc == null) {
          yield Optional.empty();
        }
        final String name = d.toString(e.getSimpleName());
        yield Optional.of(DynamicConstantDesc.of(BSM_INVOKE,
                                                 MethodHandleDesc.ofMethod(VIRTUAL,
                                                                           ClassDesc.of(Domain.class.getName()),
                                                                           "typeVariable",
                                                                           MethodTypeDesc.of(CD_TypeVariable,
                                                                                             CD_Parameterizable,
                                                                                             CD_CharSequence)),
                                                 domainDesc,
                                                 parameterizableDesc,
                                                 name));
      }
    }
    };
  }

  /**
   * Returns a nominal descriptor for the supplied argument, presuming it to have originated from the supplied {@link
   * Domain}, or an {@linkplain Optional#empty() empty} {@link Optional} if the supplied argument cannot be described.
   *
   * @param t the argument; may be {@code null}
   *
   * @param d the {@link Domain} from which the argument originated; must not be {@code null}
   *
   * @return a non-{@code null} {@link Optional}
   *
   * @exception NullPointerException if {@code d} is {@code null}
   */
  public static final Optional<? extends ConstantDesc> describe(final WildcardType t, final Domain d) {
    return switch (t) {
    case null -> Optional.of(NULL);
    case Constable c -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd); // future proofing?
    default -> {
      try (var lock = d.lock()) {
        yield describe(t.getExtendsBound(), d)
          .flatMap(domainDesc -> describe(t.getExtendsBound(), d)
                   .flatMap(extendsBoundDesc -> describe(t.getSuperBound(), d)
                            .map(superBoundDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                                          MethodHandleDesc.ofMethod(VIRTUAL,
                                                                                                    ClassDesc.of(Domain.class.getName()),
                                                                                                    "wildcardType",
                                                                                                    MethodTypeDesc.of(CD_WildcardType,
                                                                                                                      CD_TypeMirror,
                                                                                                                      CD_TypeMirror)),

                                                                          domainDesc,
                                                                          extendsBoundDesc,
                                                                          superBoundDesc))));
      }
    }
    };
  }

}
