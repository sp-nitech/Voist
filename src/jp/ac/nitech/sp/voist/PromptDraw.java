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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.IOException;
import java.util.Objects;
import javax.swing.JPanel;

public class PromptDraw extends JPanel {
  private static final long serialVersionUID = 1L;

  //
  private final PropertiesIO properties;

  //
  private final int minFontSize;

  //
  private final int maxFontSize;

  //
  private final int gapFontSize;

  //
  private final int tmpLine;

  //
  private final int[] accLength;

  //
  private String fontType;

  //
  private String rubyType;

  //
  private int upperBoundFontSize;

  //
  private int lowerBoundFontSize;

  //
  private int upperBoundLineBreakScore;

  //
  private int lowerBoundLineBreakScore;

  //
  private Prompt prompt;

  //
  private int fontSize;

  //
  private int lineBreakScore;

  //
  private int nowLine;

  //
  private boolean canDraw;

  public PromptDraw(
      JPanel panel, String propertiesFileName, int maxFontSize, int minFontSize, int gapFontSize) {
    Objects.requireNonNull(panel);
    Objects.requireNonNull(propertiesFileName);

    properties = new PropertiesIO(propertiesFileName);
    try {
      properties.load();
    } catch (IOException e) {
      e.printStackTrace();
    }

    setAlignmentX(Component.CENTER_ALIGNMENT);
    setOpaque(false);

    // Set user-independent variables.
    this.maxFontSize = maxFontSize;
    this.minFontSize = minFontSize;
    this.gapFontSize = gapFontSize;
    tmpLine = properties.getInteger("MAX_LINE", 1);
    accLength = new int[tmpLine + 1];

    // Set user-dependent variables.
    fontType = null;
    rubyType = null;
    upperBoundFontSize = 0;
    lowerBoundFontSize = 0;
    upperBoundLineBreakScore = 80;
    lowerBoundLineBreakScore = 80;

    prompt = null;
    fontSize = 0;
    lineBreakScore = 0;
    nowLine = 0;
    canDraw = false;

    panel.add(this);
  }

  public boolean setFontType(String fontType) {
    if (fontType != null) {
      if (this.fontType == null || !rubyType.equals(this.fontType)) {
        this.fontType = fontType;
        repaint();
      }
      return true;
    }
    return false;
  }

  public boolean setRubyType(String rubyType) {
    if (rubyType != null && (rubyType.equals("A") || rubyType.equals("B"))) {
      if (this.rubyType == null || !rubyType.equals(this.rubyType)) {
        this.rubyType = rubyType;
        repaint();
      }
      return true;
    }

    return false;
  }

  public void setUpperBoundFontSize(int fontSize) {
    if (upperBoundFontSize == fontSize) {
      return;
    }
    int size = fontSize;
    if (fontSize < minFontSize) {
      size = minFontSize;
    } else if (fontSize > maxFontSize) {
      size = maxFontSize;
    }
    upperBoundFontSize = size;
    this.fontSize = size;
    repaint();
  }

  public void setLowerBoundFontSize(int fontSize) {
    if (lowerBoundFontSize == fontSize) {
      return;
    }
    int size = fontSize;
    if (fontSize < minFontSize) {
      size = minFontSize;
    } else if (fontSize > maxFontSize) {
      size = maxFontSize;
    }
    lowerBoundFontSize = size;
    repaint();
  }

  public void setPrompt(Prompt prompt) {
    this.prompt = prompt;
    repaint();
  }

  public boolean setUpperBoundLineBreakScore(int score) {
    if (0 <= score && score <= 100) {
      if (upperBoundLineBreakScore != score) {
        upperBoundLineBreakScore = score;
        repaint();
      }
      return true;
    }
    return false;
  }

  public void setEnabledLineBreakAtPP(boolean enabled) {
    int prevScore = lowerBoundLineBreakScore;
    if (enabled && lowerBoundLineBreakScore != Prompt.PP_SCORE) {
      lowerBoundLineBreakScore = Prompt.PP_SCORE;
    } else if (!enabled) {
      lowerBoundLineBreakScore = upperBoundLineBreakScore;
    }
    if (prevScore != lowerBoundLineBreakScore) {
      repaint();
    }
  }

  public void setEnabledDrawing(boolean enabled) {
    if (canDraw != enabled) {
      canDraw = enabled;
      repaint();
    }
  }

  public String getFontType() {
    return fontType;
  }

  public String getRubyType() {
    return rubyType;
  }

  public int getUpperBoundFontSize() {
    return upperBoundFontSize;
  }

  public int getLowerBoundFontSize() {
    return lowerBoundFontSize;
  }

  public Prompt getPrompt() {
    return prompt;
  }

  public boolean getEnabledLineBreakAtPP() {
    return lowerBoundLineBreakScore == Prompt.PP_SCORE;
  }

