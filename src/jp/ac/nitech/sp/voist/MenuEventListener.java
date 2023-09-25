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

import java.util.EventListener;

public interface MenuEventListener extends EventListener {
  public void changePromptSet(MenuEvent e);

  public void changeUser(MenuEvent e);

  public void exit(MenuEvent e);

  public void startGuidance(MenuEvent e);

  public void skipRecordedPromptsPrev(MenuEvent e);

  public void skipRecordedPromptsNext(MenuEvent e);

  public void uploadVoice(MenuEvent e);

  public void showRecordLog(MenuEvent e);

  public void showRecordWaveform(MenuEvent e);

  public void showRecordDir(MenuEvent e);

  public void showVersion(MenuEvent e);

  public void showOpenJTalk(MenuEvent e);

  public void changeConfigAutoSamplePlayback(MenuEvent e);

  public void changeConfigAutoVoicePlayback(MenuEvent e);

  public void changeConfigBeepType(MenuEvent e);

  public void changeConfigMaxAmplitudeRejection(MenuEvent e);

  public void changeConfigMinAmplitudeRejection(MenuEvent e);

  public void changeConfigVadLevel(MenuEvent e);

  public void changeConfigPromptRubyType(MenuEvent e);

  public void changeConfigPromptFontType(MenuEvent e);

  public void changeConfigPromptUpperBoundFontSize(MenuEvent e);

  public void changeConfigPromptLowerBoundFontSize(MenuEvent e);

  public void changeConfigPromptLineBreakAtPP(MenuEvent e);

  public void changeConfigGuidanceFontSize(MenuEvent e);

  public void changeConfigStartupGuidance(MenuEvent e);

  public void changeShortcutMouseWheel(MenuEvent e);

  public void changeShortcutPlaybackSample(MenuEvent e);

  public void changeShortcutPlaybackVoice(MenuEvent e);

  public void changeShortcutPlaybackVoiceTake2(MenuEvent e);

  public void changeShortcutRecord(MenuEvent e);
}
