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
package org.groundplatform.android.ui.datacollection

/** Represents user actions associated with naming a LOI. */
sealed class LoiNameAction {
  /** Indicates that the user has confirmed the name for the LOI. */
  data class Confirmed(val name: String) : LoiNameAction()

  /** Action triggered when the user dismisses the LOI name dialog without saving changes. */
  object Dismissed : LoiNameAction()

  /** Action triggered when the name of the LOI is modified. */
  data class Changed(val name: String) : LoiNameAction()
}
