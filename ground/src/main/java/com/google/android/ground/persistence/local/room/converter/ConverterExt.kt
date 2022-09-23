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

import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Option
import com.google.android.ground.persistence.local.room.entity.MultipleChoiceEntity
import com.google.android.ground.persistence.local.room.entity.OptionEntity
import com.google.android.ground.persistence.local.room.models.MultipleChoiceEntityType
import com.google.common.collect.ImmutableList
import kotlinx.collections.immutable.toPersistentList

fun MultipleChoiceEntity.toMultipleChoice(optionEntities: List<OptionEntity>): MultipleChoice {
  val listBuilder = ImmutableList.builder<Option>()

  for (optionEntity in optionEntities) {
    listBuilder.add(OptionEntity.toOption(optionEntity))
  }

  return MultipleChoice(listBuilder.build().toPersistentList(), this.type.toCardinality())
}

fun MultipleChoice.toMultipleChoiceEntity(taskId: String): MultipleChoiceEntity =
  MultipleChoiceEntity(taskId, MultipleChoiceEntityType.fromCardinality(this.cardinality))
