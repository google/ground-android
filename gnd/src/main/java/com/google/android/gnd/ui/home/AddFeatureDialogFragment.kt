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
import com.google.android.gnd.model.layer.Job

@AndroidEntryPoint
class AddFeatureDialogFragment @Inject constructor() : AbstractDialogFragment() {

    private val fragmentTag = AddFeatureDialogFragment::class.java.simpleName
    private lateinit var jobConsumer: Consumer<Job>
    private lateinit var jobs: List<Job>

    fun show(
        jobs: List<Job>,
        fragmentManager: FragmentManager,
        jobConsumer: Consumer<Job>
    ) {
        this.jobs = jobs
        this.jobConsumer = jobConsumer
        show(fragmentManager, fragmentTag)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        return createDialog(sortByName(jobs), jobConsumer)
    }

    private fun sortByName(jobs: List<Job>): List<Job> = jobs.sortedBy { it.name }

    private fun getLayerNames(jobs: List<Job>): List<String> = jobs.map { it.name }

    private fun createDialog(
        jobs: List<Job>,
        jobConsumer: Consumer<Job>
    ): Dialog {
        // TODO: Add icons.
        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.add_feature_select_type_dialog_title)
            .setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
            .setItems(getLayerNames(jobs).toTypedArray()) { _, index -> jobConsumer.accept(jobs[index]) }
            .create()
    }
}
