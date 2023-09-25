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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Objects;
import javax.swing.JButton;
import javax.swing.JPanel;
import jp.ac.nitech.sp.voist.Voist.AppStates;

public class Buttons {
  public static enum ButtonType {
    GO_PREV,
    PLAYBACK_SAMPLE,
    RECORD,
    PLAYBACK_VOICE,
    GO_NEXT,
    NUM_BUTTONS
  }

  private final PropertiesIO properties;
  private final JButton[] buttons;

  private String[] mainTexts;
  private String[] shortcutTexts;
  private ButtonEventListener listener;
  private AppStates state;

  public Buttons(JPanel panel, String propertiesFileName) {
    Objects.requireNonNull(panel);
    Objects.requireNonNull(propertiesFileName);

    properties = new PropertiesIO(propertiesFileName);
    try {
      properties.load();
    } catch (IOException e) {
      e.printStackTrace();
    }

    mainTexts = new String[ButtonType.NUM_BUTTONS.ordinal()];
    shortcutTexts = new String[ButtonType.NUM_BUTTONS.ordinal()];
    listener = null;
    state = AppStates.NONE;

    Font font =
        new Font(properties.getString("FONT_TYPE"), Font.PLAIN, properties.getInteger("FONT_SIZE"));
    Dimension dim =
        new Dimension(
            properties.getInteger("BUTTON_WIDTH"), properties.getInteger("BUTTON_HEIGHT"));
    buttons = new JButton[ButtonType.NUM_BUTTONS.ordinal()];
    for (int i = ButtonType.GO_PREV.ordinal(); i < ButtonType.NUM_BUTTONS.ordinal(); i++) {
      buttons[i] = new JButton();
      buttons[i].setFocusable(false);
      buttons[i].setFont(font);
      buttons[i].setPreferredSize(dim);
    }

    buttons[ButtonType.GO_PREV.ordinal()].addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (listener != null) {
              switch (state) {
                case READY:
                  listener.goPrevPrompt(new ButtonEvent(this));
                  break;
                case ASKING:
                  listener.cancelSave(new ButtonEvent(this));
                  break;
                case GUIDANCE:
                  listener.goPrevGuidanceStep(new ButtonEvent(this));
                  break;
                default:
                  break;
              }
            }
          }
        });

    buttons[ButtonType.PLAYBACK_SAMPLE.ordinal()].addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (listener != null) {
              switch (state) {
                case READY:
                case ASKING:
                  listener.playbackSample(new ButtonEvent(this));
                  break;
                default:
                  break;
              }
            }
          }
        });

    buttons[ButtonType.RECORD.ordinal()].addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (listener != null) {
              switch (state) {
                case READY:
                case GUIDANCE:
                  listener.record(new ButtonEvent(this));
                  break;
                case ASKING:
                  listener.playbackVoiceTake2(new ButtonEvent(this));
                  break;
                default:
                  break;
              }
            }
          }
        });

    buttons[ButtonType.PLAYBACK_VOICE.ordinal()].addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (listener != null) {
              switch (state) {
                case READY:
                case ASKING:
                case GUIDANCE:
                  listener.playbackVoice(new ButtonEvent(this));
                  break;
                default:
                  break;
              }
            }
          }
        });

    buttons[ButtonType.GO_NEXT.ordinal()].addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (listener != null) {
              switch (state) {
                case READY:
                  listener.goNextPrompt(new ButtonEvent(this));
                  break;
                case ASKING:
                  listener.overwriteSave(new ButtonEvent(this));
                  break;
                case GUIDANCE:
                  listener.goNextGuidanceStep(new ButtonEvent(this));
                  break;
                default:
                  break;
              }
            }
          }
        });

    JPanel p = new JPanel();
    p.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 5));
    p.setBackground(Color.WHITE);
    for (int i = ButtonType.GO_PREV.ordinal(); i < ButtonType.NUM_BUTTONS.ordinal(); i++) {
      if (properties.getBoolean("USE_SAMPLE") || i != ButtonType.PLAYBACK_SAMPLE.ordinal()) {
        p.add(buttons[i]);
      }
    }
    panel.add(p);
  }

  public void setEnabled(boolean enabled) {
    for (int i = ButtonType.GO_PREV.ordinal(); i < ButtonType.NUM_BUTTONS.ordinal(); i++) {
      buttons[i].setEnabled(enabled);
    }
  }

  public void setEnabled(ButtonType which, boolean enabled) {
    buttons[which.ordinal()].setEnabled(enabled);
  }

  public void setEnabled(
      boolean lefter, boolean left, boolean center, boolean right, boolean righter) {
    buttons[0].setEnabled(lefter);
    buttons[1].setEnabled(left);
    buttons[2].setEnabled(center);
    buttons[3].setEnabled(right);
    buttons[4].setEnabled(righter);
  }

  public void doClick(ButtonType which) {
    buttons[which.ordinal()].doClick();
  }

  public void changeState(AppStates state) {
    if (this.state == state) {
      return;
    }
    this.state = state;

    setMainText(ButtonType.GO_PREV, "A");
    setMainText(ButtonType.PLAYBACK_SAMPLE, "A");
    setMainText(ButtonType.RECORD, "A");
    setMainText(ButtonType.PLAYBACK_VOICE, "A");
    setMainText(ButtonType.GO_NEXT, "A");
  }

  public void setMainText(ButtonType which, String suffix) {
    String baseKey = "BUTTON_TEXT_";
    switch (state) {
      case READY:
        baseKey += "READY_";
        break;
      case ASKING:
        baseKey += "ASKING_";
        break;
      case GUIDANCE:
        baseKey += "GUIDANCE_";
        break;
      default:
        break;
    }

    int index = which.ordinal();
    String text = properties.getString(baseKey + (index + 1) + "_" + suffix);
    mainTexts[index] = text;
    setText(which);
  }

  public void setShortcutText(ButtonType which, String text) {
    shortcutTexts[which.ordinal()] = text;
    setText(which);
  }

  private void setText(ButtonType which) {
    int index = which.ordinal();
    String s = (shortcutTexts[index] == null) ? "" : " (" + shortcutTexts[index] + ")";
    buttons[index].setText(mainTexts[index] + s);
  }

  public void addButtonEventListener(ButtonEventListener listener) {
    this.listener = listener;
  }

  public void removeButtonEventListener() {
    listener = null;
  }
}
