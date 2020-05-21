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

package com.google.android.gnd.ui.settings;

/** These keys should match 1:1 in res/xml/preferences.xml. */
final class Keys {

  // General
  public static final String UPLOAD_MEDIA = "upload_media";
  public static final String OFFLINE_AREAS = "offline_areas";

  // Help
  public static final String VISIT_WEBSITE = "visit_website";
  public static final String FEEDBACK = "feedback";

  public static final String[] ALL_KEYS = {UPLOAD_MEDIA, OFFLINE_AREAS, VISIT_WEBSITE, FEEDBACK};
}
