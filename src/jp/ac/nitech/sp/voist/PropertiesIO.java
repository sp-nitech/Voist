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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

public class PropertiesIO extends Properties {
  private static final long serialVersionUID = 1L;

  private File ioFile;

  public PropertiesIO() {
    ioFile = null;
  }

  public PropertiesIO(String fileName) {
    setIOFile(fileName);
  }

  public void setIOFile(String fileName) {
    ioFile = new File(fileName);
    if (ioFile != null && !ioFile.exists()) {
      try {
        ioFile.createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public File getIOFile() {
    return ioFile;
  }

  public void setProperty(String key, boolean value) {
    setProperty(key, Boolean.toString(value));
  }

  public void setProperty(String key, int value) {
    setProperty(key, Integer.toString(value));
  }

  public void setProperty(String key, double value) {
    setProperty(key, Double.toString(value));
  }

  @Override
  public String getProperty(String key) {
    String value = super.getProperty(key);
    if (value == null) {
      throw new IllegalArgumentException("Unexpected key: " + key);
    }
    return value;
  }

  public String getString(String key) {
    return getProperty(key);
  }

  public Boolean getBoolean(String key) {
    return Boolean.valueOf(getProperty(key));
  }

  public int getInteger(String key) {
    return Integer.valueOf(getProperty(key));
  }

  public int getInteger(String key, int min) {
    int i = getInteger(key);
    if (i < min) {
      throw new IllegalArgumentException();
    }
    return i;
  }

  public int getInteger(String key, int min, int max) {
    int i = getInteger(key);
    if (i < min || max < i) {
      throw new IllegalArgumentException();
    }
    return i;
  }

  public double getDouble(String key) {
    return Double.valueOf(getProperty(key));
  }

  public double getDouble(String key, double min) {
    double d = getDouble(key);
    if (d < min) {
      throw new IllegalArgumentException();
    }
    return d;
  }

  public double getDouble(String key, double min, double max) {
    double d = getDouble(key);
    if (d < min || max < d) {
      throw new IllegalArgumentException();
    }
    return d;
  }

  public void invert(String key) {
    boolean b = getBoolean(key);
    setProperty(key, !b);
  }

  public boolean load() throws IOException {
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(ioFile);
      load(new InputStreamReader(fis, "UTF-8"));
      return true;
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (fis != null) {
        fis.close();
      }
    }
    return false;
  }

  public boolean store() throws IOException {
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(ioFile);
      store(fos, "User properties");
      return true;
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (fos != null) {
        fos.close();
      }
    }
    return false;
  }
}
