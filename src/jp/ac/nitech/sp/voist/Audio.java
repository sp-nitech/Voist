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
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;
import jp.ac.nitech.sp.voist.RecordInfo.RecordStatus;

public class Audio extends CallPortAudio {
  public static enum PlaybackEvent {
    OFF,
    BEEP,
    SAMPLE,
    VOICE_ORG,
    VOICE_CUT,
    VOICE_TMP,
    VOICE_WAV,
  }

  public static enum PlaybackResult {
    PLAY,
    STOP,
    ERROR
  }

  public static enum RecordingEvent {
    OFF,
    ON,
  }

  // SwingWorker for sound playback
  private SwingWorker<String, String> playback;

  // Playback event
  private PlaybackEvent playbackEvent;

  // Recording event
  private RecordingEvent recordingEvent;

  // Margin time [msec],
  // which prevents to record the noise occurred by mouse click or key touch
  private int marginTime;

  public Audio() {
    super.createInstance();
    playback = null;
    playbackEvent = PlaybackEvent.OFF;
    recordingEvent = RecordingEvent.OFF;
    marginTime = 0;
  }

  public boolean setMarginTime(int time) {
    if (time < 0 || getMaxRecordingTime() < time) {
      return false;
    }
    marginTime = time;
    return true;
  }

  public int getMarginTime() {
    return marginTime;
  }

  public PlaybackEvent getPlaybackEvent() {
    return playbackEvent;
  }

  public RecordingEvent getRecordingEvent() {
    return recordingEvent;
  }

  public boolean open() {
    if (!isOpen()) {
      new Stream().execute();
      return true;
    }
    return false;
  }

  public boolean close() {
    if (isOpen()) {
      new Stream().execute();
      return true;
    }
    return false;
  }

  public PlaybackResult playback(String fileName, PlaybackEvent newEvent) {
    super.stopPlayback();

    if (fileName == null || newEvent == null) {
      return PlaybackResult.STOP;
    }
    if (newEvent == PlaybackEvent.OFF || playbackEvent == newEvent) {
      return PlaybackResult.STOP;
    }

    if (FileUtils.canRead(new File(fileName))) {
      if (playbackEvent != PlaybackEvent.OFF && playback != null) {
        try {
          playback.get();
        } catch (InterruptedException e) {
          e.printStackTrace();
          return PlaybackResult.ERROR;
        } catch (ExecutionException e) {
          e.printStackTrace();
          return PlaybackResult.ERROR;
        }
      }
      playback = new Playback(newEvent, fileName);
      playback.execute();
      return PlaybackResult.PLAY;
    }

    return PlaybackResult.ERROR;
  }

  public boolean playback(File file, PlaybackEvent newEvent) {
    super.stopPlayback();

    if (file == null || newEvent == null) {
      return false;
    }
    if (newEvent == PlaybackEvent.OFF || playbackEvent == newEvent) {
      return true;
    }

    if (FileUtils.canRead(file)) {
      if (playbackEvent != PlaybackEvent.OFF && playback != null) {
        try {
          playback.get();
        } catch (InterruptedException e) {
          e.printStackTrace();
          return false;
        } catch (ExecutionException e) {
          e.printStackTrace();
          return false;
        }
      }
      playback = new Playback(newEvent, file.getAbsolutePath());
      playback.execute();
      return true;
    }

    return false;
  }

  public boolean record(RecordInfo info, String beepFileName, boolean env) {
    switch (recordingEvent) {
      case OFF:
        playback(beepFileName, PlaybackEvent.BEEP);
        try {
          Thread.sleep(marginTime);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        super.record();
        recordingEvent = RecordingEvent.ON;
        return true;
      case ON:
        super.stopRecording();

        if (info == null) {
          return false;
        }

        boolean statusCode =
            super.finalize(
                info.getVoiceOrgFile().getAbsolutePath(),
                info.getVoiceCutFile().getAbsolutePath(),
                env);
        info.setMaxAmplitude(getMaxAmplitude());
        info.setTopSilence(getTopSilence());
        info.setEndSilence(getEndSilence());
        info.setPower(getPower());

        if (statusCode) {
          info.setStatus(RecordStatus.SUCCESS);
        } else {
          if (getTopSilence() < (double) getMinTopSilence() / 1000) {
            info.setStatus(RecordStatus.FAILURE_TOP_SILENCE);
          } else if (getEndSilence() < (double) getMinEndSilence() / 1000) {
            info.setStatus(RecordStatus.FAILURE_END_SILENCE);
          } else {
            info.setStatus(RecordStatus.FAILURE);
          }
        }

        super.clear();
        recordingEvent = RecordingEvent.OFF;
        return true;
      default:
        return false;
    }
  }

  private class Playback extends SwingWorker<String, String> {
    private final PlaybackEvent nextEvent;
    private final String fileName;

    public Playback(PlaybackEvent playbackEvent, String fileName) {
      nextEvent = playbackEvent == null ? PlaybackEvent.OFF : playbackEvent;
      this.fileName = fileName;
    }

    @Override
    public String doInBackground() {
      playbackEvent = nextEvent;
      playback(fileName, nextEvent.ordinal());
      playbackEvent = PlaybackEvent.OFF;
      return null;
    }
  }

  private class Stream extends SwingWorker<String, String> {
    public Stream() {}

    @Override
    public String doInBackground() {
      if (isOpen()) {
        stopPlayback();
        closeStream();
      } else {
        openStream();
      }
      return null;
    }
  }
}