  public boolean getEnabledDrawing() {
    return canDraw;
  }

  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);

    if (prompt != null && canDraw) {
      int maxWidth = getParent().getWidth() - 2 * properties.getInteger("X");
      int size = prompt.getNumWords();

      // Initialize.
      for (int i = 0; i < accLength.length; i++) {
        accLength[i] = 0;
      }
      nowLine = 0;
      fontSize = upperBoundFontSize;
      lineBreakScore = upperBoundLineBreakScore;
      int write = -1;

      Graphics2D g2 = (Graphics2D) g;
      g2.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

      while (write != 1) {
        int start = 0;
        accLength[tmpLine] = 0;
        for (int i = 0; i < size; i++) {
          // Count word length in this line.
          if (prompt.isKanji(i) && rubyType.equals("B")) {
            accLength[tmpLine] += prompt.getRuby(i).length();
          } else {
            accLength[tmpLine] += prompt.getWord(i).length();
          }

          if (accLength[tmpLine] * fontSize <= maxWidth) {
            if (prompt.getScore(i) >= lineBreakScore || i == size - 1) {
              if (write > -1) {
                // Draw this prompt to this screen.
                for (int j = start; j <= i; j++) {
                  drawText(g2, prompt.getWord(j), prompt.getRuby(j), prompt.isKanji(j));
                  if (prompt.isKanji(j)) {
                    if (rubyType.equals("A")) {
                      drawRubyA(g2, prompt.getWord(j), prompt.getRuby(j));
                    } else if (rubyType.equals("B")) {
                      drawRubyB(g2, prompt.getWord(j), prompt.getRuby(j));
                    }
                  }
                }
                write = 1; // Enable to skip this while loop.
              }
              start = i + 1;
              if (start == size && write == -1) {
                nowLine = 0;
                write = 0; // Enable to write this prompt to this screen.
              }
            }
          } else if (nextLine()) {
            // Take over the word length in this line to the next line.
            int sum = 0;
            for (int j = start; j <= i; j++) {
              if (prompt.isKanji(j) && rubyType.equals("B")) {
                sum += prompt.getRuby(j).length();
              } else {
                sum += prompt.getWord(j).length();
              }
            }
            accLength[tmpLine] = sum;
            i = start;
          } else {
            // Make the font size smaller if it is possible.
            if (fontSize - gapFontSize >= lowerBoundFontSize) {
              fontSize -= gapFontSize;
            } else {
              // Relax the line break threshold if it is possible.
              if (lineBreakScore - properties.getInteger("GAP_NEW_LINE_SCORE")
                  >= lowerBoundLineBreakScore) {
                lineBreakScore -= properties.getInteger("GAP_NEW_LINE_SCORE");
                fontSize = upperBoundFontSize;
              } else {
                if (write == -1) {
                  nowLine = 0;
                  write = 0;
                } else {
                  // Could not to draw this prompt suitably on this screen.
                  for (int j = start; j < size; j++) {
                    int length;
                    if (prompt.isKanji(j) && rubyType.equals("B")) {
                      length = prompt.getRuby(j).length();
                    } else {
                      length = prompt.getWord(j).length();
                    }
                    if ((accLength[nowLine] + length) * fontSize > maxWidth) {
                      nextLine();
                    }
                    drawText(g2, prompt.getWord(j), prompt.getRuby(j), prompt.isKanji(j));
                    if (prompt.isKanji(j)) {
                      if (rubyType.equals("A")) {
                        drawRubyA(g2, prompt.getWord(j), prompt.getRuby(j));
                      } else if (rubyType.equals("B")) {
                        drawRubyB(g2, prompt.getWord(j), prompt.getRuby(j));
                      }
                    }
                  }
                  write = 1; // Skip this while loop.
                }
              }
              break; // Pseudo-back tracking.
            }
          }
        }
      }

      g2.dispose();
    }
  }

  private boolean nextLine() {
    if (nowLine + 1 < tmpLine && accLength[nowLine] != 0 && accLength[tmpLine] != 0) {
      nowLine++;
      return true;
    }
    return false;
  }

  private void drawRubyA(Graphics2D g2, String word, String ruby) {
    int x =
        properties.getInteger("X")
            + fontSize * (accLength[nowLine] - word.length())
            + (word.length() * fontSize - ruby.length() * fontSize / 2) / 2;
    int y = properties.getInteger("Y") + nowLine * properties.getInteger("GAP_Y") - fontSize;

    g2.setPaint(Color.BLUE);
    g2.setFont(new Font(fontType, Font.PLAIN, fontSize / 2));
    g2.drawString(ruby, x, y);
  }

  private void drawRubyB(Graphics2D g2, String word, String ruby) {
    int x = properties.getInteger("X") + fontSize * (accLength[nowLine] - ruby.length());
    int y = properties.getInteger("Y") + nowLine * properties.getInteger("GAP_Y") - fontSize;

    g2.setPaint(Color.BLUE);
    g2.setFont(new Font(fontType, Font.PLAIN, fontSize));
    g2.drawString(word, x, y);
  }

  private void drawText(Graphics2D g2, String word, String ruby, boolean isKanji) {
    int x = properties.getInteger("X") + fontSize * accLength[nowLine];
    int y = properties.getInteger("Y") + nowLine * properties.getInteger("GAP_Y");

    String s = word;
    if (isKanji) {
      g2.setPaint(Color.BLUE);
      if (rubyType.equals("B")) {
        s = ruby;
      }
    } else {
      g2.setPaint(Color.BLACK);
    }

    g2.setFont(new Font(fontType, Font.PLAIN, fontSize));
    g2.drawString(s, x, y);
    accLength[nowLine] += s.length();
  }
}
