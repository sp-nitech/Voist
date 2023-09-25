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

public class VoistUtils {
  private VoistUtils() {}

  public static void info(String... messages) {
    boolean first = true;
    for (String message : messages) {
      if (first) {
        System.out.print("[ INFO ] ");
        first = false;
      } else {
        System.out.print("         ");
      }
      if (!VoistUtils.isEmptyString(message)) {
        System.out.println(message);
      }
    }
  }

  public static boolean isEmptyString(String str) {
    return !(str != null && !str.equals(""));
  }

  public static void warn(String message, String function) {
    System.err.println("[ WARN ] " + message + " in " + function + "()");
  }

  public static String joinStrings(String separator, String... strs) {
    StringBuffer sb = new StringBuffer();
    boolean first = true;
    boolean insertsSeparator = !VoistUtils.isEmptyString(separator);

    for (String str : strs) {
      if (!VoistUtils.isEmptyString(str)) {
        if (first) {
          first = false;
        } else if (insertsSeparator) {
          sb.append(separator);
        }
        sb.append(str);
      }
    }

    return sb.toString();
  }
}
