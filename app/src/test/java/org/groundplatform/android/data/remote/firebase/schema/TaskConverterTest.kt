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

package org.groundplatform.android.data.remote.firebase.schema

import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentListOf
import org.groundplatform.android.model.task.MultipleChoice
import org.groundplatform.android.model.task.Task as TaskModel
import org.groundplatform.android.model.task.Task.Type
import org.groundplatform.android.proto.Task
import org.groundplatform.android.proto.Task.DrawGeometry.Method
import org.groundplatform.android.proto.TaskKt.captureLocation
import org.groundplatform.android.proto.TaskKt.dateTimeQuestion
import org.groundplatform.android.proto.TaskKt.drawGeometry
import org.groundplatform.android.proto.TaskKt.multipleChoiceQuestion
import org.groundplatform.android.proto.TaskKt.numberQuestion
import org.groundplatform.android.proto.TaskKt.takePhoto
import org.groundplatform.android.proto.TaskKt.textQuestion
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class TaskConverterTest(
  private val testLabel: String,
  private val protoBuilderLambda: (Task.Builder) -> Task.Builder,
  private val taskType: Type,
  private val multipleChoice: MultipleChoice?,
  private val isLoiTask: Boolean,
  private val allowMovingPoint: Boolean,
  private val allowManualOverride: Boolean,
) {

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
          allowMovingPoint = allowMovingPoint,
          allowManualOverride = allowManualOverride,
        )
      )
  }

  companion object {
    private const val TASK_ID = "task_1"

    @Suppress("LongMethod")
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data() =
      listOf(
        testCase(
          testLabel = "text",
          protoBuilderLambda = { taskBuilder: Task.Builder ->
            taskBuilder.setTextQuestion(textQuestion { type = Task.TextQuestion.Type.SHORT_TEXT })
          },
          taskType = Type.TEXT,
          allowMovingPoint = true,
          allowManualOverride = true,
        ),
        testCase(
          testLabel = "number",
          protoBuilderLambda = { taskBuilder: Task.Builder ->
            taskBuilder.setNumberQuestion(numberQuestion { type = Task.NumberQuestion.Type.FLOAT })
          },
          taskType = Type.NUMBER,
          allowMovingPoint = true,
          allowManualOverride = true,
        ),
        testCase(
          testLabel = "date",
          protoBuilderLambda = { taskBuilder: Task.Builder ->
            taskBuilder.setDateTimeQuestion(
              dateTimeQuestion { type = Task.DateTimeQuestion.Type.DATE_ONLY }
            )
          },
          taskType = Type.DATE,
          allowMovingPoint = true,
          allowManualOverride = true,
        ),
        testCase(
          testLabel = "time",
          protoBuilderLambda = { taskBuilder: Task.Builder ->
            taskBuilder.setDateTimeQuestion(
              dateTimeQuestion { type = Task.DateTimeQuestion.Type.TIME_ONLY }
            )
          },
          taskType = Type.TIME,
          allowMovingPoint = true,
          allowManualOverride = true,
        ),
        // Defaults to date when set to BOTH_DATE_AND_TIME
        testCase(
          testLabel = "both_date_and_time",
          protoBuilderLambda = { taskBuilder: Task.Builder ->
            taskBuilder.setDateTimeQuestion(
              dateTimeQuestion { type = Task.DateTimeQuestion.Type.BOTH_DATE_AND_TIME }
            )
          },
          taskType = Type.DATE,
          allowMovingPoint = true,
          allowManualOverride = true,
        ),
        testCase(
          testLabel = "multiple_choice",
          protoBuilderLambda = { taskBuilder: Task.Builder ->
            taskBuilder.setMultipleChoiceQuestion(
              multipleChoiceQuestion { type = Task.MultipleChoiceQuestion.Type.SELECT_ONE }
            )
          },
          taskType = Type.MULTIPLE_CHOICE,
          multipleChoice =
            MultipleChoice(
              options = persistentListOf(),
              cardinality = MultipleChoice.Cardinality.SELECT_ONE,
              hasOtherOption = false,
            ),
          allowMovingPoint = true,
          allowManualOverride = true,
        ),
        testCase(
          testLabel = "draw_area",
          protoBuilderLambda = { taskBuilder: Task.Builder ->
            taskBuilder
              .setDrawGeometry(drawGeometry { allowedMethods.addAll(listOf(Method.DRAW_AREA)) })
              .setLevel(Task.DataCollectionLevel.LOI_METADATA)
          },
          taskType = Type.DRAW_AREA,
          isLoiTask = true,
          allowMovingPoint = true,
          allowManualOverride = true,
        ),
        testCase(
          testLabel = "drop_pin",
          protoBuilderLambda = { taskBuilder: Task.Builder ->
            taskBuilder
              .setDrawGeometry(drawGeometry { allowedMethods.addAll(listOf(Method.DROP_PIN)) })
              .setLevel(Task.DataCollectionLevel.LOI_METADATA)
          },
          taskType = Type.DROP_PIN,
          isLoiTask = true,
          allowMovingPoint = true,
          allowManualOverride = true,
        ),
        testCase(
          testLabel = "capture_location",
          protoBuilderLambda = { taskBuilder: Task.Builder ->
            taskBuilder
              .setCaptureLocation(captureLocation { minAccuracyMeters = 10f })
              .setLevel(Task.DataCollectionLevel.LOI_METADATA)
          },
          taskType = Type.CAPTURE_LOCATION,
          isLoiTask = true,
          allowMovingPoint = false,
          allowManualOverride = true,
        ),
        testCase(
          testLabel = "photo",
          protoBuilderLambda = { taskBuilder: Task.Builder ->
            taskBuilder.setTakePhoto(
              takePhoto {
                minHeadingDegrees = 10
                maxHeadingDegrees = 10
              }
            )
          },
          taskType = Type.PHOTO,
          allowMovingPoint = true,
          allowManualOverride = true,
        ),
        testCase(
          testLabel = "instructions",
          protoBuilderLambda = { taskBuilder: Task.Builder ->
            taskBuilder.setInstructions(taskBuilder.instructions)
          },
          taskType = Type.INSTRUCTIONS,
          allowMovingPoint = true,
          allowManualOverride = true,
        ),
      )

    /** Help to improve readability by provided named args for positional test constructor args. */
    private fun testCase(
      testLabel: String,
      protoBuilderLambda: (Task.Builder) -> Task.Builder,
      taskType: Type,
      multipleChoice: MultipleChoice? = null,
      isLoiTask: Boolean = false,
      allowMovingPoint: Boolean = true,
      allowManualOverride: Boolean = true,
    ) =
      arrayOf(
        testLabel,
        protoBuilderLambda,
        taskType,
        multipleChoice,
        isLoiTask,
        allowMovingPoint,
        allowManualOverride,
      )
  }
}
