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

import javax.swing.JOptionPane;

public class CallPortAudio {
  static {
    try {
      if (PlatformUtils.isWindows()) {
        System.loadLibrary("CallPortAudio");
      } else if (PlatformUtils.isLinux()) {
        System.loadLibrary("callportaudio");
      } else {
        JOptionPane.showMessageDialog(
            null, "This OS is not supported.", "Error", JOptionPane.ERROR_MESSAGE);
        throw new UnsupportedOperationException();
      }
    } catch (UnsatisfiedLinkError e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(
          null, "Cannot link PortAudio.", "Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  // Sound level
  private int level;

  // Maximum amplitude
  private double maxAmplitude;

  // Top silence
  private double topSilence;

  // End silence
  private double endSilence;

  // Power
  private double power;

  // Mutator
  public native void setSampleRate(int rate);

  public native void setSampleSize(int size);

  public native void setNumChannels(int num);

  public native void setSampleRateForBeep(int rate);

  public native void setSampleSizeForBeep(int size);

  public native void setNumChannelsForBeep(int size);

  public native void setSampleRateForSample(int rate);

  public native void setSampleSizeForSample(int size);

  public native void setNumChannelsForSample(int size);

  public native void setMaxRecordingTime(int time);

  public native void setMinTopSilence(int time);

  public native void setMinEndSilence(int time);

  public native void setSilenceLevel(double level);

  public native void setNormalizationRatio(double ratio);

  // Accessor
  public native int getSampleRate();

  public native int getSampleSize();

  public native int getNumChannels();

  public native int getSampleRateForBeep();

  public native int getSampleSizeForBeep();

  public native int getNumChannelsForBeep();

  public native int getSampleRateForSample();

  public native int getSampleSizeForSample();

  public native int getNumChannelsForSample();

  public native int getMaxRecordingTime();

  public native int getMinTopSilence();

  public native int getMinEndSilence();

  public native double getSilenceLevel();

  public native double getNormalizationRatio();

  // Stream
  public native void createInstance();

  public native boolean openStream();

  public native void closeStream();

  public native boolean isOpen();

  // Playback and recording
  public native boolean playback(String fileName, int event);

  public native void stopPlayback();

  public native void record();

  public native void stopRecording();

  public native boolean finalize(String orgFileName, String cutFileName, boolean env);

  public CallPortAudio() {
    clear();
  }

  public int getLevel() {
    int l = level;
    level = 0;
    return l;
  }

  public double getMaxAmplitude() {
    return maxAmplitude;
  }

  public double getTopSilence() {
    return topSilence;
  }

  public double getEndSilence() {
    return endSilence;
  }

  public double getPower() {
    return power;
  }

  public void clear() {
    level = 0;
    maxAmplitude = 0.0;
    topSilence = 0.0;
    endSilence = 0.0;
    power = 0.0;
  }
}
