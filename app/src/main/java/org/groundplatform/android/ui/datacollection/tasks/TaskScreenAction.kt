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

import org.groundplatform.android.ui.datacollection.components.ButtonAction

/**
 * Defines the set of actions that can be triggered from a task-specific screen in the data
 * collection flow.
 */
sealed interface TaskScreenAction {
  data class OnButtonClicked(val action: ButtonAction) : TaskScreenAction

  data class OnLoiNameConfirm(val name: String) : TaskScreenAction

  data object OnLoiNameDismiss : TaskScreenAction

  data class OnLoiNameChanged(val name: String) : TaskScreenAction

  data object OnInstructionsDismiss : TaskScreenAction
}