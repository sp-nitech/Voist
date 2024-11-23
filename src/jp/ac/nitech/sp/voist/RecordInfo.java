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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Objects;

public class RecordInfo {
  public static enum RecordStatus {
    NOT_YET,
    SUCCESS,
    FAILURE,
    FAILURE_MAX_AMPLITUDE,
    FAILURE_MIN_AMPLITUDE,
    FAILURE_TOP_SILENCE,
    FAILURE_END_SILENCE,
  }

  //
  private File recOrgFile;

  //
  private File recCutFile;

  //
  private File recWavFile;

  //
  private File logFile;

  //
  private boolean isTmp;

  //
  private boolean isEnv;

  //
  private double maxAmplitude;

  //
  private double topSilence;

  //
  private double endSilence;

  //
  private double power;

  //
  private int numRetakes;

  //
  private RecordStatus status;

  public RecordInfo(
      String recOrgFileName, String recCutFileName, String recWavFileName, String logFileName) {
    Objects.requireNonNull(recOrgFileName);
    Objects.requireNonNull(recCutFileName);
    recOrgFile = new File(recOrgFileName);
    recCutFile = new File(recCutFileName);
    if (recWavFileName != null) {
      recWavFile = new File(recWavFileName);
    }
    if (logFileName != null) {
      logFile = new File(logFileName);
    }
    clear();
  }

  public void clear() {
    isTmp = false;
    isEnv = false;
    maxAmplitude = 0.0;
    topSilence = 0.0;
    endSilence = 0.0;
    power = 0.0;
    numRetakes = -1;
    status = isRecorded() ? RecordStatus.SUCCESS : RecordStatus.NOT_YET;
  }

  public boolean isRecorded() {
    return (recOrgFile.exists() && recOrgFile.isFile())
        || (recCutFile.exists() && recCutFile.isFile());
  }

  public void toTmp(boolean b) {
    isTmp = b;
  }

  public void toEnv(boolean b) {
    isEnv = b;
  }

  public boolean isTmp() {
    return isTmp;
  }

  public boolean isEnv() {
    return isEnv;
  }

  public void setVoiceOrgFile(String fileName) {
    recOrgFile = new File(fileName);
  }

  public void setVoiceCutFile(String fileName) {
    recCutFile = new File(fileName);
  }

  public void setVoiceWavFile(String fileName) {
    recWavFile = new File(fileName);
  }

  public void setLogFile(String fileName) {
    logFile = new File(fileName);
  }

  public void setMaxAmplitude(double amplitude) {
    maxAmplitude = amplitude;
  }

  public void setTopSilence(double silence) {
    topSilence = silence;
  }

  public void setEndSilence(double silence) {
    endSilence = silence;
  }

  public void setPower(double power) {
    this.power = power;
  }

  public void incrementNumRetakes() {
    this.numRetakes++;
  }

  public void setStatus(RecordStatus status) {
    this.status = status;
  }

  public File getVoiceOrgFile() {
    return recOrgFile;
  }

  public File getVoiceCutFile() {
    return recCutFile;
  }

  public File getVoiceWavFile() {
    return recWavFile;
  }

  public File getLogFile() {
    return logFile;
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

  public int getNumRetakes() {
    return numRetakes;
  }

  public RecordStatus getStatus() {
    return status;
  }

  public long getFileLength() {
    return isRecorded() ? recCutFile.length() : 0L;
  }

  public long getFileSize() {
    return Math.round((double) getFileLength() / 1024);
  }

  public double getPlaybackTime(int sampleRate, int sampleSize, int numChannels) {
    return (double) getFileLength() / (sampleRate * sampleSize * numChannels);
  }

  public String getFormatedTimeStamp() {
    if (isRecorded()) {
      long stamp = logFile.lastModified();
      Date date = new Date(stamp);
      return date.toString();
    }
    return "----";
  }

  public void dump() {
    if (!FileUtils.canWrite(logFile)) {
      VoistUtils.warn("Cannot dump " + logFile.getName(), "dump");
      return;
    }

    try {
      PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(logFile)));
      pw.println("# " + logFile.getName());
      pw.println("# " + getFormatedTimeStamp());
      pw.println("max amplitude=" + String.format("%.2f", maxAmplitude));
      pw.println("top silence=" + String.format("%.2f", topSilence));
      pw.println("end silence=" + String.format("%.2f", endSilence));
      pw.println("num retakes=" + String.format("%d", numRetakes));
      pw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void load() {
    if (!FileUtils.canRead(logFile)) {
      return;
    }

    try {
      FileReader fr = new FileReader(logFile);
      BufferedReader br = new BufferedReader(fr);

      int count = 0;
      String line;
      while ((line = br.readLine()) != null) {
        if (line.startsWith("#")) { // This is a comment line.
          continue;
        } else if (line.equals("")) { // Skip empty lines.
          continue;
        } else {
          String[] ary = line.split("=");
          switch (count) {
            case 0:
              maxAmplitude = Double.parseDouble(ary[1]);
              break;
            case 1:
              topSilence = Double.parseDouble(ary[1]);
              break;
            case 2:
              endSilence = Double.parseDouble(ary[1]);
              break;
            case 3:
              numRetakes = Integer.parseInt(ary[1]);
              break;
            default:
              VoistUtils.warn("Unexpected log file format: " + logFile.getName(), "load");
              break;
          }
          count++;
        }
      }

      br.close();
      fr.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
