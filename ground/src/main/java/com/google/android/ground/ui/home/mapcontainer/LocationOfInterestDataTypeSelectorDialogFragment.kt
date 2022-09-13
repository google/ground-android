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
import android.widget.ArrayAdapter
import com.google.android.ground.R
import com.google.android.ground.ui.common.AbstractDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import java8.util.function.Consumer

@AndroidEntryPoint
class LocationOfInterestDataTypeSelectorDialogFragment(
  private val onSelectLocationOfInterestDataType: Consumer<Int>
) : AbstractDialogFragment() {
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    super.onCreateDialog(savedInstanceState)

    val listAdapter =
      ArrayAdapter<String>(requireContext(), R.layout.survey_selector_list_item, R.id.survey_name)
    listAdapter.add(getString(R.string.point))
    listAdapter.add(getString(R.string.polygon))
    return AlertDialog.Builder(context)
      .setTitle(R.string.select_loi_type)
      .setAdapter(listAdapter) { _, position ->
        onSelectLocationOfInterestDataType.accept(position)
      }
      .setCancelable(true)
      .create()
  }
}
