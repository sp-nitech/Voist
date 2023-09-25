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

#include "jp_ac_nitech_sp_voist_CallPortAudio.h"

#include <cmath>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <iostream>

#include "portaudio.h"

#pragma warning(disable : 4996)  // fopen_s

class CallPortAudio {
 public:
  enum PlaybackEvent {
    None,
    Beep,
    Sample,
    VoiceOrg,
    VoiceCut,
    VoiceTmp,
    VoiceWav,
  };

  CallPortAudio()
      : record_data_(NULL),
        frame_(0),
        sample_rate_(48000),
        sample_rate_for_beep_(48000),
        sample_rate_for_sample_(20000),
        sample_size_(3),
        sample_size_for_beep_(3),
        sample_size_for_sample_(2),
        frame_length_(25),
        frame_shift_(5),
        frames_per_buffer_(1024),
        num_channels_(1),
        max_recording_time_(20000),
        min_top_silence_(400),
        min_end_silence_(600),
        silence_level_(0.1),
        normalization_ratio_(0.15),
        open_(false),
        record_(false),
        playback_(false) {
  }

  ~CallPortAudio() {
  }

  void SetSampleRate(int rate) {
    sample_rate_ = rate;
  }

  void SetSampleRateForBeep(int rate) {
    sample_rate_for_beep_ = rate;
  }

  void SetSampleRateForSample(int rate) {
    sample_rate_for_sample_ = rate;
  }

  void SetSampleSize(int size) {
    sample_size_ = size;
  }

  void SetSampleSizeForBeep(int size) {
    sample_size_for_beep_ = size;
  }

  void SetSampleSizeForSample(int size) {
    sample_size_for_sample_ = size;
  }

  void SetFrameLength(int time) {
    frame_length_ = time;
  }

  void SetFrameShift(int time) {
    frame_shift_ = time;
  }

  void SetFramesPerBuffer(int num) {
    frames_per_buffer_ = num;
  }

  void SetNumChannels(int num) {
    num_channels_ = num;
  }

  void SetMaxRecordingTime(int time) {
    max_recording_time_ = time;
  }

  void SetMinTopSilence(int time) {
    min_top_silence_ = time;
  }

  void SetMinEndSilence(int time) {
    min_end_silence_ = time;
  }

  void SetSilenceLevel(double level) {
    silence_level_ = level;
  }

  void SetNormalizationRatio(double ratio) {
    normalization_ratio_ = ratio;
  }

  int GetSampleRate() const {
    return sample_rate_;
  }

  int GetSampleRateForBeep() const {
    return sample_rate_for_beep_;
  }

  int GetSampleRateForSample() const {
    return sample_rate_for_sample_;
  }

  int GetSampleSize() const {
    return sample_size_;
  }

  int GetSampleSizeForBeep() const {
    return sample_size_for_beep_;
  }

  int GetSampleSizeForSample() const {
    return sample_size_for_sample_;
  }

  int GetFrameLength() const {
    return frame_length_;
  }

  int GetFrameShift() const {
    return frame_shift_;
  }

  int GetFramesPerBuffer() const {
    return frames_per_buffer_;
  }

  int GetNumChannels() {
    return num_channels_;
  }

  int GetMaxRecordingTime() const {
    return max_recording_time_;
  }

  int GetMinTopSilence() const {
    return min_top_silence_;
  }

  int GetMinEndSilence() const {
    return min_end_silence_;
  }

  double GetSilenceLevel() const {
    return silence_level_;
  }

  double GetNormalizationRatio() const {
    return normalization_ratio_;
  }

