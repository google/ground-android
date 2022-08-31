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
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.local.room.relations.JobEntityAndRelations
import com.google.common.collect.ImmutableMap

@Entity(
  tableName = "job",
  foreignKeys =
    [
      ForeignKey(
        entity = SurveyEntity::class,
        parentColumns = ["id"],
        childColumns = ["survey_id"],
        onDelete = ForeignKey.CASCADE
      )
    ],
  indices = [Index("survey_id")]
)
data class JobEntity(
  @PrimaryKey @ColumnInfo(name = "id") val id: String,
  @ColumnInfo(name = "name") val name: String?,
  @ColumnInfo(name = "survey_id") val surveyId: String?
) {

  companion object {
    fun fromJob(surveyId: String?, job: Job): JobEntity {
      return JobEntity(id = job.id, surveyId = surveyId, name = job.name)
    }

    @JvmStatic
    fun toJob(jobEntityAndRelations: JobEntityAndRelations): Job {
      val jobEntity = jobEntityAndRelations.jobEntity
      val taskMap = ImmutableMap.builder<String, Task>()
      for (taskEntityAndRelations in jobEntityAndRelations.taskEntityAndRelations) {
        val task = TaskEntity.toTask(taskEntityAndRelations)
        taskMap.put(task.id, task)
      }
      return Job(jobEntity.id, jobEntity.name, taskMap.build())
    }
  }
}
