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

@Singleton
class FirebaseFirestoreProvider @Inject constructor(settings: FirebaseFirestoreSettings) :
  AsyncSingletonProvider<FirebaseFirestore>({
    // WARNING: `FirebaseFirestore.getInstance()` should only be called here and nowhere
    // else since settings can only be set on first call. Inject FirebaseFirestore instead
    // of calling `getInstance()` directly.
    FirebaseFirestore.getInstance().apply {
      setFirestoreSettings(this, settings)
      FirebaseFirestore.setLoggingEnabled(Config.FIRESTORE_LOGGING_ENABLED)
    }
  })

private fun setFirestoreSettings(
  firestore: FirebaseFirestore,
  settings: FirebaseFirestoreSettings,
) {
  if (firestore.firestoreSettings == settings) {
    return
  }
  firestore.firestoreSettings = settings
}