  bool OpenStream(JNIEnv *env, jobject obj) {
    PaError error;

    error = Pa_Initialize();
    if (error != paNoError) {
      Terminate(error, 0010, NULL);
      return false;
    }

    PaStreamParameters parameters;
    parameters.device = Pa_GetDefaultInputDevice();
    if (parameters.device == paNoDevice) {
      Terminate(error, 0020, NULL);
      return false;
    }
    parameters.channelCount = num_channels_;
    parameters.sampleFormat = GetSampleFormat(sample_size_);
    parameters.suggestedLatency =
        Pa_GetDeviceInfo(parameters.device)->defaultLowInputLatency;
    parameters.hostApiSpecificStreamInfo = NULL;

    PaStream *stream;
    error = Pa_OpenStream(&stream, &parameters, NULL, sample_rate_,
                          frames_per_buffer_, paClipOff, NULL, NULL);
    if (error != paNoError) {
      Terminate(error, 0030, NULL);
      return false;
    }

    error = Pa_StartStream(stream);
    if (error != paNoError) {
      Terminate(error, 0040, NULL);
      return false;
    }

    // Allocate memory.
    const int max_frame(sample_rate_ * max_recording_time_ / 1000 *
                        num_channels_);
    record_data_ = static_cast<int *>(std::malloc(sizeof(int) * max_frame));
    if (record_data_ == NULL) {
      std::cerr << "Cannot allocate memory to record voice" << std::endl;
      Terminate(NULL);
      return false;
    }

    // Initialize.
    std::memset(record_data_, 0, sizeof(int) * max_frame);
    frame_ = 0;

    std::cout << "*** Start Recording ***" << std::endl << std::endl;

    const jclass j_class(env->GetObjectClass(obj));
    const jfieldID j_field(env->GetFieldID(j_class, "level", "I"));

    int buffer;
    open_ = true;

    while (open_) {
      error = Pa_ReadStream(stream, &buffer, 1);
      if (error != paNoError) {
        Terminate(error, 0050, record_data_);
        return false;
      }

      if (sample_size_ == 2) {
        Int16ToInt(&buffer, &buffer);
      } else if (sample_size_ == 3) {
        Int24ToInt(&buffer, &buffer);
      } else if (sample_size_ == 4) {
        // nothing to do
      } else {
        Terminate(error, 0055, record_data_);
        return false;
      }

      // Get sound level.
      const int level(abs(buffer));
      if (env->GetIntField(obj, j_field) < level) {
        env->SetIntField(obj, j_field, level);
      }

      // Record.
      if (record_ && frame_ < max_frame) {
        record_data_[frame_++] = buffer;
      }
    }

    error = Pa_CloseStream(stream);
    if (error != paNoError) {
      Terminate(error, 0060, record_data_);
      return false;
    }

    std::cout << std::endl << "*** End Recording ***" << std::endl;

    Terminate(record_data_);

    return true;
  }

  void CloseStream() {
    open_ = false;
  }

  bool IsOpen() const {
    return open_;
  }

  bool Playback(JNIEnv *env, jobject obj, jstring file_name, jint event) {
    std::cout << "Call Playback()" << std::endl;

    int sample_rate;
    int sample_size;
    if (event == Beep) {
      sample_rate = sample_rate_for_beep_;
      sample_size = sample_size_for_beep_;
    } else if (event == Sample) {
      sample_rate = sample_rate_for_sample_;
      sample_size = sample_size_for_sample_;
    } else {
      sample_rate = sample_rate_;
      sample_size = sample_size_;
    }

    std::cout << "  Sample rate: " << sample_rate << std::endl;
    std::cout << "  Sample size: " << sample_size << std::endl;

    PaError error;
    error = Pa_Initialize();
    if (error != paNoError) {
      Terminate(error, 0110, NULL);
      return false;
    }

    PaStreamParameters parameters;
    parameters.device = Pa_GetDefaultOutputDevice();
    if (parameters.device == paNoDevice) {
      Terminate(error, 0120, NULL);
      return false;
    }
    parameters.channelCount = num_channels_;
    parameters.sampleFormat = GetSampleFormat(sample_size);
    parameters.suggestedLatency =
        Pa_GetDeviceInfo(parameters.device)->defaultLowOutputLatency;
    parameters.hostApiSpecificStreamInfo = NULL;

    PaStream *stream;
    error = Pa_OpenStream(&stream, NULL, &parameters, sample_rate,
                          frames_per_buffer_, paClipOff, NULL, NULL);
    if (error != paNoError) {
      Terminate(error, 0130, NULL);
      return false;
    }

    error = Pa_StartStream(stream);
    if (error != paNoError) {
      Terminate(error, 0140, NULL);
      return false;
    }

    // Initialize.
    int frame(0);

    // Open file.
    const char *file(env->GetStringUTFChars(file_name, 0));
    FILE *fp(std::fopen(file, "rb"));
    if (fp == NULL) {
      std::cerr << "Cannot open " << file << std::endl;
      Terminate(NULL);
      return false;
    }

    std::cout << "  Playback: " << file << std::endl;

    // Playback.
    int buffer;
    playback_ = true;
    while (std::fread(&buffer, num_channels_ * sample_size, 1, fp)) {
      error = Pa_WriteStream(stream, &buffer, 1);
      if (error != paNoError) {
        std::fclose(fp);
        Terminate(error, 0160, NULL);
        break;
      }
      if (!playback_) {
        break;
      }
    }
    playback_ = false;

    std::fclose(fp);

    error = Pa_CloseStream(stream);
    if (error != paNoError) {
      Terminate(error, 0170, NULL);
      return false;
    }

    std::cout << "  Done" << std::endl;

    return true;
  }

