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
package com.google.android.ground.ui.datacollection.tasks.instruction

import android.view.LayoutInflater
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.ground.ui.datacollection.components.TaskView
import com.google.android.ground.ui.datacollection.components.TaskViewFactory
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskFragment
import com.google.android.ground.util.createComposeView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InstructionTaskFragment : AbstractTaskFragment<InstructionTaskViewModel>() {

  override fun onCreateTaskView(inflater: LayoutInflater): TaskView =
    TaskViewFactory.createWithoutHeader(inflater)

  override fun onCreateTaskBody(inflater: LayoutInflater): View = createComposeView {
    ShowTextField()
  }

  @Composable
  private fun ShowTextField() {
    val instructions by viewModel.instructionsText.observeAsState("")
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(color = Color.White)
          .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
          .padding(16.dp)
    ) {
      Text(text = instructions, fontSize = 24.sp)
    }
  }
}
