/*
 * Copyright 2019 Google LLC
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

import android.content.Context

/** Application configuration. */
object Config {
  // Shared preferences.
  const val SHARED_PREFS_NAME = "shared_prefs"
  const val SHARED_PREFS_MODE = Context.MODE_PRIVATE

  // Local db settings.
  // TODO(#128): Reset version to 1 before releasing.
  const val DB_VERSION = 95
  const val DB_NAME = "ground.db"

  // Firebase Cloud Firestore settings.
  const val FIRESTORE_PERSISTENCE_ENABLED = false
  const val FIRESTORE_LOGGING_ENABLED = true

  // Photos
  const val PHOTO_EXT = ".jpg"
}