  void StopPlayback() {
    playback_ = false;
  }

  void Record() {
    frame_ = 0;
    record_ = true;
  }

  void StopRecording() {
    record_ = false;
  }

  bool Finalize(JNIEnv *env, jobject obj, jstring org_file_name,
                jstring cut_file_name, jboolean environment) {
    std::cout << "Call Finalize()" << std::endl;

    const jclass j_class(env->GetObjectClass(obj));
    jfieldID j_field;

    // Write record data.
    const char *org_file(env->GetStringUTFChars(org_file_name, 0));
    FILE *org_fp(std::fopen(org_file, "wb"));
    if (org_fp == NULL) {
      std::cerr << "Cannot open " << org_file << std::endl;
      return false;
    }
    std::cout << "  Writing data to " << org_file << " " << frame_ << std::endl;
    for (int i(0); i < frame_; ++i) {
      std::fwrite(&record_data_[i], num_channels_ * sample_size_, 1, org_fp);
    }
    std::fclose(org_fp);

    // Calculate RMS.
    const int frame_length_pt(sample_rate_ * frame_length_ / 1000);
    const int frame_shift_pt(sample_rate_ * frame_shift_ / 1000);
    double max_rms(0.0);
    for (int i(0); i + frame_length_pt < frame_; i += frame_shift_pt) {
      double sqr(0.0);
      for (int j(i); j < i + frame_length_pt; ++j) {
        sqr += static_cast<double>(record_data_[j]) * record_data_[j];
      }
      const double rms(std::sqrt(sqr / frame_length_pt));
      if (rms > max_rms) {
        max_rms = rms;
      }
    }

    int *normalized_data(static_cast<int *>(std::malloc(sizeof(int) * frame_)));
    if (normalized_data == NULL) {
      std::cerr << "Cannot allocate memory to write normalized data"
                << std::endl;
      return false;
    }

    // Normalize data.
    const double max_amplitude(std::pow(2.0, (sample_size_ * 8)) * 0.5);
    const double max_normalized_amplitude(max_amplitude * normalization_ratio_);
    const double scale((environment || max_rms == 0.0)
                           ? 1.0
                           : max_normalized_amplitude / max_rms);
    for (int i(0); i < frame_; ++i) {
      normalized_data[i] = static_cast<int>(scale * record_data_[i]);
    }

    // Set maximum amplitude ratio.
    int max(0);
    for (int i(0); i < frame_; ++i) {
      int amplitude(std::abs(record_data_[i]));
      if (amplitude > max) {
        max = amplitude;
      }
    }
    j_field = env->GetFieldID(j_class, "maxAmplitude", "D");
    env->SetDoubleField(obj, j_field, 100.0 * max / max_amplitude);

    // Find silence intervals.
    // [ 0 .. top_file .. end_file .. frame_ ]

    const double silence_rms(silence_level_ * max_normalized_amplitude);

    // Find top silence.
    int top_file;
    int top_speech;
    double top_silence;
    if (environment || silence_level_ == 0.0) {
      top_file = sample_rate_ * min_top_silence_ / 1000;
      top_speech = top_file;
      top_silence = static_cast<double>(min_top_silence_) / 1000;
    } else {
      int f(0);
      for (; f + frame_length_pt < frame_; f += frame_shift_pt) {
        double sqr(0.0);
        for (int i(f); i < f + frame_length_pt; ++i) {
          sqr += static_cast<double>(normalized_data[i]) * normalized_data[i];
        }
        if (std::sqrt(sqr / frame_length_pt) > silence_rms) {
          break;
        }
      }
      top_file = f - sample_rate_ * min_top_silence_ / 1000;
      top_speech = f;
      top_silence = static_cast<double>(f) / sample_rate_;
      std::cout << "  top: " << top_file << " (accept if top > 0)" << std::endl;
    }
    j_field = env->GetFieldID(j_class, "topSilence", "D");
    env->SetDoubleField(obj, j_field, top_silence);

    // Find end silence.
    int end_speech;
    int end_file;
    double end_silence;
    if (environment || silence_level_ == 0.0) {
      end_file = frame_ - sample_rate_ * min_end_silence_ / 1000;
      end_speech = end_file;
      end_silence = static_cast<double>(min_end_silence_) / 1000;
    } else {
      int f(frame_ - 1);
      for (; f - frame_length_pt >= 0; f -= frame_shift_pt) {
        double sqr = 0.0;
        for (int i(f); i > f - frame_length_pt; --i) {
          sqr += static_cast<double>(normalized_data[i]) * normalized_data[i];
        }
        if (std::sqrt(sqr / frame_length_pt) > silence_rms) {
          break;
        }
      }
      end_file = f + sample_rate_ * min_end_silence_ / 1000;
      end_speech = f;
      end_silence = static_cast<double>(frame_ - f) / sample_rate_;
      std::cout << "  end: " << end_file << " (accept if end < " << frame_
                << ")" << std::endl;
    }
    j_field = env->GetFieldID(j_class, "endSilence", "D");
    env->SetDoubleField(obj, j_field, end_silence);

    // Failed to record.
    if (top_file < 0 || end_file >= frame_ || top_file >= end_file) {
      std::free(normalized_data);
      return false;
    }

    // Set power.
    double sqr(0.0);
    for (int i(top_speech); i < end_speech; ++i) {
      sqr += static_cast<double>(record_data_[i]) * record_data_[i];
    }
    j_field = env->GetFieldID(j_class, "power", "D");
    env->SetDoubleField(obj, j_field, 10.0 * std::log10(sqr / frame_));

    // Write normalized data.
    const char *cut_file(env->GetStringUTFChars(cut_file_name, 0));
    FILE *cut_fp(fopen(cut_file, "wb"));
    if (cut_fp == NULL) {
      std::cerr << "Cannot open " << cut_file << std::endl;
      std::free(normalized_data);
      return false;
    }
    std::cout << "  Writing data to " << cut_file << " " << end_file
              << std::endl;
    for (int i(top_file); i < end_file; ++i) {
      std::fwrite(&normalized_data[i], num_channels_ * sample_size_, 1, cut_fp);
    }
    std::fclose(cut_fp);

    std::cout << "  Done" << std::endl;

    std::free(normalized_data);

    return true;
  }

