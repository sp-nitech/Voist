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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

public class UserSettings extends JDialog {
  private static final long serialVersionUID = 1L;

  private final PropertiesIO systemProperties;
  private final PropertiesIO userProperties;
  private final String userPropertiesDirName;
  private final JTextField nameField;

  private boolean hasChanged;
  private String name;

  public UserSettings(String propertiesFileName, String propertiesDirName, ImageIcon icon) {
    Objects.requireNonNull(propertiesFileName);
    Objects.requireNonNull(propertiesDirName);

    // Load system properties.
    systemProperties = new PropertiesIO(propertiesFileName);
    try {
      systemProperties.load();
    } catch (IOException e) {
      e.printStackTrace();
    }

    // Set user properties.
    userProperties = new PropertiesIO();
    userPropertiesDirName = propertiesDirName;
    setDefaultProperties();

    // Initialize.
    hasChanged = false;
    name = "";

    // Create a text filed for entering user name.
    nameField = new JTextField();
    nameField.setAlignmentX(CENTER_ALIGNMENT);
    nameField.setDocument(
        new InputDocument(
            systemProperties.getInteger("MAX_INPUT_CHARACTERS"),
            "[a-zA-Z0-9_]",
            null)); // [\\s\\^~!\"#$%&'\\(\\){}\\[\\]<>|@`=+*:;.,?/\\\\]
    nameField.setHorizontalAlignment(JTextField.CENTER);
    nameField.setMaximumSize(new Dimension(150, 30));
    nameField.setText(name);

    Font font =
        new Font(
            systemProperties.getString("FONT_TYPE"),
            Font.PLAIN,
            systemProperties.getInteger("FONT_SIZE"));
    UIManager.put("OptionPane.messageFont", font);

    // Crate a button.
    JButton button = new JButton(systemProperties.getString("BUTTON_TEXT_UPDATE"));
    button.setAlignmentX(CENTER_ALIGNMENT);
    button.setFont(font);
    button.setPreferredSize(new Dimension(50, 20));
    button.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            String newName = nameField.getText();
            setVisible(false);

            if (!newName.equals("") && !newName.equals(name)) {
              storeProperties();
              setName(newName);

              JOptionPane.showMessageDialog(
                  null,
                  systemProperties.getString("CHANGED_MESSAGE"),
                  systemProperties.getString("FRAME_TITLE"),
                  JOptionPane.INFORMATION_MESSAGE);
            } else {
              // Restore the text field.
              nameField.setText(name);
            }

            nameField.requestFocus();
          }
        });
    getRootPane().setDefaultButton(button);

    // Set frame properties.
    setAlwaysOnTop(true);
    setResizable(false);
    setSize(
        systemProperties.getInteger("FRAME_WIDTH"), systemProperties.getInteger("FRAME_HEIGHT"));
    setTitle(systemProperties.getString("FRAME_TITLE"));
    setIconImage(icon.getImage());
    setLocationRelativeTo(null);
    setModal(true);

    // Put components.
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
    panel.setBorder(new EmptyBorder(10, 10, 10, 10));
    panel.add(nameField);
    panel.add(Box.createRigidArea(new Dimension(6, 6)));
    panel.add(button);

    Container contentPane = getContentPane();
    contentPane.add(panel, BorderLayout.CENTER);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    if (name == null || this.name.equals(name) || hasChanged) {
      return;
    }

    String dirName = FileUtils.createPath(userPropertiesDirName, name);
    File dir = new File(dirName);
    if (!dir.exists()) {
      dir.mkdirs();
    }

    setDefaultProperties();
    userProperties.setIOFile(
        FileUtils.createPath(dirName, systemProperties.getString("USER_PROPERTIES")));
    try {
      userProperties.load();
    } catch (IOException e) {
      e.printStackTrace();
    }

    this.name = name;
    nameField.setText(name);

    hasChanged = true;
  }

  public boolean hasChanged() {
    boolean b = hasChanged;
    hasChanged = false;
    return b;
  }

  public void storeProperties() {
    try {
      userProperties.store();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public PropertiesIO getProperties() {
    return userProperties;
  }

  private void setDefaultProperties() {
    userProperties.setProperty("AUTO_SAMPLE_PLAYBACK", "false");
    userProperties.setProperty("AUTO_VOICE_PLAYBACK", "false");
    userProperties.setProperty("BEEP_TYPE", "A");
    userProperties.setProperty("GUIDANCE_FONT_SIZE", "22");
    userProperties.setProperty("MAX_AMPLITUDE_REJECTION", "false");
    userProperties.setProperty("MIN_AMPLITUDE_REJECTION", "false");
    userProperties.setProperty("NUM_UPLOADED_SENTENCES", "0");
    userProperties.setProperty("SHORTCUT_MOUSE_WHEEL", "true");
    userProperties.setProperty("SHORTCUT_PLAYBACK_SAMPLE", "83");
    userProperties.setProperty("SHORTCUT_PLAYBACK_VOICE", "82");
    userProperties.setProperty("SHORTCUT_PLAYBACK_VOICE_TAKE2", "69");
    userProperties.setProperty("SHORTCUT_RECORD", "10");
    userProperties.setProperty("STARTUP_GUIDANCE", "false");
    userProperties.setProperty("PROMPT_FONT_TYPE", "ＭＳ ゴシック");
    userProperties.setProperty("PROMPT_LINE_BREAK_AT_PP", "true");
    userProperties.setProperty("PROMPT_LOWER_BOUND_FONT_SIZE", "62");
    userProperties.setProperty("PROMPT_RUBY_TYPE", "A");
    userProperties.setProperty("PROMPT_UPPER_BOUND_FONT_SIZE", "46");
    userProperties.setProperty("VAD_LEVEL", "C");
  }
}
