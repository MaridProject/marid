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
package org.marid.ide

import com.sun.javafx.css.StyleManager
import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.paint.Color
import javafx.stage.Stage
import org.marid.ide.context.fxContext
import org.marid.ide.context.link
import org.marid.ide.icons.MaridIconFx
import org.marid.ide.logging.IdeLogHandler
import org.marid.ide.logging.logger
import org.marid.ide.main.MainModule
import org.marid.ide.main.MainPane
import java.util.logging.Logger

class App: Application() {

  override fun init() {
    Logger.getLogger("").addHandler(IdeLogHandler)
    logger.info("Setting UA stylesheet")
    setUserAgentStylesheet(STYLESHEET_CASPIAN)
    val url = Thread.currentThread().contextClassLoader.getResource("dark.css")
    if (url != null) {
      Platform.runLater {
        StyleManager.getInstance().addUserAgentStylesheet(url.toExternalForm())
        logger.info("UA stylesheet is set to {0}", getUserAgentStylesheet())
      }
    }
  }

  override fun start(primaryStage: Stage) {
    val context = fxContext("ide")
      .link(primaryStage)
      .init(::MainModule)
    val mainPane: MainPane by context
    val scene = Scene(mainPane, 800.0, 600.0)
    primaryStage.icons += listOf(16, 24, 32).map { MaridIconFx.getIcon(it, Color.GREEN) }
    primaryStage.isMaximized = true
    primaryStage.scene = scene
    primaryStage.show()
  }
}