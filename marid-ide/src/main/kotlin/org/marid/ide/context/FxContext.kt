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
package org.marid.ide.context

import javafx.application.Platform
import javafx.event.EventType
import javafx.stage.Window
import javafx.stage.WindowEvent
import org.marid.moan.Context

fun fxContext(name: String): Context = Context(name, closer = { Runnable { Platform.runLater(it) } })
fun fxContext(name: String, parent: Context) = Context(name, parent, closer = { Runnable { Platform.runLater(it) } })

fun Context.bind(window: Window, eventType: EventType<WindowEvent> = WindowEvent.WINDOW_HIDDEN): Context {
  window.properties["context"] = this
  window.addEventHandler(eventType) {
    window.properties.remove("context")
    close()
  }
  return this
}