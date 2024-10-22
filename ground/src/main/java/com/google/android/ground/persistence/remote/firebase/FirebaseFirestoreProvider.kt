/*
 * Copyright 2023 Google LLC
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

package com.google.android.ground.persistence.remote.firebase

import com.google.android.ground.Config
import com.google.android.ground.util.AsyncSingletonProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class FirebaseFirestoreProvider @Inject constructor(settings: FirebaseFirestoreSettings) :
  AsyncSingletonProvider<FirebaseFirestore>({
    FirebaseFirestore.getInstance().also {
      if (it.firestoreSettings == settings) {
        FirebaseFirestore.setLoggingEnabled(Config.FIRESTORE_LOGGING_ENABLED)
        return@also
      }
      try {
        it.firestoreSettings = settings
      } catch (e: IllegalStateException) {
        // Logging added for #2377.
        Timber.e(e, "Singleton provider likely initialized multiple times")
      }
      FirebaseFirestore.setLoggingEnabled(Config.FIRESTORE_LOGGING_ENABLED)
    }
  })
