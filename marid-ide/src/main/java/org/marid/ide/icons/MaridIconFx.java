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
package org.marid.ide.icons;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static javafx.scene.paint.Color.GREEN;
import static javafx.scene.paint.Color.web;
import static javafx.scene.paint.CycleMethod.NO_CYCLE;

public class MaridIconFx {

  private static final double STROKE_WIDTH = 0.15;
  private static final double SCALE = 2d / (2d + STROKE_WIDTH);
  private static final LinearGradient VGRAD = new LinearGradient(-1, -1, -1, 1, false, NO_CYCLE,
      new Stop(-1, web("0x00005fff")),
      new Stop(1, web("0x9696ffff"))
  );
  private static final LinearGradient BGRAD = new LinearGradient(0, 1, 0, -1, false, NO_CYCLE,
      new Stop(-1, web("0xffffff", 1.0)),
      new Stop(1, web("0xffffff", 0.1)));

  public static void draw(GraphicsContext context, int size, Color color) {
    final double s = size / 2d;
    context.scale(s, -s);
    context.translate(1d, -1d);
    context.setFill(VGRAD);
    context.fillRect(-1, -1, 2, 2);
    context.scale(SCALE, SCALE);

    // wave
    context.beginPath();
    context.setStroke(Color.WHITE);
    context.setLineWidth(STROKE_WIDTH);
    context.moveTo(-1.1, +0.1);
    context.quadraticCurveTo(-0.8, +0.3, -0.475, +0.17);
    context.moveTo(+1.1, +0.1);
    context.quadraticCurveTo(+0.8, +0.3, +0.475, +0.17);
    context.stroke();

    // bottle
    context.beginPath();
    context.setFill(new Color(color.getRed(), color.getGreen(), color.getBlue(), 1));
    context.moveTo(-0.5f, -1.0f);
    context.bezierCurveTo(-0.6, +0.0, -0.1, +0.3, -0.1, +1.0);
    context.lineTo(+0.1, +1.0);
    context.bezierCurveTo(+0.1, +0.3, +0.6, +0.0, +0.5, -1.0);
    context.closePath();
    context.fill();
    context.stroke();
    context.setFill(BGRAD);
    context.fill();
  }

  public static WritableImage getImage(int size, Color color) {
    return getImage(null, size, color);
  }

  public static WritableImage getImage(SnapshotParameters parameters, int size, Color color) {
    final Canvas canvas = new Canvas(size, size);
    draw(canvas.getGraphicsContext2D(), size, color);
    return canvas.snapshot(parameters, null);
  }

  public static WritableImage getIcon(SnapshotParameters parameters, int size, Color color) {
    final WritableImage image = getImage(parameters, size, color);
    return new WritableImage(image.getPixelReader(), size, size);
  }

  public static WritableImage getIcon(int size, Color color) {
    return getIcon(null, size, color);
  }

  public static WritableImage getIcon(int size) {
    return getIcon(null, size, GREEN);
  }

  public static WritableImage[] getIcons(int... sizes) {
    return Arrays.stream(sizes).mapToObj(MaridIconFx::getIcon).toArray(WritableImage[]::new);
  }

  public static void main(String... args) {
    Platform.startup(() -> {
    });
    final int size = args.length < 1 ? 128 : Integer.parseInt(args[0]);
    final Color color = args.length < 2 ? Color.GREEN : Color.valueOf(args[1]);
    final AtomicReference<WritableImage> imageRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(() -> {
      imageRef.set(getImage(size, color));
      latch.countDown();
    });
    try {
      latch.await();
      final File file = new File("marid.png");
      ImageIO.write(SwingFXUtils.fromFXImage(imageRef.get(), null), "PNG", file);
      Desktop.getDesktop().open(file);
      Platform.exit();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
