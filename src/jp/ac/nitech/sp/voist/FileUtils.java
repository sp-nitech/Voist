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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class FileUtils {
  private FileUtils() {}

  public static String createPath(String... strs) {
    StringBuffer sb = new StringBuffer();
    for (String str : strs) {
      if (str != null && !str.equals("")) {
        if (sb.length() != 0) {
          sb.append(File.separator);
        }
        sb.append(str);
      }
    }
    return sb.toString();
  }

  public static boolean canWrite(File file) {
    if (file == null) {
      return false;
    }
    if (!file.exists()) {
      try {
        file.createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return file.isFile() && file.canWrite();
  }

  public static boolean canRead(File file) {
    return file != null && file.exists() && file.isFile() && file.canRead();
  }

  public static boolean copy(File srcFile, File destFile) {
    if (srcFile == null || destFile == null) {
      return false;
    }

    FileInputStream fis = null;
    FileOutputStream fos = null;
    FileChannel srcChannel = null;
    FileChannel destChannel = null;

    try {
      fis = new FileInputStream(srcFile);
      fos = new FileOutputStream(destFile);
      srcChannel = fis.getChannel();
      destChannel = fos.getChannel();
      srcChannel.transferTo(0, srcChannel.size(), destChannel);
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    } finally {
      if (destChannel != null) {
        try {
          destChannel.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      if (srcChannel != null) {
        try {
          srcChannel.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      if (fos != null) {
        try {
          fos.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return true;
  }

  public static boolean delete(File file) {
    return file != null && file.exists() && file.delete();
  }
}
