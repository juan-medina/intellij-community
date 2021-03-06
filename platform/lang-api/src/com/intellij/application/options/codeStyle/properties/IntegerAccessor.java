// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.codeStyle.properties;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

public class IntegerAccessor extends CodeStylePropertyAccessor<Integer,Integer> {
  IntegerAccessor(@NotNull Object object, @NotNull Field field) {
    super(object, field);
  }

  @Nullable
  @Override
  protected Integer parseString(@NotNull String string) {
    try {
      return Integer.parseInt(string);
    }
    catch (NumberFormatException nfe) {
      return null;
    }
  }

  @Override
  protected Integer fromExternal(@NotNull Integer i) {
    return i;
  }

  @NotNull
  @Override
  protected Integer toExternal(@NotNull Integer value) {
    return value;
  }

  @Override
  protected boolean isEmpty(@NotNull Integer value) {
    return value < 0;
  }
}

