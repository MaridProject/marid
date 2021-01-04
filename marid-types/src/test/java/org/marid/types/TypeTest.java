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

import com.google.common.reflect.TypeResolver;
import com.google.common.reflect.TypeToken;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.reflect.TypeUtils.parameterize;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({"UnstableApiUsage", "unchecked", "rawtypes"})
class TypeTest {

  @Test
  void resolveVariableInvariantExact() {
    var tokenWithVar = TypeToken.of(parameterize(ArrayList.class, ArrayList.class.getTypeParameters()));
    var groundToken = TypeToken.of(parameterize(ArrayList.class, Integer.class));
    var type = new TypeResolver()
      .where(tokenWithVar.getType(), groundToken.getType())
      .resolveType(ArrayList.class.getTypeParameters()[0]);
    assertEquals(Integer.class, type);
  }

  @Test
  void resolveVariableInvariantSupertype() {
    var tokenWithVar = TypeToken.of(parameterize(List.class, List.class.getTypeParameters()));
    var groundToken = TypeToken.of(parameterize(ArrayList.class, Integer.class))
      .getSupertype((Class) List.class);
    var type = new TypeResolver()
      .where(tokenWithVar.getType(), groundToken.getType())
      .resolveType(List.class.getTypeParameters()[0]);
    assertEquals(Integer.class, type);
  }
}
