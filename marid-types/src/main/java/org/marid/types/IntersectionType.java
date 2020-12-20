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

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class IntersectionType implements Type {

  private final Type[] types;

  IntersectionType(Type[] types) {
    this.types = types;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (o instanceof IntersectionType) {
      var that = (IntersectionType) o;
      return Arrays.equals(types, that.types);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(types);
  }

  @Override
  public final String toString() {
    return Arrays.stream(types).map(Type::getTypeName).collect(Collectors.joining(" & "));
  }
}
