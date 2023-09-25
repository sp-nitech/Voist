// ------------------------------------------------------------------------ //
// Copyright 2016 Nagoya Institute of Technology                            //
//                                                                          //
// Licensed under the Apache License, Version 2.0 (the "License");          //
// you may not use this file except in compliance with the License.         //
// You may obtain a copy of the License at                                  //
//                                                                          //
//     http://www.apache.org/licenses/LICENSE-2.0                           //
//                                                                          //
// Unless required by applicable law or agreed to in writing, software      //
// distributed under the License is distributed on an "AS IS" BASIS,        //
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. //
// See the License for the specific language governing permissions and      //
// limitations under the License.                                           //
// ------------------------------------------------------------------------ //

package jp.ac.nitech.sp.voist;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class Waveform extends JPanel {
  private static final long serialVersionUID = 1L;

  //
  private final PropertiesIO properties;

  //
  private final JFrame frame;

  //
  private final int[] xPoints;

  //
  private final int[] yPoints;

  public Waveform(String propertiesFileName, ImageIcon icon) {
    Objects.requireNonNull(propertiesFileName);
    properties = new PropertiesIO(propertiesFileName);
    try {
      properties.load();
    } catch (IOException e) {
      e.printStackTrace();
    }

    frame = new JFrame();
    frame.setAlwaysOnTop(true);
    frame.setSize(properties.getInteger("FRAME_WIDTH"), properties.getInteger("FRAME_HEIGHT"));
    frame.setLocation(
        properties.getInteger("FRAME_LOCATION_X"), properties.getInteger("FRAME_LOCATION_Y"));
    frame.setResizable(false);
    frame.setIconImage(icon.getImage());
    frame.getContentPane().add(this);
    frame.addKeyListener(
        new KeyAdapter() {
          @Override
          public void keyReleased(KeyEvent e) {
            switch (e.getKeyCode()) {
              case KeyEvent.VK_ESCAPE:
                frame.setVisible(false);
                break;
              default:
                break;
            }
          }
        });
    setBackground(Color.WHITE);

    int numPoints = properties.getInteger("WAVEFORM_NUM_DRAWING_POINTS");
    xPoints = new int[numPoints];
    yPoints = new int[numPoints];
  }

  public boolean draw(String fileName, int fileLength, String promptId, int sampleSize) {
    if (fileName == null || fileLength <= 0 || sampleSize <= 0) {
      return false;
    }

    try {
      FileInputStream fis = new FileInputStream(fileName);
      FileChannel fc = fis.getChannel();
      ByteBuffer bb = ByteBuffer.allocate(fileLength);

      fc.read(bb);
      bb.rewind();

      int numPoints = xPoints.length;
      int skip = fileLength / xPoints.length - sampleSize;
      // Ensure 'skip' is a multiple of 'sampleSize'.
      for (int i = 1; i < sampleSize; i++) {
        if (skip % sampleSize == i) {
          skip -= i;
          break;
        }
      }

      if (skip > 0) {
        double inverseNumPoints = 1.0 / numPoints;
        byte[] bytes = new byte[sampleSize];

        for (int i = 0; i < numPoints; i++) {
          bb.get(bytes);

          // X axis starts at the left side of the screen
          // and the values increase to the right.
          xPoints[i] =
              properties.getInteger("WAVEFORM_LOCATION_X")
                  + (int)
                      Math.round(properties.getInteger("WAVEFORM_WIDTH") * i * inverseNumPoints);

          // Y axis starts at the top of the screen
          // and the values increase downward.
          yPoints[i] =
              properties.getInteger("WAVEFORM_LOCATION_Y")
                  - (int)
                      (properties.getDouble("WAVEFORM_HEIGHT_SCALE")
                          * convertBytesToInteger(bytes));

          // Skip points to reduce the computationally complexity.
          bb.position(bb.position() + skip);
        }
      }

      fc.close();
      fis.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return false;
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }

    frame.setTitle(properties.get("FRAME_TITLE") + promptId + ".raw");
    frame.setVisible(true);
    frame.repaint();

    return true;
  }

  @Override
  public void setVisible(boolean visible) {
    frame.setVisible(visible);
  }

  @Override
  public boolean isVisible() {
    return frame.isVisible();
  }

  @Override
  public void paintComponent(Graphics graphics) {
    super.paintComponent(graphics);

    // Draw a waveform.
    graphics.setColor(Color.BLACK);
    graphics.drawPolyline(xPoints, yPoints, xPoints.length);

    // Draw normalization lines.
    int normalizationLine =
        (int)
            (Integer.MAX_VALUE
                * properties.getDouble("NORMALIZATION_RATE")
                * properties.getDouble("WAVEFORM_HEIGHT_SCALE"));
    int x = properties.getInteger("WAVEFORM_LOCATION_X");
    int y = properties.getInteger("WAVEFORM_LOCATION_Y");
    int width = properties.getInteger("WAVEFORM_WIDTH");
    graphics.setColor(Color.BLUE);
    graphics.drawLine(x, normalizationLine + y, x + width, normalizationLine + y);
    graphics.drawLine(x, -normalizationLine + y, x + width, -normalizationLine + y);
  }

  private static int convertBytesToInteger(byte[] bytes) {
    Objects.requireNonNull(bytes);

    byte[] reverse = new byte[4];
    for (int i = bytes.length - 1, j = 0; i >= 0; i--, j++) {
      reverse[j] = bytes[i];
    }

    int convertedInteger = 0;
    for (byte b : reverse) {
      int i = 0xFF & b;
      convertedInteger = (convertedInteger << 8) + i;
    }

    return convertedInteger;
  }
}
