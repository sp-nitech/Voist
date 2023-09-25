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
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

public class AudioLevelMeter {
  //
  private final JProgressBar progressBar;

  //
  private LevelMeter levelMeter;

  //
  private boolean red;

  //
  private boolean green;

  //
  private double redLine;

  //
  private double greenLine;

  //
  private int sampleSize;

  //
  private double inverseMaxAmplitude;

  public AudioLevelMeter(JPanel panel, int sampleSize) {
    Objects.requireNonNull(panel);

    progressBar = new JProgressBar();
    progressBar.setBackground(Color.WHITE);
    progressBar.setFocusable(false);
    progressBar.setMaximumSize(new Dimension(400, 32767));
    progressBar.setOrientation(SwingConstants.HORIZONTAL);
    panel.add(progressBar);

    red = false;
    green = false;
    redLine = 0.0;
    greenLine = 0.0;
    setSampleSize(sampleSize);
  }

  public boolean setRedLine(double rate) {
    if (rate >= 0.0 && rate <= 1.0 && rate >= greenLine) {
      redLine = rate;
      return true;
    }
    return false;
  }

  public boolean setGreenLine(double rate) {
    if (rate >= 0.0 && rate <= 1.0 && rate <= redLine) {
      greenLine = rate;
      return true;
    }
    return false;
  }

  public boolean setSampleSize(int sampleSize) {
    if (sampleSize > 0) {
      this.sampleSize = sampleSize;
      inverseMaxAmplitude = 1.0 / (Math.pow(2.0, (sampleSize * 8)) * 0.5);
      return true;
    }
    return false;
  }

  public double getRedLine() {
    return redLine;
  }

  public double getGreenLine() {
    return greenLine;
  }

  public int getSampleSize() {
    return sampleSize;
  }

  public boolean isRed() {
    return red;
  }

  public boolean isGreen() {
    return green;
  }

  public void clear() {
    red = false;
    green = false;
  }

  public boolean start(Audio audio, int fps) {
    if (audio == null || fps <= 0) {
      return false;
    }

    levelMeter = new LevelMeter(audio, fps);
    levelMeter.addPropertyChangeListener(
        new PropertyChangeListener() {
          @Override
          public void propertyChange(PropertyChangeEvent e) {
            if ("progress".equals(e.getPropertyName())) {
              int progress = (Integer) e.getNewValue();
              progressBar.setValue(progress);
            }
          }
        });
    levelMeter.execute();

    return true;
  }

  public void stop() {
    if (levelMeter != null) {
      levelMeter.halt();
    }
  }

  private class LevelMeter extends SwingWorker<String, String> {
    // These values do not affect the decision of min/max rejection.
    private static final int BIAS = 48;
    private static final double DROP_SPEED = 1.4;

    private final Audio audio;
    private final long period;
    private boolean halt;

    public LevelMeter(Audio audio, int fps) {
      this.audio = audio;
      period = Math.round(1000.0 / fps);
      halt = false;
    }

    public void halt() {
      halt = true;
    }

    @Override
    public String doInBackground() {
      double nowDecibel = 0.0;
      double newDecibel = 0.0;

      setProgress(0);
      while (!halt) {
        try {
          // Adjust the drawing speed of this component.
          Thread.sleep(period);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }

        newDecibel = convertLevelToDecibel(audio.getLevel());
        nowDecibel = (newDecibel > nowDecibel) ? newDecibel : nowDecibel - DROP_SPEED;

        // This component can be set 0 to 100.
        setProgress(Math.min(Math.max((int) nowDecibel, 0), 100));

        double rate = convertDecibelToRate(nowDecibel);
        if (rate >= redLine) {
          progressBar.setForeground(Color.RED);
          red = true;
          green = false;
        } else if (rate >= greenLine) {
          progressBar.setForeground(Color.GREEN);
          if (!red) {
            green = true;
          }
        } else {
          progressBar.setForeground(Color.LIGHT_GRAY);
        }
      }

      return null;
    }

    private double convertLevelToDecibel(int level) {
      return (20.0 * Math.log10(level * inverseMaxAmplitude) + BIAS) / BIAS * 100.0;
    }

    private double convertDecibelToRate(double decibel) {
      return Math.exp(0.0005 * BIAS * (decibel - 100.0) * Math.log(10.0));
    }
  }
}