 private:
  void Terminate(int *data) const {
    if (data) {
      std::free(data);
      data = NULL;
    }
    Pa_Terminate();
  }

  void Terminate(PaError error, int error_code, int *data) const {
    std::cerr << "Error in PortAudio" << std::endl;
    std::cerr << "  Error number: " << error << std::endl;
    std::cerr << "  Error message: " << Pa_GetErrorText(error) << std::endl;
    std::cerr << "  Error code: " << error_code << std::endl;
    Terminate(data);
  }

  PaSampleFormat GetSampleFormat(int sample_size) const {
    switch (sample_size) {
      case 1:
        return paInt8;
      case 2:
        return paInt16;
      case 3:
        return paInt24;
      case 4:
        return paInt32;
      default:
        return 0;
    }
  }

  void Int16ToInt(void *x1, void *x2) const {
    int y(*(static_cast<int *>(x1)) & 0x0000FFFF);
    if (y >> 15 == 1) {
      y = y | 0xFFFF0000;
    }
    long long xl(y);
    *(static_cast<int *>(x2)) = static_cast<int>(xl);
  }

  void Int24ToInt(void *x1, void *x2) const {
    int y(*(static_cast<int *>(x1)) & 0x00FFFFFF);
    if (y >> 23 == 1) {
      y = y | 0xFF000000;
    }
    long long xl(y);
    *(static_cast<int *>(x2)) = static_cast<int>(xl);
  }

  int *record_data_;
  int frame_;

