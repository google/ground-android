/*
 * Copyright 2025 Google LLC
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
package org.groundplatform.android.ui.datacollection.tasks.instruction

import android.view.LayoutInflater
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.TaskView
import org.groundplatform.android.ui.datacollection.components.TaskViewFactory
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskFragment
import org.groundplatform.android.util.createComposeView

@AndroidEntryPoint
class InstructionTaskFragment : AbstractTaskFragment<InstructionTaskViewModel>() {

  override fun onCreateTaskView(inflater: LayoutInflater): TaskView =
    TaskViewFactory.createWithoutHeader(inflater)

  override fun onCreateTaskBody(inflater: LayoutInflater): View = createComposeView {
    ShowTextField()
  }

  override fun onCreateActionButtons() {
    addButton(ButtonAction.NEXT).setOnClickListener { moveToNext() }
  }

  @Composable
  private fun ShowTextField() {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(color = Color.White)
          .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
          .padding(16.dp)
    ) {
      Text(text = viewModel.task.label, fontSize = 24.sp)
    }
  }

  @Composable
  @Preview(showBackground = true)
  @ExcludeFromJacocoGeneratedReport
  private fun PreviewTextField() = ShowTextField()
}
