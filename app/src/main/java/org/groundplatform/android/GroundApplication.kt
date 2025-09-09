/*
 * Copyright 2018 Google LLC
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

import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.multidex.MultiDexApplication
import androidx.work.Configuration
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import org.groundplatform.android.common.Constants.isReleaseBuild
import org.groundplatform.android.data.local.LocalValueStore
import timber.log.Timber

@HiltAndroidApp
class GroundApplication : MultiDexApplication(), Configuration.Provider {

  @Inject lateinit var crashReportingTree: CrashReportingTree
  @Inject lateinit var workerFactory: HiltWorkerFactory
  @Inject lateinit var localValueStore: LocalValueStore
  @Inject lateinit var remoteConfig: FirebaseRemoteConfig

  override val workManagerConfiguration: Configuration
    get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

  override fun onCreate() {
    super.onCreate()
    Timber.plant(if (isReleaseBuild()) crashReportingTree else Timber.DebugTree())
    if (!isReleaseBuild()) {
      Timber.d("DEBUG build config active; enabling debug tooling")

      // Log failures when trying to do work in the UI thread.
      setStrictMode()
    }
    val selectedLanguage = localValueStore.selectedLanguage
    val appLocale = LocaleListCompat.forLanguageTags(selectedLanguage)
    AppCompatDelegate.setApplicationLocales(appLocale)
    initiateRemoteConfig()
  }

  private fun setStrictMode() {
    // NOTE: Enabling strict thread policy causes Maps SDK to lag on pan and zoom. Enable
    // only as needed when debugging.
    // https://github.com/google/ground-android/issues/1758#issuecomment-1720243538
    // StrictMode.setThreadPolicy(ThreadPolicy.Builder().detectAll().penaltyLog().build())
    StrictMode.setVmPolicy(VmPolicy.Builder().detectLeakedSqlLiteObjects().penaltyLog().build())
  }

  /**
   * Initializes Firebase Remote Config by setting default values from the provided XML file and
   * fetching remote values to activate them for use in the app.
   *
   * This method:
   * - Sets default values using `firebase_remote_config_defaults.xml`
   * - Asynchronously fetches the latest Remote Config values from Firebase
   * - Immediately activates the fetched values
   *
   * Call this during app startup to ensure Remote Config values are available.
   */
  private fun initiateRemoteConfig() {
    remoteConfig.setDefaultsAsync(R.xml.firebase_remote_config_defaults)
    remoteConfig.fetchAndActivate()
  }

  /** Reports any error with priority more than "info" to Crashlytics. */
  class CrashReportingTree
  @Inject
  constructor(private val firebaseCrashLogger: FirebaseCrashLogger) : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
      if (priority > Log.INFO) {
        firebaseCrashLogger.recordException(priority, message, t)
      }
    }
  }
}
