/*
 * Copyright 2018 Google LLC
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
package com.google.android.gnd.ui.home

import android.app.Dialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.google.android.gnd.ui.common.AbstractDialogFragment
import com.google.android.gnd.ui.home.AddFeatureDialogFragment
import android.os.Bundle
import timber.log.Timber
import com.google.android.gnd.util.ImmutableListCollector
import com.google.android.gnd.R
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import androidx.core.util.Consumer
import androidx.fragment.app.FragmentManager
import com.google.android.gnd.model.layer.Layer
import com.google.common.collect.ImmutableList
import java8.util.Objects
import java8.util.stream.StreamSupport
import java.lang.RuntimeException
import java.util.*

@AndroidEntryPoint
class AddFeatureDialogFragment @Inject constructor() : AbstractDialogFragment() {

private var layerConsumer: Consumer<Layer>? = null
private var layers: List<Layer>? = null

    fun show(
    layers: List<Layer>,
    fragmentManager: FragmentManager,
    layerConsumer: Consumer<Layer>
    ) {
      this.layers = layers
      this.layerConsumer = layerConsumer
      show(fragmentManager, TAG)
    }


  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    super.onCreateDialog(savedInstanceState)

    return if (layers != null && layerConsumer != null) {
      createDialog(sortByName(layers!!), layerConsumer!!)
    } else {
      Timber.e(e)
      fail("Error getting layers")
    }
  }

  private fun sortByName(layers: List<Layer>): List<Layer> = layers.sortedBy { it.name }

  private fun getLayerNames(layers: List<Layer>): List<String> = layers.map{ it.name }

  private fun createDialog(
    layers: List<Layer>,
    layerConsumer: Consumer<Layer>
    ): Dialog {
    // TODO: Add icons.
    return AlertDialog.Builder(requireContext())
    .setTitle(R.string.add_feature_select_type_dialog_title)
    .setNegativeButton(R.string.cancel, (_, _) -> { dismiss() })
    .setItems(getLayerNames(layers), (dialog, index) -> {
    layerConsumer.accept(layers[index])
    })
    .create()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    layers = null
    layerConsumer = null
  }

  companion object {
private val TAG = AddFeatureDialogFragment::class.java.simpleName
  }
}