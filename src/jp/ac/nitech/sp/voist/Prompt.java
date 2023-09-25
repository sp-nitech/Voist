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

import java.lang.Character.UnicodeBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.reduls.igo.Morpheme;
import net.reduls.igo.Tagger;

public class Prompt {
  public static final int PP_SCORE = 50;

  // Symbol "|" is inappropriate because it is used for regular expression.
  private static final String RUBY_SECTION_SYMBOL = "/";
  private static final String RUBY_BOUNDARY_SYMBOL = "@";
  private static final String NEW_LINE_SYMBOL = ";";

  private static enum CharType {
    UNKNOWN,
    KANA,
    KANJI,
    ALPHABET,
    DIGIT
  };

  private String copyright;
  private String language;
  private String content;
  private String style;
  private String db;
  private String id;

  private ArrayList<String> word;
  private ArrayList<String> ruby;
  private ArrayList<Integer> score;
  private ArrayList<Boolean> isKanji;
  private String[] kanji;
  private String[] kana;

  public Prompt() {
    final int initialCapacity = 128;
    final int arraySize = 128;

    copyright = "";
    language = "";
    content = "";
    style = "";
    db = "";
    id = "";

    word = new ArrayList<String>(initialCapacity);
    ruby = new ArrayList<String>(initialCapacity);
    score = new ArrayList<Integer>(initialCapacity);
    isKanji = new ArrayList<Boolean>(initialCapacity);
    kanji = new String[arraySize];
    kana = new String[arraySize];
  }

  public void clear() {
    copyright = null;
    language = null;
    content = null;
    style = null;
    db = null;
    id = null;

    word.clear();
    ruby.clear();
    score.clear();
    isKanji.clear();
  }

  public boolean set(
      String copyright,
      String language,
      String content,
      String style,
      String db,
      String id,
      String script,
      Tagger tagger,
      boolean consoleOutput) {
    if (VoistUtils.isEmptyString(id) || VoistUtils.isEmptyString(script) || tagger == null) {
      return false;
    }

    if (copyright != null) this.copyright = copyright;
    if (language != null) this.language = language;
    if (content != null) this.content = content;
    if (style != null) this.style = style;
    if (db != null) this.db = db;
    this.id = id;

    String formattedScript = script.replaceAll(NEW_LINE_SYMBOL + "+", NEW_LINE_SYMBOL);
    analyzeMorphology(tagger, removeRuby(formattedScript), consoleOutput);
    if (!setRuby(formattedScript)) {
      return false;
    }

    if (consoleOutput) {
      for (int i = 0; i < word.size(); i++) {
        System.out.println(String.format("[%02d] %3d pts. %s", i, score.get(i), word.get(i)));
      }
    }
    return true;
  }

  public String getCopyright() {
    return copyright;
  }

  public String getLanguage() {
    return language;
  }

  public String getContent() {
    return content;
  }

  public String getStyle() {
    return style;
  }

  public String getDb() {
    return db;
  }

  public String getId() {
    return id;
  }

  public String getFullPromptName(String ext) {
    String dotExt = (VoistUtils.isEmptyString(ext)) ? "" : "." + ext;

    return VoistUtils.joinStrings("_", copyright, language, content, "x00000", style, db, id)
        + dotExt;
  }

  public String getSimplePromptName() {
    return VoistUtils.joinStrings("_", style, db, id);
  }

  public int getNumWords() {
    return word.size();
  }

  public String getWord(int index) {
    return (word.size() == 0) ? null : word.get(index);
  }

  public String getRuby(int index) {
    return ruby.get(index);
  }

  public int getScore(int index) {
    return score.get(index);
  }

  public boolean isKanji(int index) {
    return isKanji.get(index);
  }

  private boolean setRuby(String prompt) {
    if (prompt == null) {
      return false;
    }

    int k = 0;
    for (String s : prompt.split(RUBY_SECTION_SYMBOL)) {
      if (s.contains(RUBY_BOUNDARY_SYMBOL)) {
        String[] t = s.split(RUBY_BOUNDARY_SYMBOL);
        kanji[k] = t[0];
        kana[k] = katakana2hiragana(t[1]);
        k++;
      }
    }

    for (int i = 0, j = 0; i < word.size(); i++) {
      if (isKanji.get(i)) {
        int l1 = word.get(i).length();
        if (kanji[j] == null) {
          return false;
        }
        int l2 = kanji[j].length();
        if (l1 == l2) {
          ruby.add(kana[j++]);
        } else if (l1 > l2) {
          String s = word.get(i);
          s = s.replace(kanji[j], "");
          word.add(i + 1, s);
          score.add(i + 1, score.get(i));
          isKanji.add(i + 1, true);
          word.set(i, kanji[j]);
          score.set(i, 0);
          ruby.add(kana[j++]);
        } else if (l1 < l2) {
          word.set(i + 1, word.get(i) + word.get(i + 1));
          word.remove(i);
          score.remove(i);
          isKanji.remove(i);
          if (word.get(i).length() == l2) {
            ruby.add(kana[j++]);
          } else {
            i--;
          }
        }
      } else {
        ruby.add("");
      }
    }

    return true;
  }

