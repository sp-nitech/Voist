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

import java.io.File;
import javax.swing.filechooser.FileSystemView;

public class PlatformUtils {
  public static final String OS_NAME = System.getProperty("os.name").toLowerCase();

  private PlatformUtils() {}

  public static boolean isLinux() {
    return OS_NAME.startsWith("linux");
  }

  public static boolean isMac() {
    return OS_NAME.startsWith("mac");
  }

  public static boolean isSunOS() {
    return OS_NAME.startsWith("sunos");
  }

  public static boolean isWindows() {
    return OS_NAME.startsWith("windows");
  }

  public static String getAppDirectory(String appName) {
    if (appName == null) {
      return null;
    }

    if (isLinux()) {
      return FileUtils.createPath(System.getProperty("user.home"), "." + appName.toLowerCase());
    } else if (isMac()) {
      return FileUtils.createPath(
          System.getProperty("user.home"), "Library", "Application Support", appName);
    } else if (isWindows()) {
      return FileUtils.createPath(getMyDocumentDirectory(), appName);
    } else if (isSunOS()) {
      return FileUtils.createPath(System.getProperty("user.dir"), appName);
    } else {
      return FileUtils.createPath(System.getProperty("user.dir"), appName);
    }
  }

  public static String getMyDocumentDirectory() {
    FileSystemView fsv = FileSystemView.getFileSystemView();
    File dir = fsv.getDefaultDirectory();
    return dir.getAbsolutePath();
  }
}
