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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

public class InputDocument extends PlainDocument {
  private static final long serialVersionUID = 1L;

  // Maximum input length
  private int maxLength;

  // Input positive pattern
  private String positivePattern;

  // Input negative pattern
  private String negativePattern;

  public InputDocument(int length, String positivePattern, String negativePattern) {
    this.maxLength = length;
    this.positivePattern = positivePattern;
    this.negativePattern = negativePattern;
  }

  public void setMaxLength(int length) {
    maxLength = length;
  }

  public void setPostivePattern(String pattern) {
    positivePattern = pattern;
  }

  public void setNegativePattern(String pattern) {
    negativePattern = pattern;
  }

  public int getMaxLength() {
    return maxLength;
  }

  public String getPositivePattern() {
    return positivePattern;
  }

  public String getNegativePattern() {
    return negativePattern;
  }

  @Override
  public void insertString(int offset, String str, AttributeSet as) throws BadLocationException {
    if (str == null) return;

    if (getLength() + str.length() > maxLength) {
      throw new BadLocationException(str, offset);
    }

    if (positivePattern != null) {
      Pattern p = Pattern.compile(positivePattern);
      Matcher m = p.matcher(str);
      if (!m.find()) return;
    }

    if (negativePattern != null) {
      Pattern p = Pattern.compile(negativePattern);
      Matcher m = p.matcher(str);
      if (m.find()) return;
    }

    super.insertString(offset, str, as);
  }
}