  private void analyzeMorphology(Tagger tagger, String sentence, boolean consoleOutput) {
    List<Morpheme> list = tagger.parse(sentence);
    Pattern pattern = Pattern.compile("\\p{InCJKUnifiedIdeographs}|々|ヵ|ヶ|○");
    String preFeature = "";

    for (Morpheme morpheme : list) {
      String surface = morpheme.surface;
      String feature = morpheme.feature;
      Matcher matcher = pattern.matcher(surface);

      // Case A: the surface contains one or more kanji.
      if (matcher.find() || surface.matches("^[ａ-ｚＡ-Ｚ]+.*$")) {
        CharType preCharType = CharType.UNKNOWN;
        char prec = '\0';
        boolean first = true;
        String cat = "";
        for (char c : surface.toCharArray()) {
          CharType charType = getCharType(c);
          if (charType != preCharType) {
            if (!cat.equals("")) {
              word.add(cat);
              isKanji.add(
                  getCharType(prec) == CharType.KANJI || getCharType(prec) == CharType.ALPHABET);
              if (first) {
                setLineBreakScore(preFeature, feature);
                first = false;
              } else {
                score.add(0);
              }
              preFeature = feature;
              cat = "";
            }
          }
          cat += c;
          preCharType = charType;
          prec = c;
        }
        if (!cat.equals("")) {
          word.add(cat);
          isKanji.add(
              getCharType(prec) == CharType.KANJI || getCharType(prec) == CharType.ALPHABET);
          if (!preFeature.equals(feature)) {
            setLineBreakScore(preFeature, feature);
          } else {
            score.add(0);
          }
        }
        preFeature = feature;
      }
      // Case B: the surface contains any kanji.
      else {
        if (!surface.equals(NEW_LINE_SYMBOL)) {
          word.add(surface);
          isKanji.add(false);
          setLineBreakScore(preFeature, feature);
          preFeature = feature;
        } else {
          preFeature = NEW_LINE_SYMBOL;
        }
      }

      if (consoleOutput) {
        System.out.println(surface + "\t" + feature);
      }
    }

    setLineBreakScore(preFeature, "");
  }

  private void setLineBreakScore(String currentFeature, String postFeature) {
    if (currentFeature.equals("")) {
      return;
    }

    int score = 0;
    if (!(postFeature.endsWith("」")
        || postFeature.startsWith("記号,句点")
        || postFeature.endsWith("？")
        || postFeature.endsWith("！")
        || postFeature.startsWith("記号,読点")
        || postFeature.startsWith("記号,空白"))) {
      if (currentFeature.endsWith("」")) score = 100;
      else if (currentFeature.startsWith("記号,句点")) score = 100;
      else if (currentFeature.endsWith("？")) score = 100;
      else if (currentFeature.endsWith("！")) score = 100;
      else if (currentFeature.startsWith("記号,読点")) score = 95;
      else if (currentFeature.startsWith("記号,空白")) score = 90;
      else if (currentFeature.startsWith("助詞")) score = PP_SCORE;
      else if (currentFeature.startsWith("名詞")) score = 8;
      else if (currentFeature.startsWith("動詞")) score = 5;
      else score = 1;
      ;
    }

    if (currentFeature.equals(NEW_LINE_SYMBOL)) {
      score = 100;
    }

    this.score.add(score);
  }

  private static String removeRuby(String prompt) {
    String noRuby = prompt.replaceAll(RUBY_BOUNDARY_SYMBOL + ".*?" + RUBY_SECTION_SYMBOL, "");
    noRuby = noRuby.replaceAll(RUBY_SECTION_SYMBOL, "");
    return noRuby;
  }

  private static CharType getCharType(char c) {
    UnicodeBlock ub = UnicodeBlock.of(c);

    CharType ct = CharType.UNKNOWN;
    if (ub == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || c == '々' || c == 'ヵ' || c == 'ヶ' || c == '○') {
      ct = CharType.KANJI;
    } else if (ub == UnicodeBlock.HIRAGANA || ub == UnicodeBlock.KATAKANA) {
      ct = CharType.KANA;
    } else if (ub == UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
      if (0xFF10 <= c && c <= 0xFF19) {
        ct = CharType.DIGIT;
      } else if (0xFF21 <= c && c <= 0xFF3A || 0xFF41 <= c && c <= 0xFF5A) {
        ct = CharType.ALPHABET;
      }
    }

    return ct;
  }

  public static String hiragana2katakana(String hiragana) {
    if (hiragana == null) {
      return null;
    }

    StringBuffer sb = new StringBuffer(hiragana);
    for (int i = 0; i < sb.length(); i++) {
      char c = sb.charAt(i);
      if (c >= 'ぁ' && c <= 'ん') {
        sb.setCharAt(i, (char) (c - 'ぁ' + 'ァ'));
      }
    }

    return sb.toString();
  }

  public static String katakana2hiragana(String katakana) {
    if (katakana == null) {
      return null;
    }

    StringBuffer sb = new StringBuffer(katakana);
    for (int i = 0; i < sb.length(); i++) {
      char c = sb.charAt(i);
      if (c >= 'ァ' && c <= 'ン') {
        sb.setCharAt(i, (char) (c - 'ァ' + 'ぁ'));
      } else if (c == 'ヵ') {
        sb.setCharAt(i, 'か');
      } else if (c == 'ヶ') {
        sb.setCharAt(i, 'け');
      } else if (c == 'ヴ') {
        sb.setCharAt(i, 'う');
        sb.insert(i + 1, '゛');
        i++;
      }
    }

    return sb.toString();
  }
}
