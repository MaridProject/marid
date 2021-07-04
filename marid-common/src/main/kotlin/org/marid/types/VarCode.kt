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
package org.marid.types

import java.util.*
import java.util.regex.Pattern

@JvmInline
value class VarCode(private val code: String) {

  val resolved: String get() = PATTERN.matcher(code).replaceAll { r -> VarName(r.group(1)).jvmName }
  fun contains(name: VarName): Boolean = code.contains(name.placeholder)
  val isRoot: Boolean get() = !PATTERN.matcher(code).find()
  override fun toString(): String = code

  val varNames: Set<VarName>
    get() {
      val treeSet = TreeSet<VarName>()
      val matcher = PATTERN.matcher(code)
      while (matcher.find()) {
        treeSet += VarName(matcher.group(1))
      }
      return treeSet
    }

  companion object {
    private val PATTERN: Pattern = Pattern.compile("@\\{([^}]+)}")
  }
}