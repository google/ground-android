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

package org.groundplatform.android.persistence.remote.firebase.schema

import kotlinx.collections.immutable.toPersistentList
import org.groundplatform.android.model.task.MultipleChoice
import org.groundplatform.android.model.task.Option
import org.groundplatform.android.proto.Task.MultipleChoiceQuestion

internal object MultipleChoiceConverter {
  fun toMultipleChoice(em: MultipleChoiceQuestion): MultipleChoice {
    var options: List<Option> = listOf()
    if (em.optionsList != null) {
      options = em.optionsList.sortedBy { it.index }.map { OptionConverter.toOption(it) }
    }
    val cardinality =
      when (em.type) {
        MultipleChoiceQuestion.Type.SELECT_ONE -> MultipleChoice.Cardinality.SELECT_ONE
        MultipleChoiceQuestion.Type.SELECT_MULTIPLE -> MultipleChoice.Cardinality.SELECT_MULTIPLE
        else -> MultipleChoice.Cardinality.SELECT_ONE
      }
    return MultipleChoice(options.toPersistentList(), cardinality, em.hasOtherOption)
  }
}
