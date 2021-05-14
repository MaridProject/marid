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
package org.marid.ide;

import org.marid.ide.logging.IdeLogManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

public class AppLauncher {

  public static void main(String... args) throws Exception {
    // initialize logging
    System.setProperty("java.util.logging.manager", IdeLogManager.class.getName());
    // create temporary directory
    var tempDir = Files.createTempDirectory("marid-ide");
    addShutdownAction(() -> Files.walkFileTree(tempDir, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.deleteIfExists(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        Files.deleteIfExists(dir);
        return FileVisitResult.CONTINUE;
      }
    }));
    // detect os
    final String os;
    {
      var osName = System.getProperty("os.name").toLowerCase();
      if (osName.contains("mac")) {
        os = "mac";
      } else if (osName.contains("win")) {
        os = "win";
      } else {
        os = "linux";
      }
    }
    // copy jars
    var pattern = Pattern.compile("javafx-\\w++-[\\d.]++-(\\w++).jar");
    var parentClassLoader = Thread.currentThread().getContextClassLoader();
    var depsListUrl = requireNonNull(parentClassLoader.getResource("deps.list"));
    var futures = new ArrayList<Future<URL>>();
    try (var scanner = new Scanner(depsListUrl.openStream(), UTF_8)) {
      while (scanner.hasNextLine()) {
        var jar = scanner.nextLine();
        var task = new FutureTask<>(() -> {
          var matcher = pattern.matcher(jar);
          if (matcher.matches()) {
            if (!matcher.group(1).equals(os)) {
              return null;
            }
          }
          var jarUrl = requireNonNull(parentClassLoader.getResource("deps/" + jar));
          var targetFile = tempDir.resolve(jar);
          try (var is = jarUrl.openStream()) {
            Files.copy(is, targetFile);
          }
          return targetFile.toUri().toURL();
        });
        var thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        futures.add(task);
      }
    }
    // make custom classpath
    var urls = new ArrayList<URL>(futures.size());
    for (var f : futures) {
      var url = f.get();
      if (url != null) {
        urls.add(url);
      }
    }
    var javaClassPath = System.getProperty("java.class.path");
    var javaClassPathParts = javaClassPath.split(File.pathSeparator);
    for (var part : javaClassPathParts) {
      var path = Path.of(part);
      urls.add(path.toUri().toURL());
    }
    var classLoader = new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader());
    addShutdownAction(() -> {
      classLoader.close();
      return null;
    });
    // launch application
    var applicationClass = classLoader.loadClass("javafx.application.Application");
    var appClass = classLoader.loadClass("org.marid.ide.App");
    var launchMethod = applicationClass.getMethod("launch", Class.class, String[].class);
    launchMethod.invoke(null, appClass, args);
  }

  private static void addShutdownAction(Callable<?> code) {
    Runtime.getRuntime().addShutdownHook(new Thread(new FutureTask<>(code)));
  }
}
