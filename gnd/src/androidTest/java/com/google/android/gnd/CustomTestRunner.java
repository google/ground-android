/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gnd;

import android.app.Application;
import android.app.UiAutomation;
import android.content.Context;
import android.os.Bundle;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnitRunner;
import com.squareup.rx2.idler.Rx2Idler;
import dagger.hilt.android.testing.HiltTestApplication;
import io.reactivex.plugins.RxJavaPlugins;
import timber.log.Timber;
import timber.log.Timber.DebugTree;

public class CustomTestRunner extends AndroidJUnitRunner {

  @Override
  public Application newApplication(ClassLoader cl, String className,
      Context context)
      throws ClassNotFoundException, IllegalAccessException, InstantiationException {

    Timber.plant(new DebugTree());
    RxJavaPlugins.setInitComputationSchedulerHandler(
        Rx2Idler.create("RxJava 2.x Computation Scheduler"));
    RxJavaPlugins.setInitIoSchedulerHandler(
        Rx2Idler.create("RxJava 2.x IO Scheduler"));

    return super.newApplication(cl, HiltTestApplication.class.getName(), context);
  }

  @Override
  public void onCreate(Bundle arguments) {
    super.onCreate(arguments);
    setAnimations(false);
  }

  @Override
  public void finish(int resultCode, Bundle results) {
    setAnimations(true);
    super.finish(resultCode, results);
  }

  private void setAnimations(boolean enabled) {
    String value = enabled ? "1.0" : "0.0";
    UiAutomation run = InstrumentationRegistry.getInstrumentation().getUiAutomation();
    run.executeShellCommand("settings put global $WINDOW_ANIMATION_SCALE " + value);
    run.executeShellCommand("settings put global $TRANSITION_ANIMATION_SCALE " + value);
    run.executeShellCommand("settings put global $ANIMATOR_DURATION_SCALE " + value);
  }
}