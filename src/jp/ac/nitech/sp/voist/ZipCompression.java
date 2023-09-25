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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipCompression {
  private static final int MAX_DEPTH = 8;
  private static final int BUFFER_SIZE = 1024;

  private int level;
  private String encoding;

  public ZipCompression() {
    level = Deflater.DEFAULT_COMPRESSION;
    encoding = PlatformUtils.isWindows() ? "Shift_JIS" : "UTF-8";
  }

  public void setLevel(int level) {
    this.level = (0 <= level && level <= 9) ? level : Deflater.DEFAULT_COMPRESSION;
  }

  public boolean setEncoding(String encoding) {
    if (encoding != null) {
      this.encoding = encoding;
      return true;
    }
    return false;
  }

  public int getLevel() {
    return level;
  }

  public String getEncoding() {
    return encoding;
  }

  public boolean zip(File srcDir, File destFile) {
    if (srcDir == null || destFile == null) {
      return false;
    }

    ZipOutputStream zos = null;

    try {
      Charset charset;
      if (encoding.equals("Shift_JIS")) {
        charset = Charset.forName("MS932");
      } else {
        charset = StandardCharsets.UTF_8;
      }
      zos = new ZipOutputStream(new FileOutputStream(destFile), charset);
      zos.setLevel(level);
      archive(zos, srcDir, destFile, 0);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return false;
    } finally {
      if (zos != null) {
        try {
          zos.closeEntry();
          zos.flush();
          zos.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return true;
  }

  private static void archive(ZipOutputStream stream, File srcDir, File destFile, int depth) {
    if (MAX_DEPTH < depth) {
      return;
    }

    if (srcDir.isDirectory()) {
      File[] srcFiles = srcDir.listFiles();
      for (File srcFile : srcFiles) {
        if (srcFile.isDirectory()) {
          archive(stream, srcFile, destFile, depth + 1);
        } else {
          if (!srcFile.getAbsoluteFile().equals(destFile)) {
            String srcPath = srcFile.getAbsolutePath();
            String entryName = srcPath.replace(destFile.getParent(), "");
            entryName = entryName.substring(1);
            // Use slashes rather than backslashes as path separators
            // to avoid a warning in unzip command.
            entryName = entryName.replace("\\", "/");
            archive(stream, srcFile, destFile, entryName);
          }
        }
      }
    }
  }

  private static void archive(
      ZipOutputStream stream, File srcFile, File destFile, String entryName) {
    try {
      stream.putNextEntry(new ZipEntry(entryName));
      FileInputStream fis = new FileInputStream(srcFile);
      BufferedInputStream bis = new BufferedInputStream(fis);

      byte[] buffer = new byte[BUFFER_SIZE];
      int readSize = bis.read(buffer, 0, BUFFER_SIZE);

      while (readSize != -1) {
        stream.write(buffer, 0, readSize);
        readSize = bis.read(buffer, 0, BUFFER_SIZE);
      }

      bis.close();
      fis.close();
      stream.closeEntry();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
