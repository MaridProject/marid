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
package org.marid.runtime;

import java.util.HashSet;

public interface Classes {

  static boolean superclasses(Class<?> c, boolean includeSelf, ElementVisitor<Class<?>> v) {
    if (c != null && c != Object.class && !c.isInterface()) {
      if (includeSelf) {
        if (!v.visit(c)) {
          return false;
        }
      }
      superclasses(c.getSuperclass(), true, v);
    }
    return true;
  }

  static boolean interfaces(Class<?> c, boolean includeSelf, ElementVisitor<Class<?>> v) {
    if (c != null && c != Object.class) {
      if (includeSelf && c.isInterface()) {
        if (!v.visit(c)) {
          return false;
        }
      }
      var set = new HashSet<Class<?>>();
      for (var i : c.getInterfaces()) {
        if (interfaces(i, v, set)) {
          return false;
        }
      }
    }
    return true;
  }

  private static boolean interfaces(Class<?> c, ElementVisitor<Class<?>> v, HashSet<Class<?>> passed) {
    if (passed.add(c)) {
      if (!v.visit(c)) {
        return true;
      }
      for (var i : c.getInterfaces()) {
        if (interfaces(i, v, passed)) {
          return true;
        }
      }
    }
    return false;
  }

  static void allSuperclasses(Class<?> c, boolean includeSelf, ElementVisitor<Class<?>> v) {
    if (c.isInterface()) {
      interfaces(c, includeSelf, v);
    } else {
      if (superclasses(c, includeSelf, v)) {
        interfaces(c, false, v);
      }
    }
  }
}