  int sample_rate_;  // [Hz]
  int sample_rate_for_beep_;
  int sample_rate_for_sample_;
  int sample_size_;  // [byte]
  int sample_size_for_beep_;
  int sample_size_for_sample_;
  int frame_length_;  // [msec]
  int frame_shift_;   // [msec]
  int frames_per_buffer_;
  int num_channels_;
  int max_recording_time_;  // [msec]
  int min_top_silence_;     // [msec]
  int min_end_silence_;     // [msec]
  double silence_level_;
  double normalization_ratio_;

  bool open_;
  bool record_;
  bool playback_;
};

CallPortAudio *port_audio = NULL;

JNIEXPORT void JNICALL Java_jp_ac_nitech_sp_voist_CallPortAudio_setSampleRate(
    JNIEnv *, jobject, jint rate) {
  if (port_audio) {
    port_audio->SetSampleRate(rate);
  }
}

JNIEXPORT void JNICALL Java_jp_ac_nitech_sp_voist_CallPortAudio_setSampleSize(
    JNIEnv *, jobject, jint size) {
  if (port_audio) {
    port_audio->SetSampleSize(size);
  }
}

JNIEXPORT void JNICALL
Java_jp_ac_nitech_sp_voist_CallPortAudio_setSampleRateForBeep(JNIEnv *, jobject,
                                                              jint rate) {
  if (port_audio) {
    port_audio->SetSampleRateForBeep(rate);
  }
}

JNIEXPORT void JNICALL
Java_jp_ac_nitech_sp_voist_CallPortAudio_setSampleSizeForBeep(JNIEnv *, jobject,
                                                              jint size) {
  if (port_audio) {
    port_audio->SetSampleSizeForBeep(size);
  }
}

JNIEXPORT void JNICALL
Java_jp_ac_nitech_sp_voist_CallPortAudio_setSampleRateForSample(JNIEnv *,
                                                                jobject,
                                                                jint rate) {
  if (port_audio) {
    port_audio->SetSampleRateForSample(rate);
  }
}

JNIEXPORT void JNICALL
Java_jp_ac_nitech_sp_voist_CallPortAudio_setSampleSizeForSample(JNIEnv *,
                                                                jobject,
                                                                jint size) {
  if (port_audio) {
    port_audio->SetSampleSizeForSample(size);
  }
}

JNIEXPORT void JNICALL
Java_jp_ac_nitech_sp_voist_CallPortAudio_setMaxRecordingTime(JNIEnv *, jobject,
                                                             jint time) {
  if (port_audio) {
    port_audio->SetMaxRecordingTime(time);
  }
}

JNIEXPORT void JNICALL
Java_jp_ac_nitech_sp_voist_CallPortAudio_setMinTopSilence(JNIEnv *, jobject,
                                                          jint time) {
  if (port_audio) {
    port_audio->SetMinTopSilence(time);
  }
}

JNIEXPORT void JNICALL
Java_jp_ac_nitech_sp_voist_CallPortAudio_setMinEndSilence(JNIEnv *, jobject,
                                                          jint time) {
  if (port_audio) {
    port_audio->SetMinEndSilence(time);
  }
}

JNIEXPORT void JNICALL Java_jp_ac_nitech_sp_voist_CallPortAudio_setSilenceLevel(
    JNIEnv *, jobject, jdouble level) {
  if (port_audio) {
    port_audio->SetSilenceLevel(level);
  }
}

JNIEXPORT void JNICALL
Java_jp_ac_nitech_sp_voist_CallPortAudio_setNormalizationRatio(JNIEnv *,
                                                               jobject,
                                                               jdouble ratio) {
  if (port_audio) {
    port_audio->SetNormalizationRatio(ratio);
  }
}

JNIEXPORT jint JNICALL
Java_jp_ac_nitech_sp_voist_CallPortAudio_getSampleRate(JNIEnv *, jobject) {
  return port_audio ? port_audio->GetSampleRate() : 0;
}

JNIEXPORT jint JNICALL
Java_jp_ac_nitech_sp_voist_CallPortAudio_getSampleSize(JNIEnv *, jobject) {
  return port_audio ? port_audio->GetSampleSize() : 0;
}

JNIEXPORT jint JNICALL
Java_jp_ac_nitech_sp_voist_CallPortAudio_getSampleRateForBeep(JNIEnv *,
                                                              jobject) {
  return port_audio ? port_audio->GetSampleRateForBeep() : 0;
}

