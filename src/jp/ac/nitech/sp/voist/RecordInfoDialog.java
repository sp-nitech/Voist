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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Objects;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.BevelBorder;

public class RecordInfoDialog extends JFrame {
  private static final long serialVersionUID = 1L;

  private static enum Areas {
    LEFT,
    RIGHT,
    NUM_AREAS
  }

  //
  private final PropertiesIO properties;

  //
  private final JTextArea[] textAreas;

  //
  private int sampleRate;

  //
  private int sampleSize;

  //
  private int numChannels;

  public RecordInfoDialog(
      String propertiesFileName, int sampleRate, int sampleSize, int numChannels, ImageIcon icon) {
    Objects.requireNonNull(propertiesFileName);
    properties = new PropertiesIO(propertiesFileName);
    try {
      properties.load();
    } catch (IOException e) {
      e.printStackTrace();
    }

    setAlwaysOnTop(true);
    setBackground(Color.WHITE);
    setLocation(
        properties.getInteger("FRAME_LOCATION_X"), properties.getInteger("FRAME_LOCATION_Y"));
    setResizable(false);
    setSize(properties.getInteger("FRAME_WIDTH"), properties.getInteger("FRAME_HEIGHT"));
    setIconImage(icon.getImage());
    addKeyListener(
        new KeyAdapter() {
          @Override
          public void keyReleased(KeyEvent e) {
            switch (e.getKeyCode()) {
              case KeyEvent.VK_ESCAPE:
                setVisible(false);
                break;
              default:
                break;
            }
          }
        });

    final JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
    textAreas = new JTextArea[Areas.NUM_AREAS.ordinal()];
    for (int i = 0; i < textAreas.length; i++) {
      textAreas[i] = new JTextArea();
      textAreas[i].setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
      textAreas[i].setFocusable(false);
      textAreas[i].setEditable(false);
      textAreas[i].setFont(
          new Font(
              properties.getString("FONT_TYPE"), Font.PLAIN, properties.getInteger("FONT_SIZE")));
      panel.add(textAreas[i]);
    }

    setSampleRate(sampleRate);
    setSampleSize(sampleSize);
    setNumChannels(numChannels);

    final Container contentPane = getContentPane();
    contentPane.add(panel, BorderLayout.CENTER);
  }

  public void setSampleRate(final int sampleRate) {
    this.sampleRate = sampleRate;
  }

  public void setSampleSize(final int sampleSize) {
    this.sampleSize = sampleSize;
  }

  public void setNumChannels(final int numChannels) {
    this.numChannels = numChannels;
  }

  public int getSampleRate() {
    return sampleRate;
  }

  public int getSampleSize() {
    return sampleSize;
  }

  public int getNumChannels() {
    return numChannels;
  }

  public void setProgress(final int now, final int max) {
    if (max == 0 || now > max) {
      throw new IllegalArgumentException();
    }
    final int percentage = 100 * now / max;
    setTitle(properties.get("FRAME_TITLE") + " " + now + "/" + max + " (" + percentage + "%)");
  }

  public void update(final RecordInfo log, final boolean left) {
    Objects.requireNonNull(log);

    final String s =
        (left ? properties.getString("TEXT_LEFT") : properties.getString("TEXT_RIGHT"))
            + "\n"
            + String.format(
                " " + properties.getString("TEXT_MAX_AMP") + "%02.1f %%\n", log.getMaxAmplitude())
            + String.format(
                " " + properties.getString("TEXT_TOP_SIL") + "%02.1f s\n", log.getTopSilence())
            + String.format(
                " " + properties.getString("TEXT_END_SIL") + "%02.1f s\n", log.getEndSilence())
            + String.format(
                " " + properties.getString("TEXT_PLAYBACK_TIME") + "%02.1f s\n",
                log.getPlaybackTime(sampleRate, sampleSize, numChannels))
            + String.format(
                " " + properties.getString("TEXT_FILE_SIZE") + "%d kB", log.getFileSize());

    final int which = left ? Areas.LEFT.ordinal() : Areas.RIGHT.ordinal();
    textAreas[which].setText(s);
  }
}
