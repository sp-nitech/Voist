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
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class UserInfoDialog extends JDialog {
  private static final long serialVersionUID = 1L;

  private JCheckBox checkBox;
  private boolean isEntered;

  private JTextField txtUserName;
  private JTextField txtVoiceName;
  private JTextField txtVoiceKanaName;
  private JButton btnNext;

  private String userName;
  private String voiceName;
  private String voiceKanaName;
  private String emailAddress;
  private String location;
  private String dialect;
  private String occupation;
  private String sex;
  private String ageGroup;
  private String smoking;
  private String recordingEnvironment;
  private String recordingEquipment;
  private String note;

  private boolean buildsVoice;

  private PropertiesIO properties;

  public UserInfoDialog(String propertiesFileName) {
    Objects.requireNonNull(propertiesFileName);

    properties = new PropertiesIO(propertiesFileName);
    try {
      properties.load();
    } catch (IOException e) {
      e.printStackTrace();
    }

    buildsVoice = true;

    Dimension dimLabel = new Dimension(280, 30);
    Dimension dimTextField = new Dimension(200, 30);
    Dimension dimComboBox = new Dimension(200, 29);

    Font font =
        new Font(properties.getString("FONT_TYPE"), Font.PLAIN, properties.getInteger("FONT_SIZE"));

    JLabel lblUserName = new JLabel(properties.getString("LABEL_USER_NAME") + "    ");
    lblUserName.setMaximumSize(dimLabel);
    lblUserName.setPreferredSize(dimLabel);
    lblUserName.setFont(font);
    lblUserName.setHorizontalAlignment(JLabel.RIGHT);

    txtUserName = new JTextField();
    txtUserName.setMaximumSize(dimTextField);
    txtUserName.setPreferredSize(dimTextField);
    txtUserName.setFont(font);
    txtUserName.setHorizontalAlignment(JTextField.CENTER);
    txtUserName.setDocument(new InputDocument(16, null, "[\"\']"));
    txtUserName
        .getDocument()
        .addDocumentListener(
            new DocumentListener() {
              @Override
              public void insertUpdate(DocumentEvent e) {
                btnNext.setEnabled(IsFilledIn());
              }

              @Override
              public void removeUpdate(DocumentEvent e) {
                btnNext.setEnabled(IsFilledIn());
              }

              @Override
              public void changedUpdate(DocumentEvent e) {}
            });

    JLabel lblVoiceName = new JLabel(properties.getString("LABEL_VOICE_NAME") + "    ");
    lblVoiceName.setMaximumSize(dimLabel);
    lblVoiceName.setPreferredSize(dimLabel);
    lblVoiceName.setFont(font);
    lblVoiceName.setHorizontalAlignment(JLabel.RIGHT);

    txtVoiceName = new JTextField();
    txtVoiceName.setMaximumSize(dimTextField);
    txtVoiceName.setPreferredSize(dimTextField);
    txtVoiceName.setFont(font);
    txtVoiceName.setHorizontalAlignment(JTextField.CENTER);
    txtVoiceName.setDocument(new InputDocument(16, null, "[\"\']"));
    txtVoiceName
        .getDocument()
        .addDocumentListener(
            new DocumentListener() {
              @Override
              public void insertUpdate(DocumentEvent e) {
                btnNext.setEnabled(IsFilledIn());
              }

              @Override
              public void removeUpdate(DocumentEvent e) {
                btnNext.setEnabled(IsFilledIn());
              }

              @Override
              public void changedUpdate(DocumentEvent e) {}
            });

    JLabel lblVoiceKanaName = new JLabel(properties.getString("LABEL_VOICE_KANA_NAME") + "    ");
    lblVoiceKanaName.setMaximumSize(dimLabel);
    lblVoiceKanaName.setPreferredSize(dimLabel);
    lblVoiceKanaName.setFont(font);
    lblVoiceKanaName.setHorizontalAlignment(JLabel.RIGHT);

    txtVoiceKanaName = new JTextField();
    txtVoiceKanaName.setMaximumSize(dimTextField);
    txtVoiceKanaName.setPreferredSize(dimTextField);
    txtVoiceKanaName.setFont(font);
    txtVoiceKanaName.setHorizontalAlignment(JTextField.CENTER);
    txtVoiceKanaName.setDocument(new InputDocument(16, "[\\p{InHiragana}|例：]", null));
    txtVoiceKanaName
        .getDocument()
        .addDocumentListener(
            new DocumentListener() {
              @Override
              public void insertUpdate(DocumentEvent e) {
                btnNext.setEnabled(IsFilledIn());
              }

              @Override
              public void removeUpdate(DocumentEvent e) {
                btnNext.setEnabled(IsFilledIn());
              }

              @Override
              public void changedUpdate(DocumentEvent e) {}
            });

    JLabel lblEmailAddress = new JLabel(properties.getString("LABEL_EMAIL_ADDRESS") + "    ");
    lblEmailAddress.setMaximumSize(dimLabel);
    lblEmailAddress.setPreferredSize(dimLabel);
    lblEmailAddress.setFont(font);
    lblEmailAddress.setHorizontalAlignment(JLabel.RIGHT);

    final JTextField txtEmailAddress = new JTextField();
    txtEmailAddress.setMaximumSize(dimTextField);
    txtEmailAddress.setPreferredSize(dimTextField);
    txtEmailAddress.setFont(font);
    txtEmailAddress.setHorizontalAlignment(JTextField.CENTER);
    txtEmailAddress.setDocument(new InputDocument(64, "\\p{InBasicLatin}", "[\"\']"));

    JPanel pnlNorthWest = new JPanel();
    pnlNorthWest.setLayout(new BoxLayout(pnlNorthWest, BoxLayout.Y_AXIS));
    pnlNorthWest.add(lblUserName);
    pnlNorthWest.add(lblVoiceName);
    pnlNorthWest.add(lblVoiceKanaName);
    pnlNorthWest.add(lblEmailAddress);

    JPanel pnlNorthEast = new JPanel();
    pnlNorthEast.setLayout(new BoxLayout(pnlNorthEast, BoxLayout.Y_AXIS));
    pnlNorthEast.add(txtUserName);
    pnlNorthEast.add(txtVoiceName);
    pnlNorthEast.add(txtVoiceKanaName);
    pnlNorthEast.add(txtEmailAddress);

    JPanel pnlNorth = new JPanel();
    pnlNorth.setLayout(new BoxLayout(pnlNorth, BoxLayout.LINE_AXIS));
    pnlNorth.add(pnlNorthWest);
    pnlNorth.add(pnlNorthEast);

    JLabel lblLocation = new JLabel(properties.getString("LABEL_LOCATION") + "    ");
    lblLocation.setMaximumSize(dimLabel);
    lblLocation.setPreferredSize(dimLabel);
    lblLocation.setFont(font);
    lblLocation.setHorizontalAlignment(JLabel.RIGHT);

    int numLocations = properties.getInteger("NUM_LOCATIONS");
    String[] locationItems = new String[numLocations + 2];
    locationItems[0] = properties.getString("LABEL_ANSWER_NONE");
    for (int i = 1; i <= numLocations; i++) {
      String num = String.format("%02d", i);
      locationItems[i] = properties.getString("LABEL_LOCATION_" + num);
    }
    locationItems[locationItems.length - 1] = properties.getString("LABEL_ANSWER_OTHER");

    final JComboBox<String> cmbLocation = new JComboBox<String>(locationItems);
    cmbLocation.setMaximumSize(dimComboBox);
    cmbLocation.setPreferredSize(dimComboBox);
    cmbLocation.setMaximumRowCount(10);
    cmbLocation.setFont(font);

    JLabel lblDialect = new JLabel(properties.getString("LABEL_DIALECT") + "    ");
    lblDialect.setMaximumSize(dimLabel);
    lblDialect.setPreferredSize(dimLabel);
    lblDialect.setFont(font);
    lblDialect.setHorizontalAlignment(JLabel.RIGHT);

    final JTextField txtDialect = new JTextField();
    txtDialect.setMaximumSize(dimTextField);
    txtDialect.setPreferredSize(dimTextField);
    txtDialect.setFont(font);
    txtDialect.setHorizontalAlignment(JTextField.CENTER);
    txtDialect.setDocument(new InputDocument(16, null, "[\"\']"));

    JLabel lblOccupation = new JLabel(properties.getString("LABEL_OCCUPATION") + "    ");
    lblOccupation.setMaximumSize(dimLabel);
    lblOccupation.setPreferredSize(dimLabel);
    lblOccupation.setFont(font);
    lblOccupation.setHorizontalAlignment(JLabel.RIGHT);

    int numOccupations = properties.getInteger("NUM_OCCUPATIONS");
    String[] occupationItems = new String[numOccupations + 2];
    occupationItems[0] = properties.getString("LABEL_ANSWER_NONE");
    for (int i = 1; i <= numOccupations; i++) {
      String num = String.format("%02d", i);
      occupationItems[i] = properties.getString("LABEL_OCCUPATION_" + num);
    }
    occupationItems[occupationItems.length - 1] = properties.getString("LABEL_ANSWER_OTHER");

    final JComboBox<String> cmbOccupation = new JComboBox<String>(occupationItems);
    cmbOccupation.setMaximumSize(dimComboBox);
    cmbOccupation.setPreferredSize(dimComboBox);
    cmbOccupation.setMaximumRowCount(10);
    cmbOccupation.setFont(font);

    JPanel pnlCenterWest = new JPanel();
    pnlCenterWest.setLayout(new BoxLayout(pnlCenterWest, BoxLayout.Y_AXIS));
    pnlCenterWest.add(lblDialect);
    pnlCenterWest.add(lblLocation);
    pnlCenterWest.add(lblOccupation);

    JPanel pnlCenterEast = new JPanel();
    pnlCenterEast.setLayout(new BoxLayout(pnlCenterEast, BoxLayout.Y_AXIS));
    pnlCenterEast.add(txtDialect);
    pnlCenterEast.add(cmbLocation);
    pnlCenterEast.add(cmbOccupation);

    JPanel pnlCenter = new JPanel();
    pnlCenter.setLayout(new BoxLayout(pnlCenter, BoxLayout.LINE_AXIS));
    pnlCenter.add(pnlCenterWest);
    pnlCenter.add(pnlCenterEast);

    JLabel lblSex = new JLabel(properties.getString("LABEL_SEX") + "    ");
    lblSex.setMaximumSize(dimLabel);
    lblSex.setPreferredSize(dimLabel);
    lblSex.setFont(font);
    lblSex.setHorizontalAlignment(JLabel.RIGHT);

    String[] sexItems =
        new String[] {
          properties.getString("LABEL_ANSWER_NONE"),
          properties.getString("LABEL_SEX_MALE"),
          properties.getString("LABEL_SEX_FEMALE"),
        };
    final JComboBox<String> cmbSex = new JComboBox<String>(sexItems);
    cmbSex.setMaximumSize(dimComboBox);
    cmbSex.setPreferredSize(dimComboBox);
    cmbSex.setFont(font);

    JLabel lblAgeGroup = new JLabel(properties.getString("LABEL_AGE_GROUP") + "    ");
    lblAgeGroup.setMaximumSize(dimLabel);
    lblAgeGroup.setPreferredSize(dimLabel);
    lblAgeGroup.setFont(font);
    lblAgeGroup.setHorizontalAlignment(JLabel.RIGHT);

    String[] ageGroupItems =
        new String[] {
          properties.getString("LABEL_ANSWER_NONE"),
          properties.getString("LABEL_AGE_GROUP_0"),
          properties.getString("LABEL_AGE_GROUP_1"),
          properties.getString("LABEL_AGE_GROUP_2"),
          properties.getString("LABEL_AGE_GROUP_3"),
          properties.getString("LABEL_AGE_GROUP_4"),
          properties.getString("LABEL_AGE_GROUP_5"),
          properties.getString("LABEL_AGE_GROUP_6"),
          properties.getString("LABEL_AGE_GROUP_7"),
          properties.getString("LABEL_AGE_GROUP_8"),
        };
    final JComboBox<String> cmbAgeGroup = new JComboBox<String>(ageGroupItems);
    cmbAgeGroup.setMaximumSize(dimComboBox);
    cmbAgeGroup.setPreferredSize(dimComboBox);
    cmbAgeGroup.setFont(font);

    JLabel lblSmoking = new JLabel(properties.getString("LABEL_SMOKING") + "    ");
    lblSmoking.setMaximumSize(dimLabel);
    lblSmoking.setPreferredSize(dimLabel);
    lblSmoking.setFont(font);
    lblSmoking.setHorizontalAlignment(JLabel.RIGHT);

    String[] smokingItems =
        new String[] {
          properties.getString("LABEL_ANSWER_NONE"),
          properties.getString("LABEL_SMOKING_YES"),
          properties.getString("LABEL_SMOKING_NO"),
        };
    final JComboBox<String> cmbSmoking = new JComboBox<String>(smokingItems);
    cmbSmoking.setMaximumSize(dimComboBox);
    cmbSmoking.setPreferredSize(dimComboBox);
    cmbSmoking.setFont(font);

    JLabel lblRecordingEnvironment =
        new JLabel(properties.getString("LABEL_RECORDING_ENVIRONMENT") + "    ");
    lblRecordingEnvironment.setMaximumSize(dimLabel);
    lblRecordingEnvironment.setPreferredSize(dimLabel);
    lblRecordingEnvironment.setFont(font);
    lblRecordingEnvironment.setHorizontalAlignment(JLabel.RIGHT);

    String[] recordingEnvironmentItems =
        new String[] {
          properties.getString("LABEL_ANSWER_NONE"),
          properties.getString("LABEL_RECORDING_ENVIRONMENT_STUDIO"),
          properties.getString("LABEL_RECORDING_ENVIRONMENT_INDOOR"),
          properties.getString("LABEL_RECORDING_ENVIRONMENT_OUTDOOR"),
          properties.getString("LABEL_ANSWER_OTHER"),
        };
    final JComboBox<String> cmbRecordingEnvironment =
        new JComboBox<String>(recordingEnvironmentItems);
    cmbRecordingEnvironment.setMaximumSize(dimComboBox);
    cmbRecordingEnvironment.setPreferredSize(dimComboBox);
    cmbRecordingEnvironment.setFont(font);

    JLabel lblRecordingEquipment =
        new JLabel(properties.getString("LABEL_RECORDING_EQUIPMENT") + "    ");
    lblRecordingEquipment.setMaximumSize(dimLabel);
    lblRecordingEquipment.setPreferredSize(dimLabel);
    lblRecordingEquipment.setFont(font);
    lblRecordingEquipment.setHorizontalAlignment(JLabel.RIGHT);

    String[] recordingEquipmentItems =
        new String[] {
          properties.getString("LABEL_ANSWER_NONE"),
          properties.getString("LABEL_RECORDING_EQUIPMENT_BUILTIN"),
          properties.getString("LABEL_RECORDING_EQUIPMENT_EXTERNAL"),
          properties.getString("LABEL_RECORDING_EQUIPMENT_HEADSET"),
          properties.getString("LABEL_ANSWER_OTHER"),
        };
    final JComboBox<String> cmbRecordingEquipment = new JComboBox<String>(recordingEquipmentItems);
    cmbRecordingEquipment.setMaximumSize(dimComboBox);
    cmbRecordingEquipment.setPreferredSize(dimComboBox);
    cmbRecordingEquipment.setFont(font);

    JLabel lblNote = new JLabel(properties.getString("LABEL_NOTE") + "    ");
    lblNote.setMaximumSize(dimLabel);
    lblNote.setPreferredSize(dimLabel);
    lblNote.setFont(font);
    lblNote.setHorizontalAlignment(JLabel.RIGHT);

    final JTextField txtNote = new JTextField();
    txtNote.setMaximumSize(dimTextField);
    txtNote.setPreferredSize(dimTextField);
    txtNote.setFont(font);
    txtNote.setHorizontalAlignment(JTextField.CENTER);
    txtNote.setDocument(new InputDocument(64, null, "[\"\']"));

    JPanel pnlSouthWest = new JPanel();
    pnlSouthWest.setLayout(new BoxLayout(pnlSouthWest, BoxLayout.Y_AXIS));
    pnlSouthWest.add(lblSex);
    pnlSouthWest.add(lblAgeGroup);
    pnlSouthWest.add(lblSmoking);
    pnlSouthWest.add(lblRecordingEnvironment);
    pnlSouthWest.add(lblRecordingEquipment);
    pnlSouthWest.add(lblNote);

    JPanel pnlSouthEast = new JPanel();
    pnlSouthEast.setLayout(new BoxLayout(pnlSouthEast, BoxLayout.Y_AXIS));
    pnlSouthEast.add(cmbSex);
    pnlSouthEast.add(cmbAgeGroup);
    pnlSouthEast.add(cmbSmoking);
    pnlSouthEast.add(cmbRecordingEnvironment);
    pnlSouthEast.add(cmbRecordingEquipment);
    pnlSouthEast.add(txtNote);

    JPanel pnlSouth = new JPanel();
    pnlSouth.setLayout(new BoxLayout(pnlSouth, BoxLayout.LINE_AXIS));
    pnlSouth.add(pnlSouthWest);
    pnlSouth.add(pnlSouthEast);

    final JButton btnCancel = new JButton();
    btnCancel.setFont(font);
    btnCancel.setText(properties.getString("BUTTON_CANCEL"));
    btnCancel.setEnabled(true);
    btnCancel.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            setVisible(false);
          }
        });

    btnNext = new JButton();
    btnNext.setFont(font);
    btnNext.setText(properties.getString("BUTTON_NEXT"));
    btnNext.setEnabled(false);
    btnNext.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            setVisible(false);

            userName = txtUserName.getText();
            voiceName = txtVoiceName.getText();
            voiceKanaName = txtVoiceKanaName.getText();
            emailAddress = txtEmailAddress.getText();
            location = (String) cmbLocation.getSelectedItem();
            dialect = txtDialect.getText();
            occupation = (String) cmbOccupation.getSelectedItem();
            sex = (String) cmbSex.getSelectedItem();
            ageGroup = (String) cmbAgeGroup.getSelectedItem();
            smoking = (String) cmbSmoking.getSelectedItem();
            recordingEnvironment = (String) cmbRecordingEnvironment.getSelectedItem();
            recordingEquipment = (String) cmbRecordingEquipment.getSelectedItem();
            note = txtNote.getText();

            isEntered = true;
          }
        });

    JLabel lblNotice = new JLabel(properties.getString("NOTICE"));
    lblNotice.setMaximumSize(dimLabel);
    lblNotice.setPreferredSize(dimLabel);
    lblNotice.setFont(font);
    lblNotice.setHorizontalAlignment(JLabel.CENTER);

    checkBox = new JCheckBox(properties.getString("CHECKBOX_TEXT"));
    checkBox.setFont(font);
    checkBox.setAlignmentX(CENTER_ALIGNMENT);
    checkBox.addChangeListener(
        new ChangeListener() {
          @Override
          public void stateChanged(ChangeEvent e) {
            btnNext.setEnabled(IsFilledIn());
          }
        });

    JPanel pnlButton = new JPanel();
    pnlButton.setLayout(new BoxLayout(pnlButton, BoxLayout.X_AXIS));
    pnlButton.add(Box.createRigidArea(new Dimension(50, 1)));
    pnlButton.add(btnCancel);
    pnlButton.add(Box.createRigidArea(new Dimension(240, 1)));
    pnlButton.add(btnNext);

    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(new EmptyBorder(25, 0, 20, 0));
    panel.add(lblNotice);
    panel.add(Box.createRigidArea(new Dimension(5, 5)));
    panel.add(pnlNorth);
    panel.add(Box.createRigidArea(new Dimension(10, 10)));
    panel.add(pnlCenter);
    panel.add(Box.createRigidArea(new Dimension(10, 10)));
    panel.add(pnlSouth);
    panel.add(Box.createRigidArea(new Dimension(15, 15)));
    panel.add(checkBox);
    panel.add(Box.createRigidArea(new Dimension(15, 15)));
    panel.add(pnlButton);

    Container contentPane = getContentPane();
    contentPane.add(panel, BorderLayout.CENTER);

    setAlwaysOnTop(true);
    setResizable(false);
    setSize(properties.getInteger("FRAME_WIDTH"), properties.getInteger("FRAME_HEIGHT"));
    setTitle(properties.getString("FRAME_TITLE"));
    setLocationRelativeTo(null);
    setModal(true);
  }

  @Override
  public void setVisible(boolean visible) {
    if (visible) {
      checkBox.setSelected(false);
      isEntered = false;
    }
    super.setVisible(visible);
  }

  public boolean isEntered() {
    return isEntered;
  }

  public String getUserName() {
    return userName;
  }

  public String getVoiceName() {
    return voiceName;
  }

  public String getVoiceKanaName() {
    return voiceKanaName;
  }

  public String getEmailAddress() {
    return emailAddress;
  }

  public String getLocalLocation() {
    return location;
  }

  public String getDialect() {
    return dialect;
  }

  public String getOccupation() {
    return occupation;
  }

  public String getSex() {
    return sex;
  }

  public String getSexInEnglish() {
    if (sex.equals(properties.getString("LABEL_SEX_MALE"))) {
      return "male";
    } else if (sex.equals(properties.getString("LABEL_SEX_FEMALE"))) {
      return "female";
    } else {
      return "undefined";
    }
  }

  public String getAgeGroup() {
    return ageGroup;
  }

  public String getSmoking() {
    return smoking;
  }

  public String getRecordingEnvironment() {
    return recordingEnvironment;
  }

  public String getRecordingEquipment() {
    return recordingEquipment;
  }

  public String getVoiceBuildingFlag() {
    return buildsVoice ? "yes" : "no";
  }

  public String getNote() {
    return note;
  }

  public String getDefaultLocale() {
    Locale loc = Locale.getDefault();
    return loc.toString();
  }

  private boolean IsFilledIn() {
    return checkBox.isSelected()
        && (!txtUserName.getText().equals("")
            && !txtVoiceName.getText().equals("")
            && !txtVoiceKanaName.getText().equals(""));
  }
}
