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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.LayoutCoordinates

/** State holder for the [TaskScreen] composable. */
@Stable
class TaskScreenState(initialLoiName: String) {
  var layoutCoordinates by mutableStateOf<LayoutCoordinates?>(null)

  var loiName by mutableStateOf(initialLoiName)

  companion object {
    fun saver(): Saver<TaskScreenState, String> =
      Saver(save = { it.loiName }, restore = { TaskScreenState(it) })
  }
}

@Composable
fun rememberTaskScreenState(initialLoiName: String): TaskScreenState {
  return rememberSaveable(saver = TaskScreenState.saver()) { TaskScreenState(initialLoiName) }
}
