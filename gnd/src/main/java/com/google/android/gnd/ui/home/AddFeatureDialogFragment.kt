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
import android.os.Bundle
import com.google.android.gnd.R
import androidx.appcompat.app.AlertDialog
import androidx.core.util.Consumer
import androidx.fragment.app.FragmentManager
import com.google.android.gnd.model.layer.Layer

@AndroidEntryPoint
class AddFeatureDialogFragment @Inject constructor() : AbstractDialogFragment() {

    private val fragmentTag = AddFeatureDialogFragment::class.java.simpleName
    private lateinit var layerConsumer: Consumer<Layer>
    private lateinit var layers: List<Layer>

    fun show(
        layers: List<Layer>,
        fragmentManager: FragmentManager,
        layerConsumer: Consumer<Layer>
    ) {
        this.layers = layers
        this.layerConsumer = layerConsumer
        show(fragmentManager, fragmentTag)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        return createDialog(sortByName(layers), layerConsumer)
    }

    private fun sortByName(layers: List<Layer>): List<Layer> = layers.sortedBy { it.name }

    private fun getLayerNames(layers: List<Layer>): List<String> = layers.map { it.name }

    private fun createDialog(
        layers: List<Layer>,
        layerConsumer: Consumer<Layer>
    ): Dialog {
        // TODO: Add icons.
        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.add_feature_select_type_dialog_title)
            .setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
            .setItems(getLayerNames(layers).toTypedArray()) { _, index -> layerConsumer.accept(layers[index]) }
            .create()
    }
}
