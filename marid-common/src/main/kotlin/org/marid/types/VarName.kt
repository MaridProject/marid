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

import org.apache.commons.codec.binary.Hex
import org.apache.commons.text.StringEscapeUtils
import java.nio.charset.StandardCharsets

@JvmRecord
data class VarName(private val name: String): Comparable<VarName> {
  val jvmName: String
    get() {
      val hex = Hex.encodeHexString(name.toByteArray(StandardCharsets.UTF_8))
      return "v_$hex"
    }

  val escaped: String get() = StringEscapeUtils.escapeJava(name)
  override fun toString(): String = name
  override fun compareTo(other: VarName): Int = name.compareTo(other.name)
  val placeholder: String get() = "@{$name}"
}