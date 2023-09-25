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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Objects;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

public class PromptIdList {
  //
  private final PropertiesIO properties;

  //
  private final JPanel parent;

  //
  private final RecordSet recSet;

  //
  private final JComboBox<String> comboBox;

  //
  private DefaultComboBoxModel<String> model;

  //
  private int selectedIndex;

  public PromptIdList(JPanel panel, String propertiesFileName, RecordSet recSet) {
    Objects.requireNonNull(panel);
    Objects.requireNonNull(propertiesFileName);
    Objects.requireNonNull(recSet);

    this.recSet = recSet;

    properties = new PropertiesIO(propertiesFileName);
    try {
      properties.load();
    } catch (IOException e) {
      e.printStackTrace();
    }

    parent = panel;
    selectedIndex = -1;

    comboBox = new JComboBox<String>();
    comboBox.setMaximumRowCount(properties.getInteger("MAX_ROW_COUNT"));
    comboBox.setFont(
        new Font(
            properties.getString("FONT_TYPE"), Font.PLAIN, properties.getInteger("FONT_SIZE")));
    comboBox.setFocusable(false);
    comboBox.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            selectedIndex = comboBox.getSelectedIndex();
            updateColor(selectedIndex);
          }
        });

    PromptIDListCellRenderer renderer = new PromptIDListCellRenderer();
    comboBox.setRenderer(renderer);

    JPanel p = new JPanel();
    p.add(comboBox);
    p.setBackground(Color.WHITE);
    panel.add(p);

    reset();
  }

  public void selectItemAt(int index) {
    if (comboBox.getSelectedIndex() == index) {
      return;
    }
    updateColor(index);
    comboBox.setSelectedIndex(index);
    comboBox.repaint();
    parent.repaint();
  }

  public int getIndexOfSelectedItem() {
    int index = selectedIndex;
    selectedIndex = -1;
    return index;
  }

  public void update(boolean success) {
    model.insertElementAt(formItemName(recSet.getPosition(), success), recSet.getPosition());
    model.removeElementAt(comboBox.getSelectedIndex());
    updateColor(recSet.getPosition());
    comboBox.repaint();
    selectedIndex = -1;
  }

  public void reset() {
    String[] names = new String[recSet.getNumPrompts()];
    for (int i = 0; i < names.length; i++) {
      names[i] = formItemName(i, false);
    }
    model = new DefaultComboBoxModel<String>(names);
    comboBox.setModel(model);
    updateColor(0);
    comboBox.repaint();
    parent.repaint();
    selectedIndex = -1;
  }

  public void setEnabled(boolean enabled) {
    comboBox.setEnabled(enabled);
  }

  public boolean isSelected() {
    return selectedIndex == -1 ? false : true;
  }

  private String formItemName(int index, boolean success) {
    RecordInfo log = recSet.getRecordInfo(index);
    String symbol =
        (log != null && log.isRecorded() || success)
            ? properties.getString("SYMBOL_RECORDED")
            : properties.getString("SYMBOL_NOT_RECORDED");

    StringBuffer sb = new StringBuffer();
    sb.append(symbol);
    sb.append(" ");
    sb.append(recSet.getPromptID(index));

    return sb.toString();
  }

  private void updateColor(int index) {
    RecordInfo log = recSet.getRecordInfo(index);
    if (log != null && log.isRecorded()) {
      comboBox.setBackground(new Color(215, 255, 215));
    } else {
      comboBox.setBackground(new Color(245, 245, 245));
    }
  }

  private class PromptIDListCellRenderer extends JLabel implements ListCellRenderer<Object> {
    private static final long serialVersionUID = 1L;

    public PromptIDListCellRenderer() {
      setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(
        JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      if (value == null) {
        return this;
      }

      String text = value.toString();
      setText(text);

      if (isSelected) {
        setForeground(Color.WHITE);
        setBackground(new Color(79, 127, 172));
      } else {
        setForeground(Color.BLACK);
        RecordInfo log = recSet.getRecordInfo(index);
        if (log != null && log.isRecorded()) {
          setBackground(new Color(215, 255, 215));
        } else {
          setBackground(new Color(245, 245, 245));
        }
      }

      return this;
    }
  }
}
