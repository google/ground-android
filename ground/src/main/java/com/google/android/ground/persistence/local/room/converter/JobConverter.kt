/*
 * Copyright 2022 Google LLC
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
package com.google.android.ground.persistence.local.room.converter

import com.google.android.ground.model.job.Job
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.local.LocalDataStoreConverter
import com.google.android.ground.persistence.local.room.entity.JobEntity
import com.google.android.ground.persistence.local.room.relations.JobEntityAndRelations
import com.google.common.collect.ImmutableMap

class JobConverter(val surveyId: String?) : LocalDataStoreConverter<Job, JobEntity> {
  override fun convertToDataStoreObject(job: Job): JobEntity =
    JobEntity(id = job.id, surveyId = surveyId, name = job.name)

  override fun convertFromDataStoreObject(entity: JobEntity): Job =
    Companion.convertFromDataStoreObject(entity)

  companion object {
    fun convertFromDataStoreObject(entity: JobEntity): Job =
      Job(entity.id, entity.name, ImmutableMap.of())

    fun convertFromDataStoreObjectWithRelations(jobEntityAndRelations: JobEntityAndRelations): Job {
      val jobEntity = jobEntityAndRelations.jobEntity
      val taskMap = ImmutableMap.builder<String, Task>()
      for (taskEntityAndRelations in jobEntityAndRelations.taskEntityAndRelations) {
        val task = taskEntityAndRelations.toModelObject()
        taskMap.put(task.id, task)
      }
      return Job(jobEntity.id, jobEntity.name, taskMap.build())
    }
  }
}
