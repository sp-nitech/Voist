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
import java.awt.Component;
import java.awt.Font;
import java.io.IOException;
import java.util.Objects;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class Guidance {
  //
  private final PropertiesIO properties;

  // (textAreas[0] is dummy).
  private final JLabel textAreas[];

  //
  private int step;

  public Guidance(JPanel panel, String propertiesFileName) {
    Objects.requireNonNull(panel);
    Objects.requireNonNull(propertiesFileName);

    properties = new PropertiesIO(propertiesFileName);
    try {
      properties.load();
    } catch (IOException e) {
      e.printStackTrace();
    }

    textAreas = new JLabel[properties.getInteger("MAX_LINES") + 1];
    for (int i = 1; i < textAreas.length; i++) {
      textAreas[i] = new JLabel(" ");
      textAreas[i].setFont(
          new Font(
              properties.getString("FONT_TYPE"), Font.PLAIN, properties.getInteger("FONT_SIZE")));
      textAreas[i].setFocusable(false);
      textAreas[i].setAlignmentX(Component.CENTER_ALIGNMENT);
      textAreas[i].setBackground(Color.WHITE);
      panel.add(textAreas[i]);
    }

    step = 0;
  }

  public boolean setFontSize(int fontSize) {
    if (fontSize > 0) {
      for (int i = 1; i < textAreas.length; i++) {
        textAreas[i].setFont(new Font(properties.getString("FONT_TYPE"), Font.PLAIN, fontSize));
      }
      return true;
    }
    return false;
  }

  public int getFontSize() {
    return textAreas[1].getFont().getSize();
  }

  public boolean isFirstStep() {
    return (step == properties.getInteger("STEP_FIRST")) ? true : false;
  }

  public boolean isEndStep() {
    return (step == properties.getInteger("STEP_END")) ? true : false;
  }

  public boolean isRecordingTestStep() {
    return (step == properties.getInteger("STEP_RECORDING_TEST")) ? true : false;
  }

  public boolean isEnvironmentTestStep() {
    return (step == properties.getInteger("STEP_ENVIRONMENT_TEST")) ? true : false;
  }

  public boolean isStepFromFirst(int num) {
    return (step == properties.getInteger("STEP_FIRST") - num) ? true : false;
  }

  public boolean isStepFromEnd(int num) {
    return (step == properties.getInteger("STEP_END") - num) ? true : false;
  }

  public boolean setText(String text, int line, boolean highlight) {
    if (text == null || line <= 0 || textAreas.length < line) {
      return false;
    }

    if (highlight) {
      textAreas[line].setForeground(Color.RED);
    } else {
      textAreas[line].setForeground(Color.BLACK);
    }

    textAreas[line].setText(text);
    for (int i = line + 1; i < textAreas.length; i++) {
      textAreas[i].setText(" ");
    }

    return true;
  }

  public void setTextRecordingTest() {
    String baseKey = properties.getString("BASE_KEY") + properties.getString("STEP_RECORDING_TEST");
    setText(properties.getString(baseKey + "_A"), 1, false);
    setText(properties.getString(baseKey + "_B"), 2, false);
  }

  public void setTextEnvironmentTest(double nowSNR, double goodSNR) {
    String baseKey =
        properties.getString("BASE_KEY") + properties.getString("STEP_ENVIRONMENT_TEST");
    setText(properties.getString(baseKey + "_A"), 1, false);
    if (nowSNR >= goodSNR) {
      setText(properties.getString(baseKey + "_B"), 2, false);
    } else {
      setText(properties.getString(baseKey + "_C"), 2, false);
    }
  }

  public void start() {
    step = properties.getInteger("STEP_FIRST");
    guide();
  }

  public void end() {
    step = properties.getInteger("STEP_END");
    guide();
  }

  public void goPrev() {
    if (step - 1 < properties.getInteger("STEP_FIRST")) {
      return;
    }
    step--;
    guide();
  }

  public void goNext() {
    if (step + 1 > properties.getInteger("STEP_END")) {
      return;
    }
    step++;
    guide();
  }

  private void guide() {
    for (int i = 1; i < textAreas.length; i++) {
      String baseKey = properties.getString("BASE_KEY") + step + "_" + i;
      setText(properties.getString(baseKey), i, false);
    }
  }
}
