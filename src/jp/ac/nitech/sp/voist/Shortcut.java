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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

public class Shortcut extends JFrame {
  private static final long serialVersionUID = 1L;

  // External properties
  private final PropertiesIO properties;

  // User-defined command -> Key-code
  private final HashMap<String, Integer> registeredShortcuts;

  // Key-code -> Readable character(s)
  private final HashMap<Integer, String> keyCodeToChars;

  // Registering command
  private String command;

  // Key-code corresponding 'command'
  private int keyCode;

  public Shortcut(String propertiesFileName, ImageIcon icon) {
    // Get external properties.
    Objects.requireNonNull(propertiesFileName);
    properties = new PropertiesIO(propertiesFileName);
    try {
      properties.load();
    } catch (IOException e) {
      e.printStackTrace();
    }

    // Initialize.
    command = null;
    keyCode = -1;

    // Create a hashmap to memory registered shortcuts.
    registeredShortcuts = new HashMap<String, Integer>(32);

    // Create the table which maps a key-code to readable character(s).
    keyCodeToChars = new HashMap<Integer, String>(64);
    for (int i = KeyEvent.VK_A; i <= KeyEvent.VK_Z; i++) {
      keyCodeToChars.put(i, String.valueOf((char) i));
    }
    for (int i = KeyEvent.VK_0; i <= KeyEvent.VK_9; i++) {
      keyCodeToChars.put(i, String.valueOf((char) i));
    }
    for (int i = KeyEvent.VK_F1; i <= KeyEvent.VK_F12; i++) {
      if (i != KeyEvent.VK_F10) {
        keyCodeToChars.put(i, "F" + (i - KeyEvent.VK_F1 + 1));
      }
    }
    keyCodeToChars.put(KeyEvent.VK_UP, "↑");
    keyCodeToChars.put(KeyEvent.VK_DOWN, "↓");
    keyCodeToChars.put(KeyEvent.VK_LEFT, "←");
    keyCodeToChars.put(KeyEvent.VK_RIGHT, "→");
    keyCodeToChars.put(KeyEvent.VK_PERIOD, ".");
    keyCodeToChars.put(KeyEvent.VK_COMMA, ",");
    keyCodeToChars.put(KeyEvent.VK_COLON, ":");
    keyCodeToChars.put(KeyEvent.VK_SEMICOLON, ";");
    keyCodeToChars.put(KeyEvent.VK_AT, "@");
    keyCodeToChars.put(KeyEvent.VK_BACK_SPACE, "BS");
    keyCodeToChars.put(KeyEvent.VK_DELETE, "Delete");
    keyCodeToChars.put(KeyEvent.VK_ENTER, "Enter");
    keyCodeToChars.put(KeyEvent.VK_ESCAPE, "ESC");
    keyCodeToChars.put(KeyEvent.VK_SPACE, "Space");

    // Create a text field to show a shortcut entered by user.
    final JTextField textField = new JTextField();
    textField.setEditable(false);
    textField.setHorizontalAlignment(JTextField.CENTER);
    textField.setMaximumSize(new Dimension(150, 30));
    textField.addKeyListener(
        new KeyAdapter() {
          @Override
          public void keyPressed(KeyEvent e) {
            String s = keyCodeToChars.get(e.getKeyCode());
            if (s == null) {
              textField.setText(properties.getString("TEXT_CANNOT_BE_USED"));
              keyCode = -1;
            } else {
              textField.setText(s);
              keyCode = e.getKeyCode();
            }
          }
        });

    Font font =
        new Font(properties.getString("FONT_TYPE"), Font.PLAIN, properties.getInteger("FONT_SIZE"));
    UIManager.put("OptionPane.messageFont", font);

    // Create a button.
    final JButton button = new JButton(properties.getString("BUTTON_TEXT_UPDATE"));
    button.setAlignmentX(CENTER_ALIGNMENT);
    button.setFont(font);
    button.setSize(new Dimension(50, 20));
    button.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            // Invalid key
            if (keyCode == -1) {
              command = null;
              return;
            }

            // Same key
            if (keyCode == getKeyCode(command)) {
              setVisible(false);
              command = null;
              return;
            }

            if (registerShortcut(command, keyCode)) {
              // Success
              setVisible(false);
            } else {
              // Failed
              command = null;
              setVisible(false);
              VoistUtils.warn(
                  "Cannot register the shortcut: " + command + " <-> " + keyCode,
                  "actionPerformed");
              JOptionPane.showMessageDialog(
                  null,
                  properties.getString("WARNING_MESSAGE_SAME_SHORTCUT"),
                  properties.getString("WARNING_MESSAGE_TITLE"),
                  JOptionPane.WARNING_MESSAGE);
            }
          }
        });
    button.addKeyListener(
        new KeyAdapter() {
          @Override
          public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
              button.doClick();
            }
          }
        });

    // Set frame properties.
    setAlwaysOnTop(true);
    setLocation(
        properties.getInteger("FRAME_LOCATION_X"), properties.getInteger("FRAME_LOCATION_Y"));
    setResizable(false);
    setSize(properties.getInteger("FRAME_WIDTH"), properties.getInteger("FRAME_HEIGHT"));
    setTitle("");
    setIconImage(icon.getImage());
    addComponentListener(
        new ComponentAdapter() {
          @Override
          public void componentShown(ComponentEvent e) {
            textField.requestFocusInWindow();
            textField.setText(getReadableChars(command));
          }
        });

    // Put components.
    JPanel panel = new JPanel();
    Dimension dim = new Dimension(6, 6);
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.add(Box.createRigidArea(dim));
    panel.add(textField);
    panel.add(Box.createRigidArea(dim));
    panel.add(button);
    panel.add(Box.createRigidArea(dim));

    Container contentPane = getContentPane();
    contentPane.add(panel, BorderLayout.CENTER);
  }

  public void setCommand(String command) {
    Objects.requireNonNull(command);
    this.command = command;
    if (registeredShortcuts.containsKey(command)) {
      keyCode = registeredShortcuts.get(command);
    }
  }

  public void setSubTitle(String subTitle) {
    setTitle(properties.get("FRAME_TITLE") + subTitle);
  }

  public String getCommand() {
    return command;
  }

  public int getKeyCode() {
    return keyCode;
  }

  public int getKeyCode(String command) {
    Integer keyCode = registeredShortcuts.get(command);
    return keyCode == null ? -1 : keyCode;
  }

  public String getReadableChars(String command) {
    return keyCodeToChars.get(registeredShortcuts.get(command));
  }

  public boolean registerShortcut(String command, int keyCode) {
    if (command == null
        || command.equals("")
        || !keyCodeToChars.containsKey(keyCode)
        || registeredShortcuts.containsValue(keyCode)) {
      return false;
    }

    registeredShortcuts.put(command, keyCode);
    return true;
  }

  public boolean registerShortcut() {
    return registerShortcut(command, keyCode);
  }

  public boolean removeShortcut(String command) {
    return registeredShortcuts.remove(command) != null;
  }
}
