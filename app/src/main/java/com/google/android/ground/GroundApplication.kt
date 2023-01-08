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
package com.google.android.ground

import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.multidex.MultiDexApplication
import androidx.work.Configuration
import androidx.work.WorkManager
import com.akaita.java.rxjava2debug.RxJava2Debug
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import timber.log.Timber

@HiltAndroidApp
class GroundApplication : MultiDexApplication(), Configuration.Provider {

  @Inject lateinit var workerFactory: HiltWorkerFactory

  init {
    Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else CrashReportingTree())
  }

  override fun onCreate() {
    super.onCreate()
    if (BuildConfig.DEBUG) {
      Timber.d("DEBUG build config active; enabling debug tooling")

      // Log failures when trying to do work in the UI thread.
      setStrictMode()
    }

    // Enable RxJava assembly stack collection for more useful stack traces.
    RxJava2Debug.enableRxJava2AssemblyTracking(arrayOf(javaClass.getPackage().name))

    WorkManager.initialize(applicationContext, workManagerConfiguration)
  }

  override fun getWorkManagerConfiguration(): Configuration {
    return Configuration.Builder().setWorkerFactory(workerFactory).build()
  }

  private fun setStrictMode() {
    StrictMode.setThreadPolicy(ThreadPolicy.Builder().detectAll().penaltyLog().build())
    StrictMode.setVmPolicy(VmPolicy.Builder().detectLeakedSqlLiteObjects().penaltyLog().build())
  }

  /** Reports any error with priority more than "info" to Crashlytics. */
  private class CrashReportingTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
      if (priority > Log.INFO) {
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.log(message)
        if (t != null && priority == Log.ERROR) crashlytics.recordException(t)
      }
    }
  }
}
