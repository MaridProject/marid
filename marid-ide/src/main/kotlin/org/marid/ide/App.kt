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
import javafx.stage.Stage
import org.marid.ide.context.bind
import org.marid.ide.context.fxContext
import org.marid.ide.logging.logger
import org.marid.ide.main.MainPane

class App : Application() {

  override fun init() {
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
    val context = fxContext("ide").bind(primaryStage)
    val mainPane: MainPane by context
    val scene = Scene(mainPane, 800.0, 600.0)
    primaryStage.isMaximized = true
    primaryStage.scene = scene
    primaryStage.show()
  }
}