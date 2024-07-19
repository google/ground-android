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

package com.google.android.ground.persistence.remote.firebase.schema

import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Task as TaskModel
import com.google.android.ground.model.task.Task.Type
import com.google.android.ground.proto.Task
import com.google.android.ground.proto.Task.DrawGeometry.Method
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class TaskConverterTest(
  private val testLabel: String,
  private val taskTypeNestedObjectLabel: String,
  private val protoBuilderLambda: (Task.Builder) -> Task.Builder,
  private val taskType: Type,
  private val multipleChoice: MultipleChoice?,
  private val isLoiTask: Boolean,
) {

  @Test
  fun `Converts to Task from TaskNestedObject`() {
    val taskNestedObject =
      TaskNestedObject(
        index = 1,
        type = taskTypeNestedObjectLabel,
        label = "task label",
        required = true,
        cardinality = multipleChoice?.cardinality.toString(),
        addLoiTask = isLoiTask,
      )
    assertThat(TaskConverter.toTask(TASK_ID, taskNestedObject))
      .isEqualTo(
        TaskModel(
          id = TASK_ID,
          index = 1,
          type = taskType,
          label = "task label",
          isRequired = true,
          multipleChoice = multipleChoice,
          isAddLoiTask = isLoiTask,
        )
      )
  }

  @Test
  fun `Converts to Task from Task proto`() {
    val taskProto =
      Task.newBuilder().setId(TASK_ID).setIndex(1).setPrompt("task label").setRequired(true).let {
        protoBuilderLambda(it).build()
      }
    assertThat(TaskConverter.toTask(taskProto))
      .isEqualTo(
        TaskModel(
          id = TASK_ID,
          index = 1,
          type = taskType,
          label = "task label",
          isRequired = true,
          multipleChoice = multipleChoice,
          isAddLoiTask = isLoiTask,
        )
      )
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data() =
      listOf(
        testCase(
          testLabel = "text",
          taskTypeNestedObjectLabel = "text_field",
          protoBuilderLambda = { taskBuilder: Task.Builder ->
            taskBuilder.setTextQuestion(
              Task.TextQuestion.newBuilder().setType(Task.TextQuestion.Type.SHORT_TEXT)
            )
          },
          taskType = Type.TEXT,
        ),
        testCase(
          testLabel = "number",
          taskTypeNestedObjectLabel = "number",
          protoBuilderLambda = { taskBuilder: Task.Builder ->
            taskBuilder.setNumberQuestion(
              Task.NumberQuestion.newBuilder().setType(Task.NumberQuestion.Type.FLOAT)
            )
          },
          taskType = Type.NUMBER,
        ),
        testCase(
          testLabel = "date",
          taskTypeNestedObjectLabel = "date",
          protoBuilderLambda = { taskBuilder: Task.Builder ->
            taskBuilder.setDateTimeQuestion(
              Task.DateTimeQuestion.newBuilder().setType(Task.DateTimeQuestion.Type.DATE_ONLY)
            )
          },
          taskType = Type.DATE,
        ),
        testCase(
          testLabel = "time",
          taskTypeNestedObjectLabel = "date_time",
          protoBuilderLambda = { taskBuilder: Task.Builder ->
            taskBuilder.setDateTimeQuestion(
              Task.DateTimeQuestion.newBuilder().setType(Task.DateTimeQuestion.Type.TIME_ONLY)
            )
          },
          taskType = Type.TIME,
        ),
        // Defaults to date when set to BOTH_DATE_AND_TIME
        testCase(
          testLabel = "both_date_and_time",
          taskTypeNestedObjectLabel = "date",
          protoBuilderLambda = { taskBuilder: Task.Builder ->
            taskBuilder.setDateTimeQuestion(
              Task.DateTimeQuestion.newBuilder()
                .setType(Task.DateTimeQuestion.Type.BOTH_DATE_AND_TIME)
            )
          },
          taskType = Type.DATE,
        ),
        testCase(
          testLabel = "multiple_choice",
          taskTypeNestedObjectLabel = "multiple_choice",
          protoBuilderLambda = { taskBuilder: Task.Builder ->
            taskBuilder.setMultipleChoiceQuestion(
              Task.MultipleChoiceQuestion.newBuilder()
                .setType(Task.MultipleChoiceQuestion.Type.SELECT_ONE)
            )
          },
          taskType = Type.MULTIPLE_CHOICE,
          multipleChoice =
            MultipleChoice(
              options = persistentListOf(),
              cardinality = MultipleChoice.Cardinality.SELECT_ONE,
              hasOtherOption = false,
            ),
        ),
        testCase(
          testLabel = "draw_area",
          taskTypeNestedObjectLabel = "draw_area",
          protoBuilderLambda = { taskBuilder: Task.Builder ->
            taskBuilder
              .setDrawGeometry(
                Task.DrawGeometry.newBuilder().addAllAllowedMethods(listOf(Method.DRAW_AREA))
              )
              .setLevel(Task.DataCollectionLevel.LOI_METADATA)
          },
          taskType = Type.DRAW_AREA,
          isLoiTask = true,
        ),
        testCase(
          testLabel = "drop_pin",
          taskTypeNestedObjectLabel = "drop_pin",
          protoBuilderLambda = { taskBuilder: Task.Builder ->
            taskBuilder
              .setDrawGeometry(
                Task.DrawGeometry.newBuilder().addAllAllowedMethods(listOf(Method.DROP_PIN))
              )
              .setLevel(Task.DataCollectionLevel.LOI_METADATA)
          },
          taskType = Type.DROP_PIN,
          isLoiTask = true,
        ),
        testCase(
          testLabel = "capture_location",
          taskTypeNestedObjectLabel = "capture_location",
          protoBuilderLambda = { taskBuilder: Task.Builder ->
            taskBuilder
              .setCaptureLocation(Task.CaptureLocation.newBuilder().setMinAccuracyMeters(10f))
              .setLevel(Task.DataCollectionLevel.LOI_METADATA)
          },
          taskType = Type.CAPTURE_LOCATION,
          isLoiTask = true,
        ),
        testCase(
          testLabel = "photo",
          taskTypeNestedObjectLabel = "photo",
          protoBuilderLambda = { taskBuilder: Task.Builder ->
            taskBuilder.setTakePhoto(
              Task.TakePhoto.newBuilder().setMinHeadingDegrees(10).setMaxHeadingDegrees(10)
            )
          },
          taskType = Type.PHOTO,
        ),
      )
    /** Help to improve readability by provided named args for positional test constructor args. */
    private fun testCase(
      testLabel: String,
      taskTypeNestedObjectLabel: String,
      protoBuilderLambda: (Task.Builder) -> Task.Builder,
      taskType: Type,
      multipleChoice: MultipleChoice? = null,
      isLoiTask: Boolean = false,
    ) =
      arrayOf(
        testLabel,
        taskTypeNestedObjectLabel,
        protoBuilderLambda,
        taskType,
        multipleChoice,
        isLoiTask,
      )
  }
}
