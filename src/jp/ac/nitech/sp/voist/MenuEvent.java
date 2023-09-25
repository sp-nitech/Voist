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

import java.util.EventObject;

public class MenuEvent extends EventObject {
  private static final long serialVersionUID = 1L;

  private String param;

  public MenuEvent(Object source) {
    super(source);
  }

  public MenuEvent(Object source, String param) {
    super(source);
    this.param = param;
  }

  public void setParam(String param) {
    this.param = param;
  }

  public String getParam() {
    return param;
  }

  public Integer getParamInteger() {
    return Integer.parseInt(param);
  }
}
