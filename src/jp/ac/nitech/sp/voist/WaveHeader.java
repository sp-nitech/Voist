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
import java.io.OutputStream;

public class WaveHeader {
  //
  private int sampleRate;

  //
  private int bitsPerSample;

  //
  private int numChannels;

  public WaveHeader(int rate, int size, int num) {
    setSampleRate(rate);
    setSampleSize(size);
    setNumChannels(num);
  }

  public void setSampleRate(int rate) {
    sampleRate = rate;
  }

  public void setSampleSize(int size) {
    bitsPerSample = size * 8;
  }

  public void setNumChannels(int num) {
    numChannels = num;
  }

  public int getSampleRate() {
    return sampleRate;
  }

  public int getSampleSize() {
    return bitsPerSample / 8;
  }

  public int getNumChannels() {
    return numChannels;
  }

  // TODO
  // public boolean readWav() {}

  public boolean writeWavFromRaw(File rawFile, File wavFile) {
    if (rawFile == null || wavFile == null) {
      return false;
    }

    try {
      FileInputStream fis = new FileInputStream(rawFile);
      FileOutputStream fos = new FileOutputStream(wavFile);

      int numBytes = (int) rawFile.length();

      try {
        // RIFF header
        writeString(fos, "RIFF");
        writeInteger(fos, 36 + numBytes);
        writeString(fos, "WAVE");

        // fmt chunk
        writeString(fos, "fmt ");
        writeInteger(fos, 16);
        writeShort(fos, (short) 1);
        writeShort(fos, (short) numChannels);
        writeInteger(fos, sampleRate);
        writeInteger(fos, numChannels * sampleRate * bitsPerSample / 8);
        writeShort(fos, (short) (numChannels * bitsPerSample / 8));
        writeShort(fos, (short) bitsPerSample);

        // data chunk
        writeString(fos, "data");
        writeInteger(fos, numBytes);

        // Waveform data
        int data = -1;
        do {
          try {
            data = fis.read();
            fos.write(data);
          } catch (IOException e) {
            e.printStackTrace();
          }
        } while (data != -1);

        fos.close();
        fis.close();
      } catch (IOException e) {
        e.printStackTrace();
        return false;
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return false;
    }

    return true;
  }

  private static void writeString(OutputStream os, String val) throws IOException {
    int length = val.length();
    for (int i = 0; i < length; i++) {
      os.write(val.charAt(i));
    }
  }

  private static void writeInteger(OutputStream os, int val) throws IOException {
    os.write(val >> 0);
    os.write(val >> 8);
    os.write(val >> 16);
    os.write(val >> 24);
  }

  private static void writeShort(OutputStream os, short val) throws IOException {
    os.write(val >> 0);
    os.write(val >> 8);
  }
}
