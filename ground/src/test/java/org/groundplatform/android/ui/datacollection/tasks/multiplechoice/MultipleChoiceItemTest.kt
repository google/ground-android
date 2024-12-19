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
package org.groundplatform.android.ui.datacollection.tasks.multiplechoice

import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.model.task.MultipleChoice.Cardinality
import org.groundplatform.android.model.task.Option
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class MultipleChoiceItemTest : BaseHiltTest() {

  private val multipleChoiceItemOne =
    MultipleChoiceItem(
      option = Option(id = "id", code = "code", label = "label"),
      cardinality = Cardinality.SELECT_ONE,
    )

  private val multipleChoiceItemMultiple =
    MultipleChoiceItem(
      option = Option(id = "id", code = "code", label = "label"),
      cardinality = Cardinality.SELECT_MULTIPLE,
    )

  private val multipleChoiceItemOneId =
    MultipleChoiceItem(
      option = Option(id = "new id", code = "code", label = "label"),
      isSelected = true,
      cardinality = Cardinality.SELECT_MULTIPLE,
    )

  @Test
  fun `isTheSameItem returns true when IDs are the same`() {
    assertThat(multipleChoiceItemOne.isTheSameItem(multipleChoiceItemMultiple)).isEqualTo(true)
  }

  @Test
  fun `isTheSameItem returns false when IDs are not same`() {
    assertThat(multipleChoiceItemOne.isTheSameItem(multipleChoiceItemOneId)).isEqualTo(false)
  }

  @Test
  fun `areContentsTheSame returns true when isSelected is same`() {
    assertThat(multipleChoiceItemOne.areContentsTheSame(multipleChoiceItemMultiple)).isEqualTo(true)
  }

  @Test
  fun `areContentsTheSame returns false when isSelected is not same`() {
    assertThat(multipleChoiceItemOne.areContentsTheSame(multipleChoiceItemOneId)).isEqualTo(false)
  }
}
