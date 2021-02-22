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
package org.marid.examples

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun main() {
  val url = URI("http://api.gismeteo.net/v2/weather/current/4368/?lang=en")
  val client = HttpClient.newHttpClient()
  val response = client.send(
    HttpRequest.newBuilder(url)
      .header("X-Gismeteo-Token", "56b30cb255.3443075")
      .build(),
    HttpResponse.BodyHandlers.ofString()
  )
  println(response.statusCode())
  println(response.body())
}