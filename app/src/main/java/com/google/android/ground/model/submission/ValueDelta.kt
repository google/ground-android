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
package com.google.android.ground.model.submission

import com.google.android.ground.model.task.Task

/**
 * Represents a change to an individual value in a submission.
 *
 * @property taskId the id of the task task being updated.
 * @property taskType the type of task being updated.
 * @property newTaskData the new value of the value, or empty if removed.
 */
data class ValueDelta(val taskId: String, val taskType: Task.Type, val newTaskData: TaskData?)
