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
package org.groundplatform.android.ui.datacollection.tasks

/** Represents high-level UI events triggered by task ViewModels during data collection. */
sealed interface DataCollectionEvent {
  /** Navigate to the previous task. */
  data object NavigatePrevious : DataCollectionEvent

  /** Navigate to the next task (includes validation and submission check). */
  data object NavigateNext : DataCollectionEvent

  /** Show the LOI Name Dialog before proceeding. */
  data object ShowLoiDialog : DataCollectionEvent
}
