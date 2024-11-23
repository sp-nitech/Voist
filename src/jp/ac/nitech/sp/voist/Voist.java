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
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import jp.ac.nitech.sp.voist.Audio.PlaybackEvent;
import jp.ac.nitech.sp.voist.Audio.PlaybackResult;
import jp.ac.nitech.sp.voist.Audio.RecordingEvent;
import jp.ac.nitech.sp.voist.Buttons.ButtonType;

public class Voist implements ButtonEventListener, MenuEventListener {
  public static void main(String[] args) {
    final boolean canMultiInst = false;

    if (!canMultiInst) {
      try {
        final File f =
            new File(FileUtils.createPath(PlatformUtils.getMyDocumentDirectory(), ".lock"));
        final FileOutputStream fos = new FileOutputStream(f);
        final FileChannel fc = fos.getChannel();
        final FileLock fl = fc.tryLock();

        if (fl == null) {
          VoistUtils.warn("This application is already running", "main");
          return;
        }

        Runtime.getRuntime()
            .addShutdownHook(
                new Thread() {
                  @Override
                  public void run() {
                    if (fl != null && fl.isValid()) {
                      try {
                        fl.release();
                        fos.close();
                        f.delete();
                      } catch (IOException e) {
                        e.printStackTrace();
                      }
                    }
                  }
                });
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    EventQueue.invokeLater(
        new Runnable() {
          @Override
          public void run() {
            try {
              // Enable anti-aliasing.
              System.setProperty("awt.useSystemAAFontSettings", "on");
              Voist voist = new Voist();
              voist.frame.setVisible(true);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        });
  }

  /* -------------------------------- MAIN -------------------------------- */

  public static enum AppStates {
    NONE,
    READY,
    ASKING,
    GUIDANCE,
  }

  private static final int uploadThreshold = 0;

  private AppStates state;
  private PropertiesIO systemProperties;
  private ImageIcon icon;
  private UserSettings user;
  private UserInfoDialog userInfoDialog;
  private LicenseDialog licenseDialog;
  private Shortcut shortcut;
  private Audio audio;
  private Waveform waveform;
  private RecordInfoDialog recInfoDialog;
  private RecordSet recSet;

  private Timer timer;
  private KeyAdapter keyAdapter;

  // GUI components
  private Menu menu;
  private JFrame frame;
  private PromptIdList promptIdList;
  private AudioLevelMeter levelMeter;
  private Buttons buttons;
  private Guidance guidance;
  private PromptDraw promptDraw;

  // Booleans
  private boolean causesErrorByMaxAmplitude;
  private boolean causesErrorByMinAmplitude;
  private boolean pressesWindowsKey;
  private boolean pressesEnterKey;
  private boolean uploadsVoice;

  private Voist() {
    state = AppStates.READY;
    causesErrorByMaxAmplitude = false;
    causesErrorByMinAmplitude = false;
    pressesWindowsKey = false;
    pressesEnterKey = false;
    uploadsVoice = false;

    systemProperties = new PropertiesIO("res/properties/system.Voist.properties");
    try {
      systemProperties.load();
    } catch (IOException e) {
      e.printStackTrace();
    }

    icon = new ImageIcon("res/image/tnlab.png");

    user =
        new UserSettings(
            systemProperties.getString("PROPERTIES_USERSETTINGS"),
            FileUtils.createPath(
                PlatformUtils.getAppDirectory(systemProperties.getString("APP_NAME")),
                systemProperties.getString("SAVE_DIR")),
            icon);
    user.setName(systemProperties.getString("DEFAULT_USER_NAME"));
    user.hasChanged(); // Exhaust.

    userInfoDialog = new UserInfoDialog(systemProperties.getString("PROPERTIES_USERINFODIALOG"));
    licenseDialog = new LicenseDialog(systemProperties.getString("PROPERTIES_LICENSEDIALOG"));

    shortcut = new Shortcut(systemProperties.getString("PROPERTIES_SHORTCUT"), icon);
    shortcut.registerShortcut("EXIT", KeyEvent.VK_ESCAPE);
    shortcut.registerShortcut("GO_PREV", KeyEvent.VK_LEFT);
    shortcut.registerShortcut("GO_NEXT", KeyEvent.VK_RIGHT);
    shortcut.registerShortcut(
        "PLAYBACK_SAMPLE", user.getProperties().getInteger("SHORTCUT_PLAYBACK_SAMPLE"));
    shortcut.registerShortcut(
        "PLAYBACK_VOICE", user.getProperties().getInteger("SHORTCUT_PLAYBACK_VOICE"));
    shortcut.registerShortcut(
        "PLAYBACK_VOICE_TAKE2", user.getProperties().getInteger("SHORTCUT_PLAYBACK_VOICE_TAKE2"));
    shortcut.registerShortcut("RECORD", user.getProperties().getInteger("SHORTCUT_RECORD"));
    shortcut.registerShortcut("CHANGE_RUBY_TYPE", KeyEvent.VK_F1);
    shortcut.registerShortcut("CHANGE_FONT_TYPE_PREV", KeyEvent.VK_F2);
    shortcut.registerShortcut("CHANGE_FONT_TYPE_NEXT", KeyEvent.VK_F3);

    audio = new Audio();
    audio.setSampleRate(systemProperties.getInteger("SAMPLE_RATE"));
    audio.setSampleSize(systemProperties.getInteger("SAMPLE_SIZE"));
    audio.setNumChannels(systemProperties.getInteger("NUM_CHANNELS"));
    audio.setSampleRateForBeep(systemProperties.getInteger("SAMPLE_RATE_FOR_BEEP"));
    audio.setSampleSizeForBeep(systemProperties.getInteger("SAMPLE_SIZE_FOR_BEEP"));
    audio.setNumChannelsForBeep(systemProperties.getInteger("NUM_CHANNELS_FOR_BEEP"));
    audio.setSampleRateForSample(systemProperties.getInteger("SAMPLE_RATE_FOR_SAMPLE"));
    audio.setSampleSizeForSample(systemProperties.getInteger("SAMPLE_SIZE_FOR_SAMPLE"));
    audio.setNumChannelsForSample(systemProperties.getInteger("NUM_CHANNELS_FOR_SAMPLE"));
    audio.setMarginTime(systemProperties.getInteger("MARGIN_TIME_MILLI_SECONDS"));
    audio.setSilenceLevel(
        systemProperties.getDouble("VAD_LEVEL_" + user.getProperties().get("VAD_LEVEL")));
    audio.open();

    waveform = new Waveform(systemProperties.getString("PROPERTIES_WAVEFORM"), icon);

    recInfoDialog =
        new RecordInfoDialog(
            systemProperties.getString("PROPERTIES_RECORDINFODIALOG"),
            systemProperties.getInteger("SAMPLE_RATE"),
            systemProperties.getInteger("SAMPLE_SIZE"),
            systemProperties.getInteger("NUM_CHANNELS"),
            icon);

    recSet =
        new RecordSet(
            systemProperties.getString("PROPERTIES_RECORDSET"),
            FileUtils.createPath(
                PlatformUtils.getAppDirectory(systemProperties.getString("APP_NAME")),
                systemProperties.getString("SAVE_DIR")),
            FileUtils.createPath(
                PlatformUtils.getAppDirectory(systemProperties.getString("APP_NAME"))),
            systemProperties.getInteger("SAMPLE_RATE"),
            systemProperties.getInteger("SAMPLE_SIZE"),
            systemProperties.getInteger("NUM_CHANNELS"));
    recSet.setUserName(user.getName());
    changePromptSet(systemProperties.getString("DEFAULT_PROMPT_SET"), true);

    initGUI();
    final GuiUpdate gui = new GuiUpdate();
    gui.start();

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread() {
              @Override
              public void run() {
                gui.halt();
                levelMeter.stop();
                audio.close();
                recSet.dispose();
                user.storeProperties();
              }
            });

    setupKeyboard();
    setupMouse();

    goToPrompt(0);
    if (user.getProperties().getBoolean("STARTUP_GUIDANCE")) {
      startGuidance();
    }
    recInfoDialog.setVisible(true);
  }

  private void initGUI() {
    frame = new JFrame();
    frame.setName(systemProperties.getString("APP_NAME"));
    frame.setSize(
        systemProperties.getInteger("FRAME_WIDTH"), systemProperties.getInteger("FRAME_HEIGHT"));
    frame.setLocation(
        systemProperties.getInteger("FRAME_LOCATION_X"),
        systemProperties.getInteger("FRAME_LOCATION_Y"));
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setBackground(Color.WHITE);
    frame.setIconImage(icon.getImage());
    setFrameTitle();

    menu = new Menu(frame, systemProperties.getString("PROPERTIES_MENU"));
    menu.update(user.getProperties());
    menu.addMenuEventListener(this);

    Dimension dim = new Dimension(6, 6);
    JPanel pnlNorth = new JPanel();
    pnlNorth.setLayout(new BoxLayout(pnlNorth, BoxLayout.PAGE_AXIS));
    pnlNorth.setBackground(Color.WHITE);
    frame.getContentPane().add(pnlNorth, BorderLayout.NORTH);

    promptIdList =
        new PromptIdList(pnlNorth, systemProperties.getString("PROPERTIES_PROMPTIDLIST"), recSet);
    pnlNorth.add(Box.createRigidArea(dim));

    levelMeter = new AudioLevelMeter(pnlNorth, systemProperties.getInteger("SAMPLE_SIZE"));
    levelMeter.setRedLine(systemProperties.getDouble("LEVEL_METER_RED_LINE"));
    levelMeter.setGreenLine(systemProperties.getDouble("LEVEL_METER_GREEN_LINE"));
    pnlNorth.add(Box.createRigidArea(dim));

    buttons = new Buttons(pnlNorth, systemProperties.getString("PROPERTIES_BUTTONS"));
    pnlNorth.add(Box.createRigidArea(dim));
    buttons.changeState(state);
    buttons.addButtonEventListener(this);
    buttons.setShortcutText(ButtonType.GO_PREV, shortcut.getReadableChars("GO_PREV"));
    buttons.setShortcutText(ButtonType.GO_NEXT, shortcut.getReadableChars("GO_NEXT"));
    buttons.setShortcutText(
        ButtonType.PLAYBACK_SAMPLE, shortcut.getReadableChars("PLAYBACK_SAMPLE"));
    buttons.setShortcutText(ButtonType.PLAYBACK_VOICE, shortcut.getReadableChars("PLAYBACK_VOICE"));
    buttons.setShortcutText(ButtonType.RECORD, shortcut.getReadableChars("RECORD"));

    guidance = new Guidance(pnlNorth, systemProperties.getString("PROPERTIES_GUIDANCE"));
    guidance.setFontSize(user.getProperties().getInteger("GUIDANCE_FONT_SIZE"));

    JPanel pnlCenter = new JPanel();
    pnlCenter.setLayout(new BoxLayout(pnlCenter, BoxLayout.PAGE_AXIS));
    pnlCenter.setBackground(Color.WHITE);
    frame.getContentPane().add(pnlCenter, BorderLayout.CENTER);

    promptDraw =
        new PromptDraw(
            pnlCenter,
            systemProperties.getString("PROPERTIES_PROMPTDRAW"),
            menu.getMaxFontSize(),
            menu.getMinFontSize(),
            menu.getGapFontSize());
    promptDraw.setLowerBoundFontSize(
        user.getProperties().getInteger("PROMPT_LOWER_BOUND_FONT_SIZE"));
    promptDraw.setUpperBoundFontSize(
        user.getProperties().getInteger("PROMPT_UPPER_BOUND_FONT_SIZE"));
    promptDraw.setFontType(user.getProperties().getString("PROMPT_FONT_TYPE"));
    promptDraw.setRubyType(user.getProperties().getString("PROMPT_RUBY_TYPE"));
    promptDraw.setEnabledLineBreakAtPP(user.getProperties().getBoolean("PROMPT_LINE_BREAK_AT_PP"));
    promptDraw.setEnabledDrawing(true);

    levelMeter.start(audio, systemProperties.getInteger("FPS"));
  }

  private void setFrameTitle() {
    if (frame != null) {
      frame.setTitle(
          VoistUtils.joinStrings(
              null,
              systemProperties.getString("APP_NAME"),
              " [",
              systemProperties.getString("FRAME_TITLE_USER"),
              ": ",
              user.getName(),
              ", ",
              systemProperties.getString("FRAME_TITLE_PROMPT"),
              ": ",
              recSet.getPromptSetName(),
              "] ",
              String.valueOf(recSet.getSampleRate() / 1000),
              "kHz / ",
              String.valueOf(recSet.getNumChannels()),
              "ch / ",
              String.valueOf(recSet.getSampleSize() * 8),
              "bit"));
    }
  }

  private void setupKeyboard() {
    String command = shortcut.getCommand();
    int keyCode = shortcut.getKeyCode();

    if (command != null) {
      if (command.equals("PLAYBACK_SAMPLE")) {
        user.getProperties().setProperty("SHORTCUT_PLAYBACK_SAMPLE", keyCode);
        buttons.setShortcutText(ButtonType.PLAYBACK_SAMPLE, shortcut.getReadableChars(command));
      } else if (command.equals("PLAYBACK_VOICE")) {
        user.getProperties().setProperty("SHORTCUT_PLAYBACK_VOICE", keyCode);
        buttons.setShortcutText(ButtonType.PLAYBACK_VOICE, shortcut.getReadableChars(command));
      } else if (command.equals("RECORD")) {
        user.getProperties().setProperty("SHORTCUT_RECORD", keyCode);
        buttons.setShortcutText(ButtonType.RECORD, shortcut.getReadableChars(command));
      }
    }

    // Remove the previous key listener.
    if (keyAdapter != null) {
      frame.removeKeyListener(keyAdapter);
    }

    frame.addKeyListener(
        keyAdapter =
            new KeyAdapter() {
              @Override
              public void keyReleased(KeyEvent e) {
                int keyCode = e.getKeyCode();

                if (keyCode == shortcut.getKeyCode("EXIT")) {
                  exit(null);
                } else if (keyCode == shortcut.getKeyCode("CHANGE_RUBY_TYPE")) {
                  changeConfigPromptRubyType(menu.getNextRubyType(promptDraw.getRubyType()));
                } else if (keyCode == shortcut.getKeyCode("CHANGE_FONT_TYPE_PREV")) {
                  changeConfigPromptFontType(menu.getPrevFontType(promptDraw.getFontType()));
                } else if (keyCode == shortcut.getKeyCode("CHANGE_FONT_TYPE_NEXT")) {
                  changeConfigPromptFontType(menu.getNextFontType(promptDraw.getFontType()));
                } else if (keyCode == shortcut.getKeyCode("GO_NEXT")) {
                  if (!pressesWindowsKey) {
                    buttons.doClick(ButtonType.GO_NEXT);
                  }
                } else if (keyCode == shortcut.getKeyCode("GO_PREV")) {
                  if (!pressesWindowsKey) {
                    buttons.doClick(ButtonType.GO_PREV);
                  }
                } else if (keyCode == shortcut.getKeyCode("PLAYBACK_SAMPLE")) {
                  buttons.doClick(ButtonType.PLAYBACK_SAMPLE);
                } else if (keyCode == shortcut.getKeyCode("PLAYBACK_VOICE")) {
                  buttons.doClick(ButtonType.PLAYBACK_VOICE);
                } else if (keyCode == shortcut.getKeyCode("PLAYBACK_VOICE_TAKE2")) {
                  if (state == AppStates.ASKING) {
                    buttons.doClick(ButtonType.RECORD);
                  }
                } else if (keyCode == shortcut.getKeyCode("RECORD")) {
                  if ((state == AppStates.READY
                          || (state == AppStates.GUIDANCE && guidance.isRecordingTestStep())
                          || (state == AppStates.GUIDANCE && guidance.isEnvironmentTestStep()))
                      && (keyCode != KeyEvent.VK_ENTER
                          || (keyCode == KeyEvent.VK_ENTER && pressesEnterKey))) {
                    buttons.doClick(ButtonType.RECORD);
                  }
                }

                if (keyCode == KeyEvent.VK_WINDOWS) {
                  pressesWindowsKey = false;
                } else if (keyCode == KeyEvent.VK_ENTER) {
                  pressesEnterKey = false;
                }
              }

              @Override
              public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();

                if (keyCode == KeyEvent.VK_WINDOWS) {
                  pressesWindowsKey = true;
                } else if (keyCode == KeyEvent.VK_ENTER) {
                  pressesEnterKey = true; // for changePromptSet()
                }
              }
            });
  }

  private void setupMouse() {
    frame.addMouseWheelListener(
        new MouseAdapter() {
          @Override
          public void mouseWheelMoved(MouseWheelEvent e) {
            if (user.getProperties().getBoolean("SHORTCUT_MOUSE_WHEEL")) {
              changeConfigPromptUpperBoundFontSize(
                  promptDraw.getUpperBoundFontSize()
                      - menu.getGapFontSize() * e.getWheelRotation());
              menu.setSelectedUpperBoundFontSize(promptDraw.getUpperBoundFontSize());
            }
          }
        });
  }

  private class GuiUpdate extends Thread {
    private final long PERIOD = Math.round(1000.0 / systemProperties.getInteger("FPS"));

    private PlaybackEvent prevPlaybackEvent;
    private RecordingEvent prevRecordEvent;
    private boolean prevShortcutVisible;
    private boolean halt;

    public GuiUpdate() {
      prevPlaybackEvent = PlaybackEvent.OFF;
      prevRecordEvent = RecordingEvent.OFF;
      halt = false;
    }

    public void halt() {
      halt = true;
      interrupt();
    }

    @Override
    public void run() {
      VoistUtils.info("Run main loop.");
      while (!halt) {
        try {
          // Sleep to lower CPU usage. (This is very important process!)
          Thread.sleep(PERIOD);
        } catch (InterruptedException e) {
          VoistUtils.info("Halt main loop.");
        }

        // Playback
        PlaybackEvent nowPlaybackEvent = audio.getPlaybackEvent();
        if (nowPlaybackEvent != prevPlaybackEvent) {
          int line = (state == AppStates.ASKING) ? 2 : 1;
          switch (nowPlaybackEvent) {
            case OFF:
              if (prevPlaybackEvent == PlaybackEvent.SAMPLE) {
                String key = "INSTRUCTION_TEXT_SAMPLE_STOP";
                if (state == AppStates.ASKING) {
                  key = key + "_2";
                }
                guidance.setText(systemProperties.getString(key), line, false);
                buttons.setMainText(ButtonType.PLAYBACK_SAMPLE, "A");
              } else if (prevPlaybackEvent == PlaybackEvent.VOICE_CUT) {
                String key = "INSTRUCTION_TEXT_VOICE_STOP";
                if (state == AppStates.ASKING) {
                  key = key + "_2";
                } else if (guidance.isRecordingTestStep()) {
                  key = key + "_3";
                }
                guidance.setText(systemProperties.getString(key), line, false);
                buttons.setMainText(ButtonType.PLAYBACK_VOICE, "A");
              } else if (prevPlaybackEvent == PlaybackEvent.VOICE_TMP) {
                String key = "INSTRUCTION_TEXT_VOICE_STOP";
                if (state == AppStates.ASKING) {
                  key = key + "_2";
                }
                guidance.setText(systemProperties.getString(key), line, false);
                buttons.setMainText(ButtonType.RECORD, "A");
              }

              // Recording environment sound should be stopped automatically.
              if (!(guidance.isEnvironmentTestStep()
                  && audio.getRecordingEvent() == Audio.RecordingEvent.ON)) {
                buttons.setEnabled(ButtonType.RECORD, true);
              }
              break;
            case SAMPLE:
              guidance.setText(
                  systemProperties.getString("INSTRUCTION_TEXT_SAMPLE_PLAYBACK"), line, false);
              buttons.setMainText(ButtonType.PLAYBACK_SAMPLE, "B");
              buttons.setMainText(ButtonType.PLAYBACK_VOICE, "A");
              if (state != AppStates.ASKING) {
                buttons.setEnabled(ButtonType.RECORD, false);
              } else {
                buttons.setMainText(ButtonType.RECORD, "A");
              }
              break;
            case VOICE_CUT:
              guidance.setText(
                  systemProperties.getString("INSTRUCTION_TEXT_VOICE_PLAYBACK"), line, false);
              buttons.setMainText(ButtonType.PLAYBACK_VOICE, "B");
              buttons.setMainText(ButtonType.PLAYBACK_SAMPLE, "A");
              if (state != AppStates.ASKING) {
                buttons.setEnabled(ButtonType.RECORD, false);
              } else {
                buttons.setMainText(ButtonType.RECORD, "A");
              }
              break;
            case VOICE_TMP:
              guidance.setText(
                  systemProperties.getString("INSTRUCTION_TEXT_VOICE_PLAYBACK"), line, false);
              buttons.setMainText(ButtonType.RECORD, "B");
              buttons.setMainText(ButtonType.PLAYBACK_SAMPLE, "A");
              buttons.setMainText(ButtonType.PLAYBACK_VOICE, "A");
              break;
            default:
              break;
          }
          prevPlaybackEvent = nowPlaybackEvent;
        }

        // Record
        RecordingEvent nowRecordEvent = audio.getRecordingEvent();
        if (nowRecordEvent != prevRecordEvent) {
          switch (nowRecordEvent) {
            case OFF:
              buttons.setMainText(ButtonType.RECORD, "A");

              RecordInfo prev = recSet.getRecordInfo();
              RecordInfo now = recSet.getRecordTmpInfo();
              updateRecordInfoWindow();

              if (causesErrorByMaxAmplitude) {
                now.setStatus(RecordInfo.RecordStatus.FAILURE_MAX_AMPLITUDE);
              } else if (causesErrorByMinAmplitude) {
                now.setStatus(RecordInfo.RecordStatus.FAILURE_MIN_AMPLITUDE);
              }
              RecordInfo.RecordStatus status = now.getStatus();

              String baseKey = "INSTRUCTION_TEXT_";
              if (status == RecordInfo.RecordStatus.SUCCESS) {
                if (state == AppStates.GUIDANCE) {
                  recSet.overwrite();
                  recSet.dumpLog();
                  promptIdList.update(true);
                  if (guidance.isRecordingTestStep()) {
                    guidance.setTextRecordingTest();
                  } else if (guidance.isEnvironmentTestStep()) {
                    double snr = recSet.getRecordInfo(0).getPower() - now.getPower();
                    VoistUtils.info("");
                    System.out.printf("SNR: %.1f dB\n", snr);
                    guidance.setTextEnvironmentTest(snr, systemProperties.getDouble("GOOD_SNR"));
                  }
                } else if (prev.isRecorded()) {
                  changeMode(AppStates.READY, AppStates.ASKING);
                  guidance.setText(systemProperties.getString(baseKey + "ASKING"), 1, true);
                  buttons.setEnabled(true);
                  buttons.setShortcutText(
                      ButtonType.RECORD, shortcut.getReadableChars("PLAYBACK_VOICE_TAKE2"));
                } else {
                  recSet.overwrite();
                  recSet.dumpLog();
                  recSet.incNumRecordedPrompts();
                  guidance.setText(systemProperties.getString(baseKey + "SUCCESS"), 1, false);
                  buttons.setEnabled(true);
                  promptIdList.update(true);
                  promptIdList.setEnabled(true);
                  recInfoDialog.setProgress(recSet.getNumRecordedPrompts(), recSet.getNumPrompts());
                }

                checkButtons();
                if (user.getProperties().getBoolean("AUTO_VOICE_PLAYBACK") && !prev.isEnv()) {
                  playbackVoice(true);
                }
              } else {
                baseKey += "FAILURE_";
                switch (status) {
                  case FAILURE:
                    baseKey += "UNKNOWN_";
                    break;
                  case FAILURE_END_SILENCE:
                    baseKey += "END_SILENCE_";
                    break;
                  case FAILURE_MAX_AMPLITUDE:
                    baseKey += "MAX_AMPLITUDE_";
                    break;
                  case FAILURE_MIN_AMPLITUDE:
                    baseKey += "MIN_AMPLITUDE_";
                    break;
                  case FAILURE_TOP_SILENCE:
                    baseKey += "TOP_SILENCE_";
                    break;
                  default:
                    break;
                }

                guidance.setText(systemProperties.getString(baseKey + 1), 1, true);
                guidance.setText(systemProperties.getString(baseKey + 2), 2, false);
                buttons.setEnabled(true);
                promptIdList.setEnabled(true);
                checkButtons();
              }
              break;
            case ON:
              guidance.setText(systemProperties.getString("INSTRUCTION_TEXT_RECORD_1"), 1, true);
              if (recSet.getRecordInfo().isEnv()) {
                guidance.setText(
                    systemProperties.getString("INSTRUCTION_TEXT_RECORD_2_B"), 2, false);
              } else {
                guidance.setText(
                    systemProperties.getString("INSTRUCTION_TEXT_RECORD_2_A"), 2, false);
              }
              buttons.setMainText(ButtonType.RECORD, "B");
              buttons.setEnabled(false, false, true, false, false);
              if (recSet.getRecordInfo().isEnv()) {
                buttons.setEnabled(ButtonType.RECORD, false);
              }
              promptIdList.setEnabled(false);
              break;
            default:
              break;
          }
          prevRecordEvent = nowRecordEvent;
        }

        if (nowRecordEvent == RecordingEvent.ON) {
          if (user.getProperties().getBoolean("MAX_AMPLITUDE_REJECTION")
              && levelMeter.isRed()
              && !recSet.getRecordInfo().isEnv()) {
            causesErrorByMaxAmplitude = true;
            record();
            audio.playback(
                FileUtils.createPath(
                    recSet.getRecordingDirectoryName(PlaybackEvent.BEEP),
                    systemProperties.getString("BEEP_FILE_TWICE")),
                PlaybackEvent.BEEP);
          }
          if (user.getProperties().getBoolean("MIN_AMPLITUDE_REJECTION")
              && (levelMeter.isGreen() || levelMeter.isRed())) {
            causesErrorByMinAmplitude = false;
          }
        }

        if (!shortcut.isVisible()) {
          if (prevShortcutVisible && shortcut.getCommand() != null) {
            setupKeyboard();
          }
        }
        prevShortcutVisible = shortcut.isVisible();

        int index = promptIdList.getIndexOfSelectedItem();
        if (!promptIdList.isSelected() && index != recSet.getPosition()) {
          goToPrompt(index);
        }

        if (user.hasChanged()) {
          setFrameTitle();
          recSet.setUserName(user.getName());
          changePromptSet(systemProperties.getString("DEFAULT_PROMPT_SET"), false);
          menu.update(user.getProperties());
          promptDraw.setUpperBoundFontSize(
              user.getProperties().getInteger("PROMPT_UPPER_BOUND_FONT_SIZE"));
          promptDraw.setLowerBoundFontSize(
              user.getProperties().getInteger("PROMPT_LOWER_BOUND_FONT_SIZE"));
          promptDraw.setFontType(user.getProperties().getString("PROMPT_FONT_TYPE"));
          promptDraw.setRubyType(user.getProperties().getString("PROMPT_RUBY_TYPE"));
        }
      }
    }
  }

  private void changePromptSet(String fileName, boolean first) {
    if (state != AppStates.READY) {
      guidance.setText(systemProperties.getString("INSTRUCTION_TEXT_PROMPT_CANNOT_LOAD"), 1, false);
      return;
    }

    if (fileName == null) {
      VoistUtils.warn("This is null prompt set", "changePromptSet");
      return;
    }

    if (!first) {
      guidance.setText(systemProperties.getString("INSTRUCTION_TEXT_PROMPT_LOADING"), 1, false);
      promptDraw.setEnabledDrawing(false);
      promptIdList.setEnabled(false);
      buttons.setEnabled(false);
    }

    if (!recSet.read(fileName, false)) {
      exit(null);
    }
    setFrameTitle();

    if (!first) {
      promptIdList.reset();
      promptIdList.setEnabled(true);
      promptDraw.setEnabledDrawing(true);
      buttons.setEnabled(true);
      goToPrompt(0);
    }

    recInfoDialog.setProgress(recSet.getNumRecordedPrompts(), recSet.getNumPrompts());
  }

  private void startGuidance() {
    if (state == AppStates.GUIDANCE) {
      changeMode(AppStates.GUIDANCE, AppStates.READY);
      changePromptSet(systemProperties.getString("DEFAULT_PROMPT_SET"), false);
      guidance.end();
    } else if (state == AppStates.READY) {
      changePromptSet(systemProperties.getString("GUIDANCE_PROMPT_SET"), false);
      recSet.getRecordInfo(1).toEnv(true);
      changeMode(AppStates.READY, AppStates.GUIDANCE);
      guidance.start();
    } else {
      guidance.setText(
          systemProperties.getString("INSTRUCTION_TEXT_GUIDANCE_CANNOT_START"), 1, false);
    }
  }

  private void goToPrompt(int position) {
    if (recSet.setPosition(position)) {
      go(false);
    }
  }

  private void go(boolean playSample) {
    checkButtons();

    if (state == AppStates.READY) {
      if (user.getProperties().getBoolean("AUTO_SAMPLE_PLAYBACK") && playSample) {
        playbackSample();
      } else {
        if (recSet.getRecordInfo().isRecorded()) {
          guidance.setText(
              systemProperties.getString("INSTRUCTION_TEXT_PLEASE_PUSH_BUTTON_B"), 1, false);
        } else {
          guidance.setText(
              systemProperties.getString("INSTRUCTION_TEXT_PLEASE_PUSH_BUTTON_A"), 1, false);
        }
      }
    }

    promptDraw.setPrompt(recSet.getPrompt());
    promptIdList.selectItemAt(recSet.getPosition());
    updateRecordInfoWindow();
  }

  private void checkButtons() {
    if (state == AppStates.READY) {
      if (recSet.getPosition() == 0) {
        buttons.setEnabled(ButtonType.GO_PREV, false);
      } else {
        buttons.setEnabled(ButtonType.GO_PREV, true);
      }

      if (recSet.getPosition() == recSet.getNumPrompts() - 1) {
        buttons.setEnabled(ButtonType.GO_NEXT, false);
      } else {
        buttons.setEnabled(ButtonType.GO_NEXT, true);
      }

      if (recSet.getRecordInfo().isRecorded()) {
        buttons.setMainText(ButtonType.RECORD, "C");
        buttons.setEnabled(ButtonType.PLAYBACK_VOICE, true);
      } else {
        buttons.setMainText(ButtonType.RECORD, "A");
        buttons.setEnabled(ButtonType.PLAYBACK_VOICE, false);
      }
    } else if (state == AppStates.GUIDANCE) {
      if (guidance.isFirstStep()) {
        buttons.setEnabled(ButtonType.GO_PREV, false);
      } else {
        buttons.setEnabled(ButtonType.GO_PREV, true);
      }

      if (guidance.isEndStep()) {
        buttons.setEnabled(ButtonType.GO_NEXT, false);
      } else {
        buttons.setEnabled(ButtonType.GO_NEXT, true);
      }

      if (guidance.isRecordingTestStep() || guidance.isEnvironmentTestStep()) {
        buttons.setEnabled(ButtonType.RECORD, true);
      } else {
        buttons.setEnabled(ButtonType.RECORD, false);
      }

      if (guidance.isRecordingTestStep() && recSet.getRecordInfo().isRecorded()) {
        buttons.setEnabled(ButtonType.PLAYBACK_VOICE, true);
      } else {
        buttons.setEnabled(ButtonType.PLAYBACK_VOICE, false);
      }

      buttons.setEnabled(ButtonType.PLAYBACK_SAMPLE, false);
    }
  }

  private void record() {
    if (audio.getRecordingEvent() == Audio.RecordingEvent.OFF) {
      levelMeter.clear();
      causesErrorByMaxAmplitude = false;
      causesErrorByMinAmplitude =
          recSet.getRecordInfo().isEnv()
              ? false
              : user.getProperties().getBoolean("MIN_AMPLITUDE_REJECTION");
    }

    String beepPath = null;
    if (user.getProperties().getString("BEEP_TYPE").equals("A")) {
      beepPath =
          FileUtils.createPath(
              recSet.getRecordingDirectoryName(PlaybackEvent.BEEP),
              systemProperties.getString("BEEP_FILE_LONG"));
    } else if (user.getProperties().getString("BEEP_TYPE").equals("B")) {
      beepPath =
          FileUtils.createPath(
              recSet.getRecordingDirectoryName(PlaybackEvent.BEEP),
              systemProperties.getString("BEEP_FILE_SHORT"));
    }

    audio.record(recSet.getRecordTmpInfo(), beepPath, recSet.getRecordInfo().isEnv());

    if (recSet.getRecordInfo().isEnv()) {
      try {
        autoStop();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private void playbackSample() {
    if (audio.playback(
                FileUtils.createPath(
                    recSet.getRecordingDirectoryName(PlaybackEvent.SAMPLE), recSet.getFileName()),
                PlaybackEvent.SAMPLE)
            == PlaybackResult.ERROR
        && audio.playback(
                FileUtils.createPath("res/sample", recSet.getFileName()), PlaybackEvent.SAMPLE)
            == PlaybackResult.ERROR) {
      int line = (state == AppStates.ASKING) ? 2 : 1;
      guidance.setText(
          systemProperties.getString("INSTRUCTION_TEXT_SAMPLE_NOT_FOUND"), line, false);
    }
  }

  private void playbackVoice(boolean take2) {
    if (take2) {
      audio.playback(
          recSet.getRecordTmpInfo().getVoiceCutFile().getAbsolutePath(), PlaybackEvent.VOICE_TMP);
    } else {
      audio.playback(
          FileUtils.createPath(
              recSet.getRecordingDirectoryName(PlaybackEvent.VOICE_CUT), recSet.getFileName()),
          PlaybackEvent.VOICE_CUT);
    }
  }

  private void changeMode(AppStates from, AppStates to) {
    state = to;
    buttons.changeState(state);

    if (from == AppStates.READY) {
      if (to == AppStates.ASKING) {
        promptIdList.setEnabled(false);
        buttons.setEnabled(true);
      } else if (to == AppStates.GUIDANCE) {
        promptIdList.setEnabled(false);
        buttons.setEnabled(false, false, false, false, true);
      }
    } else if (from == AppStates.ASKING) {
      if (to == AppStates.READY) {
        promptIdList.setEnabled(true);
        checkButtons();
      }
    } else if (from == AppStates.GUIDANCE) {
      if (to == AppStates.READY) {
        promptIdList.setEnabled(true);
        checkButtons();
      }
    }
  }

  private void updateRecordInfoWindow() {
    recInfoDialog.update(recSet.getRecordInfo(), true);
    recInfoDialog.update(recSet.getRecordTmpInfo(), false);
  }

  private void autoStop() throws InterruptedException {
    TimerTask task =
        new TimerTask() {
          @Override
          public void run() {
            record();
            timer.cancel();
          }
        };
    timer = new Timer();
    timer.schedule(
        task,
        TimeUnit.SECONDS.toMillis(systemProperties.getInteger("ENVIRONMENT_RECORDING_SECONDS")));
  }

  private class ZipUploader extends SwingWorker<String, String> {
    private File srcDir;
    private File zipFile;
    private boolean success;
    private int numSentences;

    public ZipUploader(File dir, File file) {
      srcDir = dir;
      zipFile = file;
    }

    @Override
    protected String doInBackground() throws Exception {
      success = false;

      if (srcDir != null && zipFile != null) {
        uploadsVoice = true;
        numSentences = recSet.getNumRecordedPrompts();

        VoistUtils.info(
            "Start zipping.",
            "From: " + srcDir.getAbsolutePath(),
            "To: " + zipFile.getAbsolutePath());
        ZipCompression zc = new ZipCompression();
        if (zc.zip(srcDir, zipFile)) {
          VoistUtils.info("Speech files are successfully zipped.");
        } else {
          VoistUtils.warn("Cannot zip speech files", "doInBackground");
          return null;
        }

        VoistUtils.info(
            "Start uploading.",
            "From: " + zipFile.getAbsolutePath(),
            "To: " + systemProperties.getString("UPLOAD_URL"));
        FileUpload fu = new FileUpload(systemProperties.getString("UPLOAD_URL"));
        fu.put("user-name", userInfoDialog.getUserName());
        fu.put("voice-name", userInfoDialog.getVoiceName());
        fu.put("voice-kana-name", userInfoDialog.getVoiceKanaName());
        fu.put("email-address", userInfoDialog.getEmailAddress());
        fu.put("location", userInfoDialog.getLocalLocation());
        fu.put("dialect", userInfoDialog.getDialect());
        fu.put("occupation", userInfoDialog.getOccupation());
        fu.put("sex", userInfoDialog.getSexInEnglish());
        fu.put("age-group", userInfoDialog.getAgeGroup());
        fu.put("smoking", userInfoDialog.getSmoking());
        fu.put("recording-environment", userInfoDialog.getRecordingEnvironment());
        fu.put("recording-equipment", userInfoDialog.getRecordingEquipment());
        fu.put("voice-building-flag", userInfoDialog.getVoiceBuildingFlag());
        fu.put("note", userInfoDialog.getNote());
        fu.put("locale", userInfoDialog.getDefaultLocale());
        fu.put("toolkit-version", systemProperties.getString("APP_RELEASED"));
        fu.put("checksum", FileUpload.calculateChecksum(zipFile.getAbsolutePath()));

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        fu.put("time-stamp", sdf.format(cal.getTime()));

        if (fu.upload(zipFile, "nvbank" + userInfoDialog.getUserName())) {
          VoistUtils.info("Speech files are successfully uploaded.");
          success = true;
        } else {
          VoistUtils.warn("Cannot upload speech files", "doInBackground");
        }
      }
      return null;
    }

    @Override
    protected void done() {
      uploadsVoice = false;

      if (success) {
        JOptionPane.showMessageDialog(
            null,
            systemProperties.getString("UPLOAD_TEXT_DONE_SUCCESS"),
            systemProperties.getString("UPLOAD_DIALOG_TITLE"),
            JOptionPane.INFORMATION_MESSAGE);

        user.getProperties().setProperty("NUM_UPLOADED_SENTENCES", numSentences);
      } else {
        JOptionPane.showMessageDialog(
            null,
            systemProperties.getString("UPLOAD_TEXT_DONE_FAILURE"),
            systemProperties.getString("UPLOAD_DIALOG_TITLE"),
            JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  @Override
  public void exit(MenuEvent e) {
    if (uploadsVoice) {
      int option =
          JOptionPane.showConfirmDialog(
              null,
              systemProperties.getString("UPLOAD_TEXT_QUESTION_2"),
              systemProperties.getString("UPLOAD_DIALOG_TITLE"),
              JOptionPane.YES_NO_OPTION,
              JOptionPane.QUESTION_MESSAGE);
      if (option != JOptionPane.YES_OPTION) {
        return;
      }
    }
    System.exit(0);
  }

  @Override
  public void changeConfigBeepType(MenuEvent e) {
    user.getProperties().setProperty("BEEP_TYPE", e.getParam());
  }

  @Override
  public void changePromptSet(MenuEvent e) {
    changePromptSet(e.getParam(), false);
  }

  @Override
  public void changeUser(MenuEvent e) {
    if (state != AppStates.READY) {
      guidance.setText(systemProperties.getString("INSTRUCTION_TEXT_USER_CANNOT_CHANGE"), 1, false);
      return;
    }
    user.setVisible(true);
  }

  @Override
  public void startGuidance(MenuEvent e) {
    startGuidance();
  }

  @Override
  public void skipRecordedPromptsPrev(MenuEvent e) {
    if (state != AppStates.READY || audio.getRecordingEvent() == RecordingEvent.ON) {
      return;
    }

    int index = recSet.checkComplete(false);
    if (index == -1) {
      guidance.setText(systemProperties.getString("INSTRUCTION_TEXT_PROMPT_COMPLETE"), 1, false);
    } else {
      goToPrompt(index);
    }
  }

  @Override
  public void skipRecordedPromptsNext(MenuEvent e) {
    if (state != AppStates.READY || audio.getRecordingEvent() == RecordingEvent.ON) {
      return;
    }

    int index = recSet.checkComplete(true);
    if (index == -1) {
      guidance.setText(systemProperties.getString("INSTRUCTION_TEXT_PROMPT_COMPLETE"), 1, false);
    } else {
      goToPrompt(index);
    }
  }

  @Override
  public void uploadVoice(MenuEvent e) {
    if (uploadsVoice) {
      guidance.setText(systemProperties.getString("UPLOAD_TEXT_UPLOADING"), 1, true);
      return;
    }

    if (recSet.getNumRecordedPrompts() < uploadThreshold) {
      JOptionPane.showMessageDialog(
          null,
          systemProperties.getString("UPLOAD_TEXT_FAILED_LACK_OF_DATA1"),
          systemProperties.getString("UPLOAD_DIALOG_TITLE"),
          JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    if (user.getProperties().getInteger("NUM_UPLOADED_SENTENCES") != 0
        && recSet.getNumRecordedPrompts()
            < user.getProperties().getInteger("NUM_UPLOADED_SENTENCES") + uploadThreshold) {
      String message =
          systemProperties.getString("UPLOAD_TEXT_FAILED_LACK_OF_DATA2_1")
              + (user.getProperties().getInteger("NUM_UPLOADED_SENTENCES")
                  + uploadThreshold
                  - recSet.getNumRecordedPrompts())
              + systemProperties.getString("UPLOAD_TEXT_FAILED_LACK_OF_DATA2_2");
      JOptionPane.showMessageDialog(
          null,
          message,
          systemProperties.getString("UPLOAD_DIALOG_TITLE"),
          JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    licenseDialog.setVisible(true);
    if (!licenseDialog.isAgreed()) {
      return;
    }

    userInfoDialog.setVisible(true);
    if (!userInfoDialog.isEntered()) {
      return;
    }

    String message =
        systemProperties.getString("UPLOAD_TEXT_QUESTION_1")
            + "\n　　　ユーザ名："
            + userInfoDialog.getUserName()
            + "\n　希望ボイス名："
            + userInfoDialog.getVoiceName()
            + ((userInfoDialog.getVoiceKanaName().equals(""))
                ? ""
                : "（" + userInfoDialog.getVoiceKanaName() + "）")
            + "\nメールアドレス："
            + userInfoDialog.getEmailAddress()
            + "\n　　　　　方言："
            + userInfoDialog.getDialect()
            + "\n　　　　出身地："
            + userInfoDialog.getLocalLocation()
            + "\n　　　　　職業："
            + userInfoDialog.getOccupation()
            + "\n　　　　　性別："
            + userInfoDialog.getSex()
            + "\n　　　　　年代："
            + userInfoDialog.getAgeGroup()
            + "\n　　　　喫煙歴："
            + userInfoDialog.getSmoking()
            + "\n　　　収録環境："
            + userInfoDialog.getRecordingEnvironment()
            + "\n　　　収録機材："
            + userInfoDialog.getRecordingEquipment()
            + "\n　　　　　備考："
            + userInfoDialog.getNote()
            + "\n　　　収録文数："
            + recSet.getNumRecordedPrompts();

    int option =
        JOptionPane.showConfirmDialog(
            null,
            message,
            systemProperties.getString("UPLOAD_DIALOG_TITLE"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);

    if (option == JOptionPane.YES_OPTION) {
      JOptionPane.showMessageDialog(
          null,
          systemProperties.getString("UPLOAD_TEXT_OK"),
          systemProperties.getString("UPLOAD_DIALOG_TITLE"),
          JOptionPane.INFORMATION_MESSAGE);

      File uploadFile =
          new File(
              FileUtils.createPath(
                  recSet.getRecordingDirectoryName(null),
                  systemProperties.getString("UPLOAD_FILE")));

      ZipUploader zu =
          new ZipUploader(
              new File(recSet.getRecordingDirectoryName(PlaybackEvent.VOICE_WAV)).getParentFile(),
              uploadFile);
      zu.execute();
    } else {
      JOptionPane.showMessageDialog(
          null,
          systemProperties.getString("UPLOAD_TEXT_CANCEL"),
          systemProperties.getString("UPLOAD_DIALOG_TITLE"),
          JOptionPane.INFORMATION_MESSAGE);
    }
  }

  @Override
  public void showRecordLog(MenuEvent e) {
    boolean b = recInfoDialog.isVisible();
    recInfoDialog.setVisible(!b);
  }

  @Override
  public void showRecordWaveform(MenuEvent e) {
    if (waveform.isVisible()) {
      waveform.setVisible(false);
    } else {
      RecordInfo log = recSet.getRecordInfo();
      if (log.isRecorded()) {
        waveform.draw(
            FileUtils.createPath(
                recSet.getRecordingDirectoryName(PlaybackEvent.VOICE_CUT), recSet.getFileName()),
            (int) log.getFileLength(),
            recSet.getPromptID(),
            systemProperties.getInteger("SAMPLE_SIZE"),
            systemProperties.getInteger("NUM_CHANNELS"));
      } else {
        guidance.setText(
            systemProperties.getString("INSTRUCTION_TEXT_WAVEFORM_CANNOT_SHOW"), 1, false);
      }
    }
  }

  @Override
  public void showRecordDir(MenuEvent e) {
    try {
      Desktop.getDesktop()
          .open(new File(recSet.getRecordingDirectoryName(PlaybackEvent.VOICE_WAV)));
    } catch (IOException e1) {
      e1.printStackTrace();
    } catch (UnsupportedOperationException e1) {
      e1.printStackTrace();
    }
  }

  @Override
  public void showVersion(MenuEvent e) {
    String str =
        VoistUtils.joinStrings(
            null,
            " ",
            systemProperties.getString("APP_NAME"),
            "\n",
            " ",
            "Version: ",
            systemProperties.getString("APP_RELEASED"),
            "\n\n",
            " ",
            systemProperties.getString("COPYRIGHT"),
            "\n",
            " ",
            "Homepage: ",
            systemProperties.getString("HOME_PAGE"),
            "\n",
            " ",
            "GitHub: ",
            systemProperties.getString("GITHUB_PAGE"),
            "\n");
    JOptionPane.showMessageDialog(
        null,
        str,
        systemProperties.getString("VERSION_DIALOG_TITLE"),
        JOptionPane.INFORMATION_MESSAGE,
        icon);
  }

  @Override
  public void showOpenJTalk(MenuEvent e) {
    try {
      URI uri = new URI("http://open-jtalk.sp.nitech.ac.jp/");
      Desktop desktop = Desktop.getDesktop();
      desktop.browse(uri);
    } catch (URISyntaxException e1) {
      e1.printStackTrace();
    } catch (IOException e1) {
      e1.printStackTrace();
    }
  }

  @Override
  public void changeConfigAutoSamplePlayback(MenuEvent e) {
    user.getProperties().invert("AUTO_SAMPLE_PLAYBACK");
  }

  @Override
  public void changeConfigAutoVoicePlayback(MenuEvent e) {
    user.getProperties().invert("AUTO_VOICE_PLAYBACK");
  }

  @Override
  public void changeConfigMaxAmplitudeRejection(MenuEvent e) {
    user.getProperties().invert("MAX_AMPLITUDE_REJECTION");
  }

  @Override
  public void changeConfigMinAmplitudeRejection(MenuEvent e) {
    user.getProperties().invert("MIN_AMPLITUDE_REJECTION");
  }

  @Override
  public void changeConfigVadLevel(MenuEvent e) {
    String vadLevel = e.getParam();
    user.getProperties().setProperty("VAD_LEVEL", vadLevel);
    audio.setSilenceLevel(systemProperties.getDouble("VAD_LEVEL_" + vadLevel));
  }

  @Override
  public void changeConfigPromptRubyType(MenuEvent e) {
    String rubyType = e.getParam();
    promptDraw.setRubyType(rubyType);
    user.getProperties().setProperty("PROMPT_RUBY_TYPE", promptDraw.getRubyType());
  }

  public void changeConfigPromptRubyType(String rubyType) {
    promptDraw.setRubyType(rubyType);
    user.getProperties().setProperty("PROMPT_RUBY_TYPE", promptDraw.getRubyType());
    menu.setSelectedPromptRubyType(promptDraw.getRubyType());
  }

  @Override
  public void changeConfigPromptFontType(MenuEvent e) {
    String fontType = e.getParam();
    promptDraw.setFontType(fontType);
    user.getProperties().setProperty("PROMPT_FONT_TYPE", promptDraw.getFontType());
  }

  public void changeConfigPromptFontType(String fontType) {
    promptDraw.setFontType(fontType);
    user.getProperties().setProperty("PROMPT_FONT_TYPE", promptDraw.getFontType());
    menu.setSelectedPromptFontType(promptDraw.getFontType());
  }

  @Override
  public void changeConfigPromptUpperBoundFontSize(MenuEvent e) {
    int fontSize = e.getParamInteger();
    promptDraw.setUpperBoundFontSize(fontSize);
    user.getProperties()
        .setProperty("PROMPT_UPPER_BOUND_FONT_SIZE", promptDraw.getUpperBoundFontSize());
  }

  public void changeConfigPromptUpperBoundFontSize(int fontSize) {
    promptDraw.setUpperBoundFontSize(fontSize);
    user.getProperties()
        .setProperty("PROMPT_UPPER_BOUND_FONT_SIZE", promptDraw.getUpperBoundFontSize());
  }

  @Override
  public void changeConfigPromptLowerBoundFontSize(MenuEvent e) {
    int fontSize = e.getParamInteger();
    promptDraw.setLowerBoundFontSize(fontSize);
    user.getProperties()
        .setProperty("PROMPT_LOWER_BOUND_FONT_SIZE", promptDraw.getLowerBoundFontSize());
  }

  @Override
  public void changeConfigPromptLineBreakAtPP(MenuEvent e) {
    user.getProperties().invert("PROMPT_LINE_BREAK_AT_PP");
    promptDraw.setEnabledLineBreakAtPP(user.getProperties().getBoolean("PROMPT_LINE_BREAK_AT_PP"));
  }

  @Override
  public void changeConfigGuidanceFontSize(MenuEvent e) {
    int fontSize = e.getParamInteger();
    guidance.setFontSize(fontSize);
    user.getProperties().setProperty("GUIDANCE_FONT_SIZE", guidance.getFontSize());
  }

  @Override
  public void changeConfigStartupGuidance(MenuEvent e) {
    user.getProperties().invert("STARTUP_GUIDANCE");
  }

  @Override
  public void changeShortcutMouseWheel(MenuEvent e) {
    user.getProperties().invert("SHORTCUT_MOUSE_WHEEL");
  }

  @Override
  public void changeShortcutPlaybackSample(MenuEvent e) {
    shortcut.setSubTitle(systemProperties.getString("SHORTCUT_PLAYBACK_SAMPLE"));
    shortcut.setCommand("PLAYBACK_SAMPLE");
    shortcut.setVisible(true);
  }

  @Override
  public void changeShortcutPlaybackVoice(MenuEvent e) {
    shortcut.setSubTitle(systemProperties.getString("SHORTCUT_PLAYBACK_VOICE"));
    shortcut.setCommand("PLAYBACK_VOICE");
    shortcut.setVisible(true);
  }

  @Override
  public void changeShortcutPlaybackVoiceTake2(MenuEvent e) {
    shortcut.setSubTitle(systemProperties.getString("SHORTCUT_PLAYBACK_VOICE_TAKE2"));
    shortcut.setCommand("PLAYBACK_VOICE_TAKE2");
    shortcut.setVisible(true);
  }

  @Override
  public void changeShortcutRecord(MenuEvent e) {
    shortcut.setSubTitle(systemProperties.getString("SHORTCUT_RECORD"));
    shortcut.setCommand("RECORD");
    shortcut.setVisible(true);
  }

  @Override
  public void goPrevPrompt(ButtonEvent e) {
    if (recSet.setPosition(recSet.getPosition() - 1)) {
      go(true);
    }
  }

  @Override
  public void goPrevGuidanceStep(ButtonEvent e) {
    guidance.goPrev();
    checkButtons();
    if (guidance.isRecordingTestStep()) {
      recSet.setPosition(0);
      go(false);
    } else if (guidance.isStepFromEnd(2)) {
      buttons.setMainText(ButtonType.GO_NEXT, "A");
    }
  }

  @Override
  public void goNextPrompt(ButtonEvent e) {
    if (recSet.setPosition(recSet.getPosition() + 1)) {
      go(true);
    }
  }

  @Override
  public void goNextGuidanceStep(ButtonEvent e) {
    guidance.goNext();
    checkButtons();
    if (guidance.isEnvironmentTestStep()) {
      recSet.setPosition(1);
      go(false);
    } else if (guidance.isStepFromEnd(1)) {
      buttons.setMainText(ButtonType.GO_NEXT, "B");
    } else if (guidance.isEndStep()) {
      startGuidance();
    }
  }

  @Override
  public void playbackSample(ButtonEvent e) {
    playbackSample();
  }

  @Override
  public void playbackVoice(ButtonEvent e) {
    playbackVoice(false);
  }

  @Override
  public void playbackVoiceTake2(ButtonEvent e) {
    playbackVoice(true);
  }

  @Override
  public void record(ButtonEvent e) {
    record();
  }

  @Override
  public void overwriteSave(ButtonEvent e) {
    recSet.overwrite();
    recSet.dumpLog();
    changeMode(AppStates.ASKING, AppStates.READY);
    guidance.setText(systemProperties.getString("INSTRUCTION_TEXT_ASKING_YES"), 1, false);
    buttons.setShortcutText(ButtonType.RECORD, shortcut.getReadableChars("RECORD"));
  }

  @Override
  public void cancelSave(ButtonEvent e) {
    recSet.deleteTmpFile();
    changeMode(AppStates.ASKING, AppStates.READY);
    guidance.setText(systemProperties.getString("INSTRUCTION_TEXT_ASKING_NO"), 1, false);
    buttons.setShortcutText(ButtonType.RECORD, shortcut.getReadableChars("RECORD"));
  }
}
