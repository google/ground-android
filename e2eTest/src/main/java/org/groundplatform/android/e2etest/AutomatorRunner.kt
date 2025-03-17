/*
 * Copyright 2024 Google LLC
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
package org.groundplatform.android.e2etest

import android.content.Context
import android.content.Intent
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlin.reflect.KClass
import org.groundplatform.android.e2etest.TestConfig.LONG_TIMEOUT
import org.groundplatform.android.e2etest.TestConfig.SHORT_TIMEOUT

interface AutomatorRunner {
  var device: UiDevice

  fun stringResource(
    @StringRes resId: Int,
    context: Context = InstrumentationRegistry.getInstrumentation().targetContext,
  ): String = context.getString(resId)

  fun byText(@StringRes resId: Int): BySelector = By.text(stringResource(resId))

  fun <T : Any> byClass(kclass: KClass<T>): BySelector = By.clazz(kclass.java.name)

  fun waitClickGone(selector: BySelector, timeout: Long = SHORT_TIMEOUT): Boolean {
    device.wait(Until.hasObject(selector), timeout)
    device.findObject(selector)?.click()
    return device.wait(Until.gone(selector), timeout)
  }

  fun hasTextField() = device.hasObject(byClass(EditText::class))

  fun enterText(text: String) {
    val textSelector = byClass(EditText::class)
    device.wait(Until.hasObject(textSelector), SHORT_TIMEOUT)
    device.findObject(textSelector).text = text
  }

  fun allowPermissions() {
    waitClickGone(By.textContains("While using the app"))
  }

  fun launchPackage(packageName: String) {
    // Start from the home screen
    device.pressHome()

    // Wait for launcher
    val launcherPackage: String = device.launcherPackageName
    device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LONG_TIMEOUT)

    // Launch the app
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
      context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
        // Clear out any previous instances
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
      }
    context.startActivity(intent)
  }
}
