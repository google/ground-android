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
package org.groundplatform.android.data.local.room.relations

import androidx.room.Embedded
import androidx.room.Relation
import org.groundplatform.android.data.local.room.entity.JobEntity
import org.groundplatform.android.data.local.room.entity.TaskEntity

/**
 * Represents relationship between JobEntity and TaskEntity.
 *
 * Querying any of the below data class automatically loads the field annotated as @Relation.
 */
data class JobEntityAndRelations(
  @Embedded val jobEntity: JobEntity,
  @Relation(parentColumn = "id", entityColumn = "job_id", entity = TaskEntity::class)
  val taskEntityAndRelations: List<TaskEntityAndRelations>,
)
