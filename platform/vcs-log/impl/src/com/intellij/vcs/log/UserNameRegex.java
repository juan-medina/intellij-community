/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.vcs.log;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.Function;

public class UserNameRegex implements Function<String, String> {
  public static final UserNameRegex INSTANCE = new UserNameRegex();

  private UserNameRegex(){}

  @Override
  public String fun(String s) {
    if (SystemInfo.isMac) {
      return "^" + s + ".*";// tmp fix for mac
    }
    return "^" +
           s +
           "( <.*>)?$|^<" +
           s +
           "@.*>$|^" +
           s +
           "@.*$"; // either exact user name with any email or no name with exact email (on any domain)
  }
}
