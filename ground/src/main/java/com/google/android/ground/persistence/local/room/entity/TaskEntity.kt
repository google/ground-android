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
package com.google.android.ground.persistence.local.room.entity

import androidx.room.*
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.local.room.converter.toMultipleChoice
import com.google.android.ground.persistence.local.room.models.TaskEntityType
import com.google.android.ground.persistence.local.room.relations.TaskEntityAndRelations
import timber.log.Timber

@Entity(
  tableName = "task",
  foreignKeys =
    [
      ForeignKey(
        entity = JobEntity::class,
        parentColumns = ["id"],
        childColumns = ["job_id"],
        onDelete = ForeignKey.CASCADE
      )],
  indices = [Index("job_id")]
)
data class TaskEntity(
  @ColumnInfo(name = "id") @PrimaryKey val id: String,
  @ColumnInfo(name = "index") val index: Int,
  @ColumnInfo(name = "task_type") val taskType: TaskEntityType,
  @ColumnInfo(name = "label") val label: String?,
  @ColumnInfo(name = "is_required") val isRequired: Boolean,
  @ColumnInfo(name = "job_id") val jobId: String?
) {

  companion object {
    fun fromTask(jobId: String?, task: Task): TaskEntity =
      TaskEntity(
        id = task.id,
        jobId = jobId,
        index = task.index,
        label = task.label,
        isRequired = task.isRequired,
        taskType = TaskEntityType.fromTaskType(task.type)
      )

    fun toTask(taskEntityAndRelations: TaskEntityAndRelations): Task {
      val taskEntity = taskEntityAndRelations.taskEntity
      val multipleChoiceEntities = taskEntityAndRelations.multipleChoiceEntities
      var multipleChoice: MultipleChoice? = null
      if (multipleChoiceEntities.isNotEmpty()) {
        if (multipleChoiceEntities.size > 1) {
          Timber.e("More than 1 multiple choice found for task")
        }
        multipleChoice =
          multipleChoiceEntities[0].toMultipleChoice(taskEntityAndRelations.optionEntities)
      }
      return Task(
        taskEntity.id,
        taskEntity.index,
        taskEntity.taskType.toTaskType(),
        taskEntity.label!!,
        taskEntity.isRequired,
        multipleChoice
      )
    }
  }
}
