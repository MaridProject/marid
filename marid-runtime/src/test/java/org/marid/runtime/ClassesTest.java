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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClassesTest {

  @Test
  void testSingleClassSuperclass() {
    var scs = ElementVisitor.<Class<?>>toList(v -> Classes.superclasses(A.class, true, v));
    assertEquals(List.of(A.class), scs);
  }

  @Test
  void testSingleClassSuperclassExcluded() {
    var scs = ElementVisitor.<Class<?>>toList(v -> Classes.superclasses(A.class, false, v));
    assertEquals(List.of(), scs);
  }

  @Test
  void testInterfaceSuperclass() {
    var scs = ElementVisitor.<Class<?>>toList(v -> Classes.superclasses(B.class, true, v));
    assertEquals(List.of(), scs);
  }

  @Test
  void testEnumSuperclass() {
    var scs = ElementVisitor.<Class<?>>toList(v -> Classes.superclasses(C.class, true, v));
    assertEquals(List.of(C.class, Enum.class), scs);
  }

  @Test
  void testSuperclasses() {
    var scs = ElementVisitor.<Class<?>>toList(v -> Classes.superclasses(D.class, true, v));
    assertEquals(List.of(D.class, A.class), scs);
  }

  @Test
  void testSingleInterfaceInterfaces() {
    var scs = ElementVisitor.<Class<?>>toList(v -> Classes.interfaces(B.class, true, v));
    assertEquals(List.of(B.class), scs);
  }

  @Test
  void testSingleInterfaceInterfacesExcluded() {
    var scs = ElementVisitor.<Class<?>>toList(v -> Classes.interfaces(B.class, false, v));
    assertEquals(List.of(), scs);
  }

  @Test
  void testInterfacesOfFIncluded() {
    var scs = ElementVisitor.<Class<?>>toList(v -> Classes.interfaces(F.class, true, v));
    assertEquals(List.of(F.class, E.class), scs);
  }

  @Test
  void testInterfacesOfFExcluded() {
    var scs = ElementVisitor.<Class<?>>toList(v -> Classes.interfaces(F.class, false, v));
    assertEquals(List.of(E.class), scs);
  }

  @Test
  void testInterfacesOfGIncluded() {
    var scs = ElementVisitor.<Class<?>>toList(v -> Classes.interfaces(G.class, true, v));
    assertEquals(List.of(E.class), scs);
  }

  @Test
  void testInterfacesOfGExcluded() {
    var scs = ElementVisitor.<Class<?>>toList(v -> Classes.interfaces(G.class, false, v));
    assertEquals(List.of(E.class), scs);
  }

  @Test
  void testInterfacesOfHIncluded() {
    var scs = ElementVisitor.<Class<?>>toList(v -> Classes.interfaces(H.class, true, v));
    assertEquals(List.of(F.class, E.class), scs);
  }

  @Test
  void testInterfacesOfHExcluded() {
    var scs = ElementVisitor.<Class<?>>toList(v -> Classes.interfaces(H.class, false, v));
    assertEquals(List.of(F.class, E.class), scs);
  }

  @Test
  void testInterfacesOfIIncluded() {
    var scs = ElementVisitor.<Class<?>>toList(v -> Classes.interfaces(I.class, true, v));
    assertEquals(List.of(I.class, F.class, E.class), scs);
  }

  @Test
  void testInterfacesOfIExcluded() {
    var scs = ElementVisitor.<Class<?>>toList(v -> Classes.interfaces(I.class, false, v));
    assertEquals(List.of(F.class, E.class), scs);
  }

  static class A { }
  interface B { }
  enum C {}
  static class D extends A { }
  interface E { }
  interface F extends E { }
  static class G implements E { }
  static class H implements F, E { }
  interface I extends F, E { }
}