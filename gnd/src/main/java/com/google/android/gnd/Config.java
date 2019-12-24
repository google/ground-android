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

package com.google.android.gnd;

/** Project configurations. */
public final class Config {

  // Local db settings
  // TODO(#128): Reset version to 1 before releasing.
  public static final int DB_VERSION = 32;
  public static final String DB_NAME = "gnd.db";

  // Firebase firestore settings
  public static final boolean FIRESTORE_PERSISTANCE_ENABLED = false;
  public static final boolean FIRESTORE_LOGGING_ENABLED = true;
}