JNIEXPORT jint JNICALL
Java_jp_ac_nitech_sp_voist_CallPortAudio_getSampleSizeForBeep(JNIEnv *,
                                                              jobject) {
  return port_audio ? port_audio->GetSampleSizeForBeep() : 0;
}

JNIEXPORT jint JNICALL
Java_jp_ac_nitech_sp_voist_CallPortAudio_getSampleRateForSample(JNIEnv *,
                                                                jobject) {
  return port_audio ? port_audio->GetSampleRateForSample() : 0;
}

JNIEXPORT jint JNICALL
Java_jp_ac_nitech_sp_voist_CallPortAudio_getSampleSizeForSample(JNIEnv *,
                                                                jobject) {
  return port_audio ? port_audio->GetSampleSizeForSample() : 0;
}

JNIEXPORT jint JNICALL
Java_jp_ac_nitech_sp_voist_CallPortAudio_getMaxRecordingTime(JNIEnv *,
                                                             jobject) {
  return port_audio ? port_audio->GetMaxRecordingTime() : 0;
}

JNIEXPORT jint JNICALL
Java_jp_ac_nitech_sp_voist_CallPortAudio_getMinTopSilence(JNIEnv *, jobject) {
  return port_audio ? port_audio->GetMinTopSilence() : 0;
}

JNIEXPORT jint JNICALL
Java_jp_ac_nitech_sp_voist_CallPortAudio_getMinEndSilence(JNIEnv *, jobject) {
  return port_audio ? port_audio->GetMinEndSilence() : 0;
}

JNIEXPORT jdouble JNICALL
Java_jp_ac_nitech_sp_voist_CallPortAudio_getSilenceLevel(JNIEnv *, jobject) {
  return port_audio ? port_audio->GetSilenceLevel() : 0.0;
}

JNIEXPORT jdouble JNICALL
Java_jp_ac_nitech_sp_voist_CallPortAudio_getNormalizationRatio(JNIEnv *,
                                                               jobject) {
  return port_audio ? port_audio->GetNormalizationRatio() : 0.0;
}

JNIEXPORT void JNICALL
Java_jp_ac_nitech_sp_voist_CallPortAudio_createInstance(JNIEnv *, jobject) {
  port_audio = new CallPortAudio();
}

JNIEXPORT jboolean JNICALL
Java_jp_ac_nitech_sp_voist_CallPortAudio_openStream(JNIEnv *env, jobject obj) {
  return port_audio ? port_audio->OpenStream(env, obj) : false;
}

JNIEXPORT void JNICALL
Java_jp_ac_nitech_sp_voist_CallPortAudio_closeStream(JNIEnv *, jobject) {
  if (port_audio) {
    port_audio->CloseStream();
  }
}

JNIEXPORT jboolean JNICALL
Java_jp_ac_nitech_sp_voist_CallPortAudio_isOpen(JNIEnv *, jobject) {
  return port_audio ? port_audio->IsOpen() : false;
}

JNIEXPORT jboolean JNICALL Java_jp_ac_nitech_sp_voist_CallPortAudio_playback(
    JNIEnv *env, jobject obj, jstring file_name, jint event) {
  return port_audio ? port_audio->Playback(env, obj, file_name, event) : false;
}

JNIEXPORT void JNICALL
Java_jp_ac_nitech_sp_voist_CallPortAudio_stopPlayback(JNIEnv *, jobject) {
  if (port_audio) {
    port_audio->StopPlayback();
  }
}

JNIEXPORT void JNICALL
Java_jp_ac_nitech_sp_voist_CallPortAudio_record(JNIEnv *, jobject) {
  if (port_audio) {
    port_audio->Record();
  }
}

JNIEXPORT void JNICALL
Java_jp_ac_nitech_sp_voist_CallPortAudio_stopRecording(JNIEnv *, jobject) {
  if (port_audio) {
    port_audio->StopRecording();
  }
}

JNIEXPORT jboolean JNICALL Java_jp_ac_nitech_sp_voist_CallPortAudio_finalize(
    JNIEnv *env, jobject obj, jstring org_file_name, jstring cut_file_name,
    jboolean environment) {
  return port_audio ? port_audio->Finalize(env, obj, org_file_name,
                                           cut_file_name, environment)
                    : false;
}
