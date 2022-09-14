/*
 * Copyright 2021 Google LLC
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
package com.google.android.ground.ui.home.mapcontainer

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import com.google.android.ground.databinding.DialogPolygonInfoBinding
import com.google.android.ground.ui.common.AbstractDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PolygonDrawingInfoDialogFragment(private val onGetStartedButtonClick: Runnable) :
  AbstractDialogFragment() {
  lateinit var binding: DialogPolygonInfoBinding

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    super.onCreateDialog(savedInstanceState)

    val builder = AlertDialog.Builder(requireActivity())
    val inflater = requireActivity().layoutInflater
    binding = DialogPolygonInfoBinding.inflate(inflater)
    binding.getStartedButton.setOnClickListener {
      dismiss()
      onGetStartedButtonClick.run()
    }
    builder.setView(binding.root)
    binding.cancelTextView.setOnClickListener { dismiss() }
    return builder.create()
  }
}
