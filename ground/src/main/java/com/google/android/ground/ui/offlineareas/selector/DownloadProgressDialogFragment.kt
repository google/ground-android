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

package com.google.android.ground.ui.offlineareas.selector

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import com.google.android.ground.R
import com.google.android.ground.databinding.DownloadProgressDialogFragBinding
import com.google.android.ground.ui.common.AbstractDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DownloadProgressDialogFragment : AbstractDialogFragment() {
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val inflater = requireActivity().layoutInflater
    val binding = DownloadProgressDialogFragBinding.inflate(inflater)
    binding.lifecycleOwner = this
    binding.viewModel = getViewModel(OfflineAreaSelectorViewModel::class.java)
    val dialog =
      AlertDialog.Builder(requireActivity())
        .setTitle(getString(R.string.offline_map_imagery_download_progress_dialog_title))
        .setMessage(getString(R.string.offline_map_imagery_download_progress_dialog_message))
        .setView(binding.root)
        .setCancelable(false)
        .create()
    dialog.setCanceledOnTouchOutside(false)
    return dialog
  }

  fun setVisibility(childFragmentManager: FragmentManager, newVisibility: Boolean) {
    if (newVisibility && !isAdded && !isVisible) {
      show(childFragmentManager, this::class.simpleName)
    } else if (!newVisibility && isAdded && isVisible) {
      dismiss()
    }
  }
}
