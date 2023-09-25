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

public interface ButtonEventListener extends EventListener {
  public void goPrevPrompt(ButtonEvent e);

  public void goPrevGuidanceStep(ButtonEvent e);

  public void goNextPrompt(ButtonEvent e);

  public void goNextGuidanceStep(ButtonEvent e);

  public void playbackSample(ButtonEvent e);

  public void playbackVoice(ButtonEvent e);

  public void playbackVoiceTake2(ButtonEvent e);

  public void record(ButtonEvent e);

  public void overwriteSave(ButtonEvent e);

  public void cancelSave(ButtonEvent e);
}
