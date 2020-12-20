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
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

public class MaridTypes {

  private MaridTypes() {}

  @NotNull
  public static Type arrayType(@NotNull Type componentType) {
    if (componentType instanceof Class<?>) {
      return Array.newInstance((Class<?>) componentType, 0).getClass();
    } else {
      return ReflectionFactory.arrayType(componentType);
    }
  }

  @NotNull
  public static Type parameterizedType(@NotNull Class<?> raw, @NotNull Type... parameters) {
    if (parameters.length == 0) {
      return raw;
    } else {
      return ReflectionFactory.parameterizedType(raw, parameters);
    }
  }

  @NotNull
  public static Type parameterizedType(@Nullable Type owner, @NotNull Class<?> raw, @NotNull Type... parameters) {
    if (parameters.length == raw.getTypeParameters().length) {
      if (owner instanceof ParameterizedType) {
        if (Objects.equals(raw.getDeclaringClass(), ((ParameterizedType) owner).getRawType())) {
          return new MaridParameterizedType(owner, raw, parameters);
        } else {
          throw new IllegalArgumentException(
            "Owner of " + raw + "(" + raw.getDeclaringClass() + ") doesn't match " + owner.getTypeName()
          );
        }
      } else if (owner == null) {
        return new MaridParameterizedType(raw.getDeclaringClass(), raw, parameters);
      } else {
        throw new IllegalArgumentException(
          "Unknown owner type " + owner.getTypeName()
        );
      }
    } else {
      throw new IllegalArgumentException(
        "Type parameter count mismatch of " + raw + ": " + parameters.length + " vs " + raw.getTypeParameters().length
      );
    }
  }
}
