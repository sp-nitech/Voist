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

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Menu {
  private final PropertiesIO properties;

  private boolean canSendCommand;
  private MenuEventListener listener;

  private final JCheckBoxMenuItem cbAutoSamplePlayback;
  private final JCheckBoxMenuItem cbAutoVoicePlayback;
  private final JCheckBoxMenuItem cbMaxAmplitudeRejection;
  private final JCheckBoxMenuItem cbMinAmplitudeRejection;
  private final JCheckBoxMenuItem cbPromptLineBreakAtPP;
  private final JCheckBoxMenuItem cbShortcutMouseWheel;
  private final JCheckBoxMenuItem cbStartupGuidance;
  private final JRadioButtonMenuItem[] rdbtnBeepTypes;
  private final JRadioButtonMenuItem[] rdbtnGuidanceFontSizes;
  private final JRadioButtonMenuItem[] rdbtnPromptFontTypes;
  private final JRadioButtonMenuItem[] rdbtnPromptLowerBoundFontSizes;
  private final JRadioButtonMenuItem[] rdbtnPromptUpperBoundFontSizes;
  private final JRadioButtonMenuItem[] rdbtnPromptRubyTypes;
  private final JRadioButtonMenuItem[] rdbtnVadLevels;
  private final ArrayList<String> fontTypes;

  private final int maxPromptFontSize;
  private final int maxGuidanceFontSize;

  public Menu(JFrame frame, String propertiesFileName) {
    Objects.requireNonNull(frame);
    Objects.requireNonNull(propertiesFileName);

    properties = new PropertiesIO(propertiesFileName);
    try {
      properties.load();
    } catch (IOException e) {
      e.printStackTrace();
    }

    canSendCommand = true;

    final Font font =
        new Font(properties.getString("FONT_TYPE"), Font.PLAIN, properties.getInteger("FONT_SIZE"));
    UIManager.put("OptionPane.messageFont", font);

    final JMenuBar menuBar = new JMenuBar();
    frame.setJMenuBar(menuBar);

    /*
     * ------------------------------- File -------------------------------- *
     */
    final JMenu mnFile = new JMenu(properties.getString("MENU_FILE"));
    mnFile.setMnemonic(KeyEvent.VK_F);
    mnFile.setFont(font);
    menuBar.add(mnFile);

    //
    final JFileChooser fileChooser = new JFileChooser();
    final FileFilter filter =
        new FileNameExtensionFilter(properties.getString("PROMPT_SELECTION_EXT"), "xml");
    fileChooser.addChoosableFileFilter(filter);
    fileChooser.setFileFilter(filter);
    fileChooser.setCurrentDirectory(new File(properties.getString("PROMPT_SELECTION_DIR")));
    fileChooser.setDialogTitle(properties.getString("PROMPT_SELECTION_TITLE"));
    setFileChooserFont(fileChooser.getComponents(), font);
    final JMenuItem mnChangePromptSet =
        new JMenuItem(properties.getString("MENU_FILE_CHANGE_PROMPT_SET"));
    mnChangePromptSet.setFont(font);
    mnChangePromptSet.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            final int selected = fileChooser.showOpenDialog(frame);
            final String param =
                (selected == JFileChooser.APPROVE_OPTION)
                    ? fileChooser.getSelectedFile().getAbsolutePath()
                    : null;
            System.gc(); // Prevent an error.
            if (listener != null && param != null) {
              listener.changePromptSet(new MenuEvent(this, param));
            }
          }
        });
    mnFile.add(mnChangePromptSet);

    //
    final JMenuItem mnChangeUser = new JMenuItem(properties.getString("MENU_FILE_CHANGE_USER"));
    mnChangeUser.setFont(font);
    mnChangeUser.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (listener != null) {
              listener.changeUser(new MenuEvent(this));
            }
          }
        });
    mnFile.add(mnChangeUser);

    //
    final JMenuItem mnExit = new JMenuItem(properties.getString("MENU_FILE_EXIT"));
    mnExit.setFont(font);
    mnExit.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (listener != null) {
              listener.exit(new MenuEvent(this));
            }
          }
        });
    mnFile.add(mnExit);

    // Right justify the rest of menu items
    // in order not to cover the main screen by the menu list.
    menuBar.add(Box.createHorizontalGlue());

    /*
     * ----------------------------- Execution ----------------------------- *
     */
    final JMenu mnExe = new JMenu(properties.getString("MENU_EXE"));
    mnExe.setMnemonic(KeyEvent.VK_E);
    mnExe.setFont(font);
    menuBar.add(mnExe);

    //
    final JMenuItem mnStartGuidance =
        new JMenuItem(properties.getString("MENU_EXE_START_GUIDANCE"));
    mnStartGuidance.setFont(font);
    mnStartGuidance.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (listener != null) {
              listener.startGuidance(new MenuEvent(this));
            }
          }
        });
    mnExe.add(mnStartGuidance);

    //
    final JMenuItem mnSkipRecordedPromptsPrev =
        new JMenuItem(properties.getString("MENU_EXE_SKIP_RECORDED_PROMPTS_PREV"));
    mnSkipRecordedPromptsPrev.setFont(font);
    mnSkipRecordedPromptsPrev.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (listener != null) {
              listener.skipRecordedPromptsPrev(new MenuEvent(this));
            }
          }
        });
    mnExe.add(mnSkipRecordedPromptsPrev);

    //
    final JMenuItem mnSkipRecordedPromptsNext =
        new JMenuItem(properties.getString("MENU_EXE_SKIP_RECORDED_PROMPTS_NEXT"));
    mnSkipRecordedPromptsNext.setFont(font);
    mnSkipRecordedPromptsNext.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (listener != null) {
              listener.skipRecordedPromptsNext(new MenuEvent(this));
            }
          }
        });
    mnExe.add(mnSkipRecordedPromptsNext);

    //
    if (properties.getBoolean("ENABLE_UPLOAD")) {
      final JMenuItem mnUploadVoice = new JMenuItem(properties.getString("MENU_EXE_UPLOAD_VOICE"));
      mnUploadVoice.setFont(font);
      mnUploadVoice.addActionListener(
          new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              if (listener != null) {
                listener.uploadVoice(new MenuEvent(this));
              }
            }
          });
      mnExe.add(mnUploadVoice);
    }

    /*
     * -------------------------------- View ------------------------------- *
     */
    final JMenu mnView = new JMenu(properties.getString("MENU_VIEW"));
    mnView.setMnemonic(KeyEvent.VK_V);
    mnView.setFont(font);
    menuBar.add(mnView);

    //
    final JMenuItem mnViewLog = new JMenuItem(properties.getString("MENU_VIEW_RECORD_LOG"));
    mnViewLog.setFont(font);
    mnViewLog.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (listener != null) {
              listener.showRecordLog(new MenuEvent(this));
            }
          }
        });
    mnView.add(mnViewLog);

    //
    final JMenuItem mnViewWaveform =
        new JMenuItem(properties.getString("MENU_VIEW_RECORD_WAVEFORM"));
    mnViewWaveform.setFont(font);
    mnViewWaveform.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (listener != null) {
              listener.showRecordWaveform(new MenuEvent(this));
            }
          }
        });
    mnView.add(mnViewWaveform);

    //
    final JMenuItem mnViewDir = new JMenuItem(properties.getString("MENU_VIEW_RECORD_DIR"));
    mnViewDir.setFont(font);
    mnViewDir.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (listener != null) {
              listener.showRecordDir(new MenuEvent(this));
            }
          }
        });
    mnView.add(mnViewDir);

    /*
     * ------------------------------- Option ------------------------------ *
     */
    final JMenu mnOption = new JMenu(properties.getString("MENU_OPTION"));
    mnOption.setMnemonic(KeyEvent.VK_O);
    mnOption.setFont(font);
    menuBar.add(mnOption);

    // //
    final JMenu mnOptionSubPlay = new JMenu(properties.getString("MENU_OPTION_SUB_PLAYBACK"));
    mnOptionSubPlay.setFont(font);
    mnOption.add(mnOptionSubPlay);

    //
    cbAutoSamplePlayback =
        new JCheckBoxMenuItem(properties.getString("MENU_OPTION_AUTO_SAMPLE_PLAYBACK"));
    cbAutoSamplePlayback.setFont(font);
    cbAutoSamplePlayback.addItemListener(
        new ItemListener() {
          @Override
          public void itemStateChanged(ItemEvent e) {
            if (listener != null && canSendCommand) {
              listener.changeConfigAutoSamplePlayback(new MenuEvent(this));
            }
          }
        });
    mnOptionSubPlay.add(cbAutoSamplePlayback);

    //
    cbAutoVoicePlayback =
        new JCheckBoxMenuItem(properties.getString("MENU_OPTION_AUTO_VOICE_PLAYBACK"));
    cbAutoVoicePlayback.setFont(font);
    cbAutoVoicePlayback.addItemListener(
        new ItemListener() {
          @Override
          public void itemStateChanged(ItemEvent e) {
            if (listener != null && canSendCommand) {
              listener.changeConfigAutoVoicePlayback(new MenuEvent(this));
            }
          }
        });
    mnOptionSubPlay.add(cbAutoVoicePlayback);

    //
    final JMenu mnBeepType = new JMenu(properties.getString("MENU_OPTION_BEEP_TYPE"));
    final ArrayList<String> beepType = new ArrayList<String>(3);
    beepType.add(properties.getString("MENU_OPTION_BEEP_TYPE_A"));
    beepType.add(properties.getString("MENU_OPTION_BEEP_TYPE_B"));
    beepType.add(properties.getString("MENU_OPTION_BEEP_TYPE_C"));
    mnBeepType.setFont(font);
    rdbtnBeepTypes = new JRadioButtonMenuItem[beepType.size()];
    for (int i = 0; i < rdbtnBeepTypes.length; i++) {
      rdbtnBeepTypes[i] = new JRadioButtonMenuItem(beepType.get(i));
      rdbtnBeepTypes[i].setName(beepType.get(i));
      rdbtnBeepTypes[i].setFont(font);
      rdbtnBeepTypes[i].addItemListener(
          new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
              final JRadioButtonMenuItem item = (JRadioButtonMenuItem) e.getSource();
              if (item.isSelected() && listener != null && canSendCommand) {
                // Extract the first character, A, B, or C.
                final String param = item.getName().substring(0, 1);
                listener.changeConfigBeepType(new MenuEvent(this, param));
              }
            }
          });
      mnBeepType.add(rdbtnBeepTypes[i]);
    }
    final ButtonGroup grpBeepType = new ButtonGroup();
    for (int i = 0; i < rdbtnBeepTypes.length; i++) {
      grpBeepType.add(rdbtnBeepTypes[i]);
    }
    mnOptionSubPlay.add(mnBeepType);

    // //
    final JMenu mnOptionSubRecord = new JMenu(properties.getString("MENU_OPTION_SUB_RECORD"));
    mnOptionSubRecord.setFont(font);
    mnOption.add(mnOptionSubRecord);

    //
    cbMaxAmplitudeRejection =
        new JCheckBoxMenuItem(properties.getString("MENU_OPTION_MAX_AMPLITUDE_REJECTION"));
    cbMaxAmplitudeRejection.setFont(font);
    cbMaxAmplitudeRejection.addItemListener(
        new ItemListener() {
          @Override
          public void itemStateChanged(ItemEvent e) {
            if (listener != null && canSendCommand) {
              listener.changeConfigMaxAmplitudeRejection(new MenuEvent(this));
            }
          }
        });
    mnOptionSubRecord.add(cbMaxAmplitudeRejection);

    //
    cbMinAmplitudeRejection =
        new JCheckBoxMenuItem(properties.getString("MENU_OPTION_MIN_AMPLITUDE_REJECTION"));
    cbMinAmplitudeRejection.setFont(font);
    cbMinAmplitudeRejection.addItemListener(
        new ItemListener() {
          @Override
          public void itemStateChanged(ItemEvent e) {
            if (listener != null && canSendCommand) {
              listener.changeConfigMinAmplitudeRejection(new MenuEvent(this));
            }
          }
        });
    mnOptionSubRecord.add(cbMinAmplitudeRejection);

    //
    final JMenu mnVad = new JMenu(properties.getString("MENU_OPTION_VAD_LEVEL"));
    final ArrayList<String> vadLevels = new ArrayList<String>(3);
    vadLevels.add(properties.getString("MENU_OPTION_VAD_LEVEL_A"));
    vadLevels.add(properties.getString("MENU_OPTION_VAD_LEVEL_B"));
    vadLevels.add(properties.getString("MENU_OPTION_VAD_LEVEL_C"));
    mnVad.setFont(font);
    rdbtnVadLevels = new JRadioButtonMenuItem[vadLevels.size()];
    for (int i = 0; i < rdbtnVadLevels.length; i++) {
      rdbtnVadLevels[i] = new JRadioButtonMenuItem(vadLevels.get(i));
      rdbtnVadLevels[i].setName(vadLevels.get(i));
      rdbtnVadLevels[i].setFont(font);
      rdbtnVadLevels[i].addItemListener(
          new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
              final JRadioButtonMenuItem item = (JRadioButtonMenuItem) e.getSource();
              if (item.isSelected() && listener != null && canSendCommand) {
                final String param = item.getName().substring(0, 1);
                listener.changeConfigVadLevel(new MenuEvent(this, param));
              }
            }
          });
      mnVad.add(rdbtnVadLevels[i]);
    }
    final ButtonGroup grpVadLevel = new ButtonGroup();
    for (int i = 0; i < rdbtnVadLevels.length; i++) {
      grpVadLevel.add(rdbtnVadLevels[i]);
    }
    mnOptionSubRecord.add(mnVad);

    // //
    final JMenu mnOptionSubPrompt = new JMenu(properties.getString("MENU_OPTION_SUB_PROMPT"));
    mnOptionSubPrompt.setFont(font);
    mnOption.add(mnOptionSubPrompt);

    final JMenu mnPromptRubyType = new JMenu(properties.getString("MENU_OPTION_PROMPT_RUBY_TYPE"));
    final ArrayList<String> rubyTypes = new ArrayList<String>(2);
    rubyTypes.add(properties.getString("MENU_OPTION_PROMPT_RUBY_TYPE_A"));
    rubyTypes.add(properties.getString("MENU_OPTION_PROMPT_RUBY_TYPE_B"));
    mnPromptRubyType.setFont(font);
    rdbtnPromptRubyTypes = new JRadioButtonMenuItem[rubyTypes.size()];
    for (int i = 0; i < rdbtnPromptRubyTypes.length; i++) {
      rdbtnPromptRubyTypes[i] = new JRadioButtonMenuItem(rubyTypes.get(i));
      rdbtnPromptRubyTypes[i].setName(rubyTypes.get(i));
      rdbtnPromptRubyTypes[i].setFont(font);
      rdbtnPromptRubyTypes[i].addItemListener(
          new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
              final JRadioButtonMenuItem item = (JRadioButtonMenuItem) e.getSource();
              if (item.isSelected() && listener != null && canSendCommand) {
                final String param = item.getName().substring(0, 1);
                listener.changeConfigPromptRubyType(new MenuEvent(this, param));
              }
            }
          });
      mnPromptRubyType.add(rdbtnPromptRubyTypes[i]);
    }
    final ButtonGroup grpPromptRubyType = new ButtonGroup();
    for (int i = 0; i < rdbtnPromptRubyTypes.length; i++) {
      grpPromptRubyType.add(rdbtnPromptRubyTypes[i]);
    }
    mnOptionSubPrompt.add(mnPromptRubyType);

    //
    fontTypes = new ArrayList<String>(50);
    String[] fonts =
        GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getAvailableFontFamilyNames(Locale.JAPANESE);
    for (int i = 0; i < fonts.length; i++) {
      if (fonts[i].equals("メイリオ")
          || fonts[i].startsWith("游")
          || (!fonts[i].startsWith("ＭＳ Ｐ") && fonts[i].startsWith("ＭＳ"))
          || (!fonts[i].contains("HGHangle")
              && !fonts[i].startsWith("HGP")
              && fonts[i].startsWith("HG"))
          || (!fonts[i].contains("UDP") && fonts[i].startsWith("BIZ "))
          || (!fonts[i].contains("NP-")
              && !fonts[i].contains("NK-")
              && fonts[i].startsWith("UD "))) {
        fontTypes.add(fonts[i]);
      }
    }
    fonts = null;
    final JMenu mnPromptFontType = new JMenu(properties.getString("MENU_OPTION_PROMPT_FONT_TYPE"));
    mnPromptFontType.setFont(font);
    rdbtnPromptFontTypes = new JRadioButtonMenuItem[fontTypes.size()];
    for (int i = 0; i < rdbtnPromptFontTypes.length; i++) {
      rdbtnPromptFontTypes[i] = new JRadioButtonMenuItem(fontTypes.get(i));
      rdbtnPromptFontTypes[i].setName(fontTypes.get(i));
      rdbtnPromptFontTypes[i].setFont(font);
      rdbtnPromptFontTypes[i].addItemListener(
          new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
              final JRadioButtonMenuItem item = (JRadioButtonMenuItem) e.getSource();
              if (item.isSelected() && listener != null && canSendCommand) {
                final String param = item.getName();
                listener.changeConfigPromptFontType(new MenuEvent(this, param));
              }
            }
          });
      mnPromptFontType.add(rdbtnPromptFontTypes[i]);
    }
    final ButtonGroup grpPromptFontType = new ButtonGroup();
    for (int i = 0; i < rdbtnPromptFontTypes.length; i++) {
      grpPromptFontType.add(rdbtnPromptFontTypes[i]);
    }
    mnOptionSubPrompt.add(mnPromptFontType);

    //
    final JMenu mnUpperBoundFontSize =
        new JMenu(properties.getString("MENU_OPTION_PROMPT_UPPER_BOUND_FONT_SIZE"));
    final JMenu mnLowerBoundFontSize =
        new JMenu(properties.getString("MENU_OPTION_PROMPT_LOWER_BOUND_FONT_SIZE"));
    mnUpperBoundFontSize.setFont(font);
    mnLowerBoundFontSize.setFont(font);
    final int numPromptFontSize = properties.getInteger("PROMPT_NUM_FONT_SIZE");
    final int gapPromptFontSize = properties.getInteger("PROMPT_GAP_FONT_SIZE");
    rdbtnPromptUpperBoundFontSizes = new JRadioButtonMenuItem[numPromptFontSize];
    rdbtnPromptLowerBoundFontSizes = new JRadioButtonMenuItem[numPromptFontSize];
    maxPromptFontSize =
        properties.getInteger("PROMPT_MIN_FONT_SIZE") + (numPromptFontSize - 1) * gapPromptFontSize;
    for (int i = 0; i < rdbtnPromptUpperBoundFontSizes.length; i++) {
      // Enumerate font sizes which can be selected by user in descending-order.
      final int size = maxPromptFontSize - gapPromptFontSize * i;
      rdbtnPromptUpperBoundFontSizes[i] = new JRadioButtonMenuItem(String.valueOf(size));
      rdbtnPromptUpperBoundFontSizes[i].setName(String.valueOf(size));
      rdbtnPromptUpperBoundFontSizes[i].setFont(font);
      rdbtnPromptLowerBoundFontSizes[i] = new JRadioButtonMenuItem(String.valueOf(size));
      rdbtnPromptLowerBoundFontSizes[i].setName(String.valueOf(size));
      rdbtnPromptLowerBoundFontSizes[i].setFont(font);
      rdbtnPromptUpperBoundFontSizes[i].addItemListener(
          new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
              final JRadioButtonMenuItem item = (JRadioButtonMenuItem) e.getSource();
              if (item.isSelected() && listener != null && canSendCommand) {
                final String param = item.getName();
                listener.changeConfigPromptUpperBoundFontSize(new MenuEvent(this, param));
              }
            }
          });
      rdbtnPromptLowerBoundFontSizes[i].addItemListener(
          new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
              final JRadioButtonMenuItem item = (JRadioButtonMenuItem) e.getSource();
              if (item.isSelected() && listener != null && canSendCommand) {
                final String param = item.getName();
                listener.changeConfigPromptLowerBoundFontSize(new MenuEvent(this, param));
              }
            }
          });
      mnUpperBoundFontSize.add(rdbtnPromptUpperBoundFontSizes[i]);
      mnLowerBoundFontSize.add(rdbtnPromptLowerBoundFontSizes[i]);
    }
    final ButtonGroup grpPromptUpperBoundFontSize = new ButtonGroup();
    final ButtonGroup grpPromptLowerBoundFontSize = new ButtonGroup();
    for (int i = 0; i < rdbtnPromptUpperBoundFontSizes.length; i++) {
      grpPromptUpperBoundFontSize.add(rdbtnPromptUpperBoundFontSizes[i]);
      grpPromptLowerBoundFontSize.add(rdbtnPromptLowerBoundFontSizes[i]);
    }
    mnOptionSubPrompt.add(mnUpperBoundFontSize);
    mnOptionSubPrompt.add(mnLowerBoundFontSize);

    //
    cbShortcutMouseWheel =
        new JCheckBoxMenuItem(properties.getString("MENU_OPTION_SHORTCUT_MOUSEWHEEL"));
    cbShortcutMouseWheel.setFont(font);
    cbShortcutMouseWheel.addItemListener(
        new ItemListener() {
          @Override
          public void itemStateChanged(ItemEvent e) {
            if (listener != null && canSendCommand) {
              listener.changeShortcutMouseWheel(new MenuEvent(this));
            }
          }
        });
    mnOptionSubPrompt.add(cbShortcutMouseWheel);

    //
    cbPromptLineBreakAtPP =
        new JCheckBoxMenuItem(properties.getString("MENU_OPTION_PROMPT_LINE_BREAK_AT_PP"));
    cbPromptLineBreakAtPP.setFont(font);
    cbPromptLineBreakAtPP.addItemListener(
        new ItemListener() {
          @Override
          public void itemStateChanged(ItemEvent e) {
            if (listener != null && canSendCommand) {
              listener.changeConfigPromptLineBreakAtPP(new MenuEvent(this));
            }
          }
        });
    mnOptionSubPrompt.add(cbPromptLineBreakAtPP);

    // //
    final JMenu mnOptionSubGuidance = new JMenu(properties.getString("MENU_OPTION_SUB_GUIDANCE"));
    mnOptionSubGuidance.setFont(font);
    mnOption.add(mnOptionSubGuidance);

    //
    final JMenu mnGuidanceFontSize =
        new JMenu(properties.getString("MENU_OPTION_GUIDANCE_FONT_SIZE"));
    mnGuidanceFontSize.setFont(font);
    final int numGuidanceFontSize = properties.getInteger("GUIDANCE_NUM_FONT_SIZE");
    final int gapGuidanceFontSize = properties.getInteger("GUIDANCE_GAP_FONT_SIZE");
    rdbtnGuidanceFontSizes = new JRadioButtonMenuItem[numGuidanceFontSize];
    maxGuidanceFontSize =
        properties.getInteger("GUIDANCE_MIN_FONT_SIZE")
            + (numGuidanceFontSize - 1) * gapGuidanceFontSize;
    for (int i = 0; i < rdbtnGuidanceFontSizes.length; i++) {
      final int size = maxGuidanceFontSize - gapGuidanceFontSize * i;
      rdbtnGuidanceFontSizes[i] = new JRadioButtonMenuItem(String.valueOf(size));
      rdbtnGuidanceFontSizes[i].setName(String.valueOf(size));
      rdbtnGuidanceFontSizes[i].setFont(font);
      rdbtnGuidanceFontSizes[i].addItemListener(
          new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
              final JRadioButtonMenuItem item = (JRadioButtonMenuItem) e.getSource();
              if (item.isSelected() && listener != null && canSendCommand) {
                final String param = item.getName();
                listener.changeConfigGuidanceFontSize(new MenuEvent(this, param));
              }
            }
          });
      mnGuidanceFontSize.add(rdbtnGuidanceFontSizes[i]);
    }
    final ButtonGroup grpGuidanceFontSize = new ButtonGroup();
    for (int i = 0; i < rdbtnGuidanceFontSizes.length; i++) {
      grpGuidanceFontSize.add(rdbtnGuidanceFontSizes[i]);
    }
    mnOptionSubGuidance.add(mnGuidanceFontSize);

    //
    cbStartupGuidance = new JCheckBoxMenuItem(properties.getString("MENU_OPTION_STARTUP_GUIDANCE"));
    cbStartupGuidance.setFont(font);
    cbStartupGuidance.addItemListener(
        new ItemListener() {
          @Override
          public void itemStateChanged(ItemEvent e) {
            if (listener != null && canSendCommand) {
              listener.changeConfigStartupGuidance(new MenuEvent(this));
            }
          }
        });
    mnOptionSubGuidance.add(cbStartupGuidance);

    // //
    final JMenu mnOptionSubShortcut = new JMenu(properties.getString("MENU_OPTION_SUB_SHORTCUT"));
    mnOptionSubShortcut.setFont(font);
    mnOption.add(mnOptionSubShortcut);

    //
    final JMenuItem mnShortcutPlaybackSample =
        new JMenuItem(properties.getString("MENU_OPTION_SHORTCUT_PLAYBACK_SAMPLE"));
    mnShortcutPlaybackSample.setFont(font);
    mnShortcutPlaybackSample.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (listener != null) {
              listener.changeShortcutPlaybackSample(new MenuEvent(this));
            }
          }
        });
    mnOptionSubShortcut.add(mnShortcutPlaybackSample);

    //
    final JMenuItem mnShortcutPlaybackVoice =
        new JMenuItem(properties.getString("MENU_OPTION_SHORTCUT_PLAYBACK_VOICE"));
    mnShortcutPlaybackVoice.setFont(font);
    mnShortcutPlaybackVoice.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (listener != null) {
              listener.changeShortcutPlaybackVoice(new MenuEvent(this));
            }
          }
        });
    mnOptionSubShortcut.add(mnShortcutPlaybackVoice);

    //
    final JMenuItem mnShortcutPlaybackVoiceTake2 =
        new JMenuItem(properties.getString("MENU_OPTION_SHORTCUT_PLAYBACK_VOICE_TAKE2"));
    mnShortcutPlaybackVoiceTake2.setFont(font);
    mnShortcutPlaybackVoiceTake2.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (listener != null) {
              listener.changeShortcutPlaybackVoiceTake2(new MenuEvent(this));
            }
          }
        });
    mnOptionSubShortcut.add(mnShortcutPlaybackVoiceTake2);

    //
    final JMenuItem mnShortcutRecord =
        new JMenuItem(properties.getString("MENU_OPTION_SHORTCUT_RECORD"));
    mnShortcutRecord.setFont(font);
    mnShortcutRecord.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (listener != null) {
              listener.changeShortcutRecord(new MenuEvent(this));
            }
          }
        });
    mnOptionSubShortcut.add(mnShortcutRecord);

    /*
     * -------------------------------- Help ------------------------------- *
     */
    final JMenu mnHelp = new JMenu(properties.getString("MENU_HELP"));
    mnHelp.setMnemonic(KeyEvent.VK_H);
    mnHelp.setFont(font);
    menuBar.add(mnHelp);

    //
    final JMenuItem mnShowAppVersion = new JMenuItem(properties.getString("MENU_HELP_VERSION"));
    mnShowAppVersion.setFont(font);
    mnShowAppVersion.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (listener != null) {
              listener.showVersion(new MenuEvent(this));
            }
          }
        });
    mnHelp.add(mnShowAppVersion);

    //
    if (properties.getBoolean("ENABLE_OPENJTALK")) {
      final JMenuItem mnShowOpenJTalk = new JMenuItem(properties.getString("MENU_HELP_OPENJTALK"));
      mnShowOpenJTalk.setFont(font);
      mnShowOpenJTalk.addActionListener(
          new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              if (listener != null) {
                listener.showOpenJTalk(new MenuEvent(this));
              }
            }
          });
      mnHelp.add(mnShowOpenJTalk);
    }

    System.gc();
  }

  public void setSelectedUpperBoundFontSize(int fontSize) {
    int index = (maxPromptFontSize - fontSize) / properties.getInteger("PROMPT_GAP_FONT_SIZE");
    rdbtnPromptUpperBoundFontSizes[index].setSelected(true);
  }

  public void setSelectedLowerBoundFontSize(int fontSize) {
    int index = (maxPromptFontSize - fontSize) / properties.getInteger("PROMPT_GAP_FONT_SIZE");
    rdbtnPromptLowerBoundFontSizes[index].setSelected(true);
  }

  public void setSelectedGuidanceFontSize(int fontSize) {
    int index = (maxGuidanceFontSize - fontSize) / properties.getInteger("GUIDANCE_GAP_FONT_SIZE");
    rdbtnGuidanceFontSizes[index].setSelected(true);
  }

  public boolean setSelectedPromptFontType(String fontType) {
    if (fontType == null) {
      return false;
    }

    int index;
    for (index = 0; index < fontTypes.size(); index++) {
      if (fontType.equals(fontTypes.get(index))) {
        break;
      }
    }
    if (index == fontTypes.size()) {
      index = 0;
    }

    rdbtnPromptFontTypes[index].setSelected(true);
    return true;
  }

  public boolean setSelectedPromptRubyType(String rubyType) {
    if (rubyType != null) {
      int index = rubyType.charAt(0) - 'A';
      if (0 <= index && index < rdbtnPromptRubyTypes.length) {
        rdbtnPromptRubyTypes[index].setSelected(true);
      }
      return true;
    }
    return false;
  }

  public boolean setSelectedBeepType(String beepType) {
    if (beepType != null) {
      int index = beepType.charAt(0) - 'A';
      if (0 <= index && index < rdbtnBeepTypes.length) {
        rdbtnBeepTypes[index].setSelected(true);
        return true;
      }
    }
    return false;
  }

  public boolean setSelectedVadLevel(String vadLevel) {
    if (vadLevel != null) {
      int index = vadLevel.charAt(0) - 'A';
      if (0 <= index && index < rdbtnVadLevels.length) {
        rdbtnVadLevels[index].setSelected(true);
        return true;
      }
    }
    return false;
  }

  public int getMinFontSize() {
    return properties.getInteger("PROMPT_MIN_FONT_SIZE");
  }

  public int getMaxFontSize() {
    return maxPromptFontSize;
  }

  public int getGapFontSize() {
    return properties.getInteger("PROMPT_GAP_FONT_SIZE");
  }

  public String getNextRubyType(String rubyType) {
    int index = rubyType.charAt(0) - 'A';
    if (index < 0 || rdbtnPromptRubyTypes.length <= index) {
      return rubyType;
    } else if (index == rdbtnPromptRubyTypes.length - 1) {
      index = 0;
    } else {
      ++index;
    }
    char c = (char) ('A' + index);
    return String.valueOf(c);
  }

  public String getPrevFontType(String fontType) {
    int index = fontTypes.indexOf(fontType);
    if (index == -1) {
      return fontType;
    } else if (index == 0) {
      index = fontTypes.size() - 1;
    } else {
      --index;
    }
    return fontTypes.get(index);
  }

  public String getNextFontType(String fontType) {
    int index = fontTypes.indexOf(fontType);
    if (index == -1) {
      return fontType;
    } else if (index == fontTypes.size() - 1) {
      index = 0;
    } else {
      ++index;
    }
    return fontTypes.get(index);
  }

  public boolean update(PropertiesIO properties) {
    if (properties == null) {
      return false;
    }
    canSendCommand = false;

    cbAutoSamplePlayback.setSelected(properties.getBoolean("AUTO_SAMPLE_PLAYBACK"));
    cbAutoVoicePlayback.setSelected(properties.getBoolean("AUTO_VOICE_PLAYBACK"));
    cbMaxAmplitudeRejection.setSelected(properties.getBoolean("MAX_AMPLITUDE_REJECTION"));
    cbMinAmplitudeRejection.setSelected(properties.getBoolean("MIN_AMPLITUDE_REJECTION"));
    cbShortcutMouseWheel.setSelected(properties.getBoolean("SHORTCUT_MOUSE_WHEEL"));
    cbPromptLineBreakAtPP.setSelected(properties.getBoolean("PROMPT_LINE_BREAK_AT_PP"));
    cbStartupGuidance.setSelected(properties.getBoolean("STARTUP_GUIDANCE"));

    setSelectedBeepType(properties.getString("BEEP_TYPE"));
    setSelectedGuidanceFontSize(properties.getInteger("GUIDANCE_FONT_SIZE"));
    setSelectedPromptFontType(properties.getString("PROMPT_FONT_TYPE"));
    setSelectedUpperBoundFontSize(properties.getInteger("PROMPT_UPPER_BOUND_FONT_SIZE"));
    setSelectedLowerBoundFontSize(properties.getInteger("PROMPT_LOWER_BOUND_FONT_SIZE"));
    setSelectedPromptRubyType(properties.getString("PROMPT_RUBY_TYPE"));
    setSelectedVadLevel(properties.getString("VAD_LEVEL"));

    canSendCommand = true;
    return true;
  }

  private void setFileChooserFont(Component[] components, Font font) {
    for (Component comp : components) {
      if (comp instanceof Container) {
        setFileChooserFont(((Container) comp).getComponents(), font);
      }
      comp.setFont(font);
    }
  }

  public void addMenuEventListener(MenuEventListener listener) {
    this.listener = listener;
  }

  public void removeMenuEventListener() {
    listener = null;
  }
}
