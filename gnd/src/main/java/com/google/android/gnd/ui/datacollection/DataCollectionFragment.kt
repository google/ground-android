/*
 * Copyright 2022 Google LLC
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
package com.google.android.gnd.ui.datacollection

import dagger.hilt.android.AndroidEntryPoint
import com.google.android.gnd.ui.common.AbstractFragment
import javax.inject.Inject
import com.google.android.gnd.ui.common.FeatureHelper
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gnd.MainActivity
import com.google.android.gnd.databinding.DataCollectionFragBinding
import com.google.android.gnd.ui.common.Navigator

/** Fragment allowing the user to collect data to complete a task.  */
@AndroidEntryPoint
class DataCollectionFragment : AbstractFragment() {
    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var featureHelper: FeatureHelper

    private lateinit var viewModel: DataCollectionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = getViewModel(DataCollectionViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        val binding = DataCollectionFragBinding.inflate(inflater, container, false)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        (activity as MainActivity?)!!.setActionBar(binding.dataCollectionToolbar, true)

        return binding.root
    }
}