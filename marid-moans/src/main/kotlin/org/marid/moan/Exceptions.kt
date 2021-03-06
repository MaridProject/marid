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
package org.marid.moan

import kotlin.reflect.KClass

class MoanCreationException(name: String, cause: Throwable) : RuntimeException(name, cause)
class MoanDestructionException(name: String) : RuntimeException(name)
class ScopeDestructionException(name: String) : RuntimeException(name)
class ContextCloseException(name: String) : RuntimeException(name)
class ModuleInitializationException(name: String, cause: Throwable): RuntimeException(name, cause)
class DuplicatedMoanException(name: String) : RuntimeException(name)
class ContextBoundException(targetClass: KClass<out Initializable>) : RuntimeException(targetClass.qualifiedName)
class MultipleBindingException(message: String) : RuntimeException(message)