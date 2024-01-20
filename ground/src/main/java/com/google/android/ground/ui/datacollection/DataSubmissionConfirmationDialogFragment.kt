/*
 * Copyright 2023 Google LLC
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
package com.google.android.ground.ui.datacollection

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import com.google.android.ground.databinding.DataSubmissionConfirmationDialogBinding
import com.google.android.ground.ui.common.AbstractDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DataSubmissionConfirmationDialogFragment : AbstractDialogFragment() {

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    super.onCreateDialog(savedInstanceState)

    val builder = AlertDialog.Builder(requireActivity())
    val inflater = requireActivity().layoutInflater
    val binding = DataSubmissionConfirmationDialogBinding.inflate(inflater)
    builder.setView(binding.root)
    binding.doneBtn.setOnClickListener { dismiss() }
    return builder.create()
  }
}
