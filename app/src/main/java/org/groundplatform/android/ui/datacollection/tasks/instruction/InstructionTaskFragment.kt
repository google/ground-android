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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.datacollection.components.Header
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskFragment
import org.groundplatform.ui.theme.sizes

@AndroidEntryPoint
class InstructionTaskFragment : AbstractTaskFragment<InstructionTaskViewModel>() {

  override val taskHeader: Header? = null

  @Composable
  override fun TaskBody() {
    ShowTextField(viewModel.task.label)
  }

  @Composable
  private fun ShowTextField(text: String) {
     Box(modifier = Modifier.padding(MaterialTheme.sizes.taskViewPadding)) {
      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(color = Color.White)
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
            .padding(MaterialTheme.sizes.taskViewPadding)
      ) {
        Text(text = text, style = MaterialTheme.typography.headlineSmall)
      }
    }
  }

  @Composable
  @Preview(showBackground = true)
  @ExcludeFromJacocoGeneratedReport
  private fun PreviewTextField() = ShowTextField("Sample instruction text")
}
