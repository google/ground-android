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
package com.google.android.ground.util

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.ground.FirebaseCrashLogger
import com.google.firebase.FirebaseNetworkException
import timber.log.Timber

object Debug {
  fun logLifecycleEvent(instance: Any) {
    val stackTrace = Thread.currentThread().stackTrace
    val callingMethod = stackTrace[3].methodName + "()"
    val className = instance.javaClass.simpleName
    Timber.tag(className).v("Lifecycle event: $callingMethod")

    if ((instance is Fragment || instance is FragmentActivity) && callingMethod == "onResume()") {
      FirebaseCrashLogger().setScreenName(className)
    }
  }

  fun <T> logOnFailure(fn: () -> T): T? =
    try {
      fn()
    } catch (e: RuntimeException) {
      Timber.d(e.message)
      null
    }
}

fun Throwable.priority() = if (this is FirebaseNetworkException) Log.DEBUG else Log.ERROR
