package com.google.android.gnd;

import android.app.Application;
import android.content.Context;
import androidx.test.runner.AndroidJUnitRunner;

public class CustomTestRunner extends AndroidJUnitRunner {

  @Override
  public Application newApplication(ClassLoader cl, String className, Context context)
      throws InstantiationException, IllegalAccessException, ClassNotFoundException {
    return super.newApplication(cl, TestGndApplication.class.getName(), context);
  }
}