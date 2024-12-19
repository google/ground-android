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
package org.groundplatform.android

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication
import timber.log.Timber

class CustomTestRunner : AndroidJUnitRunner() {
  @Throws(
    ClassNotFoundException::class,
    IllegalAccessException::class,
    InstantiationException::class,
  )
  override fun newApplication(cl: ClassLoader, className: String, context: Context): Application {
    Timber.plant(Timber.DebugTree())
    return super.newApplication(cl, HiltTestApplication::class.java.getName(), context)
  }

  override fun onCreate(arguments: Bundle) {
    super.onCreate(arguments)
    setAnimations(false)
  }

  override fun finish(resultCode: Int, results: Bundle) {
    setAnimations(true)
    super.finish(resultCode, results)
  }

  private fun setAnimations(enabled: Boolean) {
    val value = if (enabled) "1.0" else "0.0"
    val run = InstrumentationRegistry.getInstrumentation().uiAutomation
    run.executeShellCommand("settings put global \$WINDOW_ANIMATION_SCALE $value")
    run.executeShellCommand("settings put global \$TRANSITION_ANIMATION_SCALE $value")
    run.executeShellCommand("settings put global \$ANIMATOR_DURATION_SCALE $value")
  }
}
