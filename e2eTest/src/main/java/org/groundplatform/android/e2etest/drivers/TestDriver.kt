/*
 * Copyright 2025 Google LLC
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
package org.groundplatform.android.e2etest.drivers

interface TestDriver {
  fun click(target: Target)

  fun selectFromList(target: Target, index: Int)

  fun dragMapBy(offsetX: Int, offsetY: Int)

  fun clickMapMarker(description: String)

  fun scrollTo(target: Target)

  fun insertText(text: String, target: Target)

  fun takePhoto()

  fun setDate()

  fun setTime()

  fun getStringResource(id: Int): String

  sealed class Target {
    data class TestTag(val tag: String) : Target()

    data class ViewId(val resId: Int) : Target()

    data class Text(val text: String, val substring: Boolean = false) : Target()

    data class ContentDescription(val text: String) : Target()
  }
}
