/*
 * Copyright 2024 Google LLC
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
package org.groundplatform.android.e2etest

import org.groundplatform.android.model.task.Task

object TestConfig {
  const val LONG_TIMEOUT = 30000L
  const val SHORT_TIMEOUT = 10000L
  const val GROUND_PACKAGE = "org.groundplatform.android"
  val TEST_SURVEY_TASKS_ADHOC =
    listOf(
      Task.Type.CAPTURE_LOCATION,
      Task.Type.DROP_PIN,
      Task.Type.TEXT,
      Task.Type.MULTIPLE_CHOICE,
      Task.Type.MULTIPLE_CHOICE,
      Task.Type.NUMBER,
      Task.Type.DATE,
      Task.Type.TIME,
      Task.Type.PHOTO,
      Task.Type.CAPTURE_LOCATION,
    )
  val TEST_SURVEY_LOI_TASK_INDEX = 1
  const val TEST_SURVEY_IDENTIFIER = "test"
}
