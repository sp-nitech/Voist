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
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Objects;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class LicenseDialog extends JDialog {
  private static final long serialVersionUID = 1L;

  private JCheckBox checkBox;
  private boolean agreed;

  public LicenseDialog(String propertiesFileName) {
    Objects.requireNonNull(propertiesFileName);

    PropertiesIO properties = new PropertiesIO(propertiesFileName);
    try {
      properties.load();
    } catch (IOException e) {
      e.printStackTrace();
    }

    Font font =
        new Font(properties.getString("FONT_TYPE"), Font.PLAIN, properties.getInteger("FONT_SIZE"));

    JTextArea licenseArea = new JTextArea();
    licenseArea.setLineWrap(true);
    licenseArea.setWrapStyleWord(true);
    licenseArea.setEditable(false);
    licenseArea.setMargin(new Insets(5, 5, 5, 5));
    licenseArea.setFont(font);

    JScrollPane scrollPane =
        new JScrollPane(
            licenseArea,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setMaximumSize(
        new Dimension(properties.getInteger("AREA_WIDTH"), properties.getInteger("AREA_HEIGHT")));
    scrollPane.setAlignmentX(CENTER_ALIGNMENT);

    Calendar c = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日");
    licenseArea.setText(properties.getString("LICENSE_TEXT") + "\n\n" + sdf.format(c.getTime()));

    final JButton cancelButton = new JButton();
    cancelButton.setFont(font);
    cancelButton.setText(properties.getString("CANCEL_BUTTON_TEXT"));
    cancelButton.setEnabled(true);
    cancelButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            setVisible(false);
          }
        });

    final JButton nextButton = new JButton();
    nextButton.setFont(font);
    nextButton.setText(properties.getString("NEXT_BUTTON_TEXT"));
    nextButton.setEnabled(false);
    nextButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            setVisible(false);
            agreed = true;
          }
        });

    checkBox = new JCheckBox(properties.getString("AGREEMENT_TEXT"));
    checkBox.setFont(font);
    checkBox.setAlignmentX(CENTER_ALIGNMENT);
    checkBox.addChangeListener(
        new ChangeListener() {
          @Override
          public void stateChanged(ChangeEvent e) {
            JCheckBox cb = (JCheckBox) e.getSource();
            nextButton.setEnabled(cb.isSelected());
          }
        });

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
    buttonPanel.add(cancelButton);
    buttonPanel.add(Box.createRigidArea(new Dimension(200, 1)));
    buttonPanel.add(nextButton);

    setAlwaysOnTop(true);
    setResizable(false);
    setSize(properties.getInteger("FRAME_WIDTH"), properties.getInteger("FRAME_HEIGHT"));
    setTitle(properties.getString("FRAME_TITLE"));
    setLocationRelativeTo(null);
    setModal(true);

    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(new EmptyBorder(20, 0, 20, 0));
    panel.add(scrollPane);
    panel.add(Box.createRigidArea(new Dimension(15, 15)));
    panel.add(checkBox);
    panel.add(Box.createRigidArea(new Dimension(15, 15)));
    panel.add(buttonPanel);

    Container contentPane = getContentPane();
    contentPane.add(panel, BorderLayout.CENTER);
  }

  @Override
  public void setVisible(boolean visible) {
    if (visible) {
      checkBox.setSelected(false);
      agreed = false;
    }
    super.setVisible(visible);
  }

  public boolean isAgreed() {
    return agreed;
  }
}
