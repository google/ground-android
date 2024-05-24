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
package com.google.android.ground.ui.syncstatus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.ground.databinding.SyncStatusFragBinding
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.common.LocationOfInterestHelper
import com.google.android.ground.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import java.text.DateFormat
import javax.inject.Inject

/** Fragment containing a list of mutations and their respective upload statuses. */
@AndroidEntryPoint
class SyncStatusFragment : AbstractFragment() {

  @Inject lateinit var locationOfInterestHelper: LocationOfInterestHelper
  lateinit var viewModel: SyncStatusViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = getViewModel(SyncStatusViewModel::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    val binding = SyncStatusFragBinding.inflate(inflater, container, false)
    binding.viewModel = viewModel
    binding.lifecycleOwner = this

    getAbstractActivity().setSupportActionBar(binding.syncStatusToolbar)

    //    val data = listOf("One", "Two", "Three")

    //    val syncStatusListAdapter = SyncStatusListAdapter(requireContext())
    //    val recyclerView = binding.syncStatusList
    //    recyclerView.setHasFixedSize(true)
    //    recyclerView.layoutManager = LinearLayoutManager(context)
    //    recyclerView.adapter = syncStatusListAdapter

    binding.composeView.setContent { AppTheme { ShowSyncItems() } }

    //    viewModel.mutations.observe(viewLifecycleOwner) {
    //          syncStatusListAdapter.update(it)

    //    }
    return binding.root
  }

  @Composable
  private fun ShowSyncItems() {
    val list by viewModel.mutations.observeAsState()

    if (list == null) return

    LazyColumn(Modifier.fillMaxSize()) {
      items(list!!) { ListItem(modifier = Modifier, detail = it) }
    }
  }

  @Composable
  @Preview
  fun ListItem(
    modifier: Modifier = Modifier,
    detail: MutationDetail =
      MutationDetail(
        user = "User",
        loiLabel = "LoiLabel",
        loiSubtitle = "LoiSubtitle",
        mutation = SubmissionMutation(job = Job(id = "123")),
      ),
  ) {
    val dateFormat: DateFormat = android.text.format.DateFormat.getDateFormat(context)
    val timeFormat: DateFormat = android.text.format.DateFormat.getTimeFormat(context)

    Row(modifier.fillMaxWidth()) {
      Column {
        val modified =
          "${dateFormat.format(detail.mutation.clientTimestamp)} . ${timeFormat.format(detail.mutation.clientTimestamp)}"
        Text(text = modified)
        Text(text = detail.user)
        Text(text = detail.loiLabel)
        Text(text = detail.loiSubtitle)
      }
    }
  }
}
