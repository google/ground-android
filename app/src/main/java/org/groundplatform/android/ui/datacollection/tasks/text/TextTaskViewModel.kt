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
package org.groundplatform.android.ui.datacollection.tasks.text

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import javax.inject.Inject
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import org.groundplatform.android.Config
import org.groundplatform.android.R
import org.groundplatform.android.model.submission.TaskData
import org.groundplatform.android.model.submission.TextTaskData
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskViewModel

class TextTaskViewModel @Inject constructor() : AbstractTaskViewModel() {

  /** Transcoded text to be displayed for the current [TaskData]. */
  val responseText: LiveData<String> =
    taskTaskData.filterIsInstance<TextTaskData?>().map { it?.text ?: "" }.asLiveData()

  override fun validate(task: Task, taskData: TaskData?): Int? {
    if (task.type != Task.Type.TEXT) return super.validate(task, taskData)

    if ((taskData as TextTaskData).text.length > Config.TEXT_DATA_CHAR_LIMIT)
      return R.string.text_task_data_character_limit

    return super.validate(task, taskData)
  }
}
