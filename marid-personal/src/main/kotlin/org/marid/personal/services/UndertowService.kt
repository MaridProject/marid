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
package org.marid.personal.services

import io.undertow.Undertow
import org.marid.moan.Moan
import org.marid.personal.config.UndertowConfig
import org.xnio.OptionMap
import org.xnio.Options

class UndertowService(config: UndertowConfig): Moan {

  private val undertow = Undertow.builder()
    .setIoThreads(config.ioThreads)
    .setWorkerThreads(config.workerThreads)
    .addListener(
      Undertow.ListenerBuilder()
        .setHost(config.host)
        .setPort(config.port)
        .setType(Undertow.ListenerType.HTTP)
        .setOverrideSocketOptions(
          OptionMap.builder()
            .set(Options.SEND_BUFFER, 128 shl 10)
            .set(Options.KEEP_ALIVE, true)
            .map
        )
    )
    .build()

  override fun init() {
    undertow.start()
  }

  override fun destroy() {
    undertow.stop()
  }
}