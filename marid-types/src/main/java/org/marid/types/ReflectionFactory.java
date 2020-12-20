/*
 * MARID, the visual component programming environment.
 * Copyright (C) 2020 Dzmitry Auchynnikau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.marid.types;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class ReflectionFactory {

  public static final Type[] EMPTY_TYPES = new Type[0];

  private ReflectionFactory() {}

  @NotNull
  public static ParameterizedType parameterizedType(@NotNull Class<?> raw, @NotNull Type... parameters) {
    return new MaridParameterizedType(raw.getDeclaringClass(), raw, parameters);
  }

  @NotNull
  public static ParameterizedType parameterizedType(@NotNull Type owner, @NotNull Class<?> raw, @NotNull Type... parameters) {
    return new MaridParameterizedType(owner, raw, parameters);
  }

  @NotNull
  public static Type intersectionType(@NotNull Type... types) {
    return new IntersectionType(types);
  }

  @NotNull
  public static WildcardType upperBoundedWildcardType(@NotNull Type... upperBounds) {
    return new MaridWildcardType(upperBounds, EMPTY_TYPES);
  }

  @NotNull
  public static WildcardType lowerBoundedWildcardType(@NotNull Type... lowerBounds) {
    return new MaridWildcardType(EMPTY_TYPES, lowerBounds);
  }

  @NotNull
  public static WildcardType wildcradType() {
    return new MaridWildcardType(EMPTY_TYPES, EMPTY_TYPES);
  }

  @NotNull
  public static WildcardType wildcardType(@NotNull Type[] upperBounds, @NotNull Type[] lowerBounds) {
    return new MaridWildcardType(upperBounds, lowerBounds);
  }

  @NotNull
  public static GenericArrayType arrayType(@NotNull Type componentType) {
    return new MaridArrayType(componentType);
  }
}

final class MaridParameterizedType implements ParameterizedType {

  private final Type owner;
  private final Class<?> raw;
  private final Type[] parameters;

  MaridParameterizedType(Type owner, Class<?> raw, Type[] parameters) {
    this.owner = owner;
    this.raw = raw;
    this.parameters = parameters;
  }

  @Override
  public final Type[] getActualTypeArguments() {
    return parameters;
  }

  @Override
  public final Type getRawType() {
    return raw;
  }

  @Override
  public final Type getOwnerType() {
    return owner;
  }

  @Override
  public final boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof ParameterizedType) {
      var that = (ParameterizedType) o;
      return Objects.equals(owner, that.getOwnerType())
        && raw.equals(that.getRawType())
        && Arrays.equals(parameters, that.getActualTypeArguments());
    } else {
      return false;
    }
  }

  @Override
  public final int hashCode() {
    return Arrays.hashCode(parameters) ^ Objects.hashCode(owner) ^ raw.hashCode();
  }

  @Override
  public final String toString() {
    var ownerPrefix = owner == null ? "" : owner.getTypeName() + ".";
    return Arrays.stream(parameters)
      .map(Type::getTypeName)
      .collect(Collectors.joining(",", ownerPrefix + raw.getCanonicalName() + "<", ">"));
  }
}

final class MaridWildcardType implements WildcardType {

  private final Type[] upperBounds;
  private final Type[] lowerBounds;

  MaridWildcardType(Type[] upperBounds, Type[] lowerBounds) {
    this.upperBounds = upperBounds;
    this.lowerBounds = lowerBounds;
  }

  @Override
  public final Type[] getUpperBounds() {
    return upperBounds;
  }

  @Override
  public final Type[] getLowerBounds() {
    return lowerBounds;
  }

  @Override
  public final boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof WildcardType) {
      var that = (WildcardType) o;
      return Arrays.equals(lowerBounds, that.getLowerBounds()) && Arrays.equals(upperBounds, that.getUpperBounds());
    } else {
      return false;
    }
  }

  @Override
  public final int hashCode() {
    return Arrays.hashCode(lowerBounds) ^ Arrays.hashCode(upperBounds);
  }

  @Override
  public final String toString() {
    var ep = upperBounds.length > 0
      ? Arrays.stream(upperBounds).map(Type::getTypeName).collect(Collectors.joining(" & ", " extends", ""))
      : "";
    var sp = lowerBounds.length > 0
      ? Arrays.stream(lowerBounds).map(Type::getTypeName).collect(Collectors.joining(" & ", " super", ""))
      : "";
    return "?" + ep + sp;
  }
}

final class MaridArrayType implements GenericArrayType {

  private final Type componentType;

  MaridArrayType(Type componentType) {
    this.componentType = componentType;
  }

  @Override
  public final Type getGenericComponentType() {
    return componentType;
  }

  @Override
  public final boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof GenericArrayType) {
      return componentType.equals(((GenericArrayType) o).getGenericComponentType());
    } else {
      return false;
    }
  }

  @Override
  public final int hashCode() {
    return componentType.hashCode();
  }

  @Override
  public final String toString() {
    return componentType.getTypeName() + "[]";
  }
}