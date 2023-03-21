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

package com.google.android.ground.util

import android.os.StrictMode

/**
 * Disables StrictMode warnings for on-thread disk reads made from the provided block.
 *
 * **IMPORTANT**: Use with caution! This should only be used to execute code which is guaranteed to
 * execute small, quick reads.
 */
inline fun <T> allowThreadDiskReads(block: () -> T): T {
  val oldPolicy = StrictMode.allowThreadDiskReads()
  val returnValue = block()
  StrictMode.setThreadPolicy(oldPolicy)
  return returnValue
}

/**
 * Disables StrictMode warnings for on-thread disk writes made from the provided block.
 *
 * **IMPORTANT**: Use with caution! This should only be used to execute code which is guaranteed to
 * execute small, quick writes.
 */
inline fun <T> allowThreadDiskWrites(block: () -> T): T {
  val oldPolicy = StrictMode.allowThreadDiskWrites()
  val returnValue = block()
  StrictMode.setThreadPolicy(oldPolicy)
  return returnValue
}
