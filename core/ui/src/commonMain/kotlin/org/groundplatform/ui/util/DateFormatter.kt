/*
 * Copyright 2026 Google LLC
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
package org.groundplatform.ui.util

/**
 * Locale-aware date and time formatting. Implemented as an interface so it can be injected in tests
 * and provided per platform.
 */
interface DateFormatter {

  /** Formats just the date portion of [millis] in the user's locale (medium style). */
  fun formatDate(millis: Long): String

  /** Formats just the time portion of [millis] in the user's locale (short style, no seconds). */
  fun formatTime(millis: Long): String
}
