package com.google.android.ground.ui.datacollection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.ground.R
import com.google.android.ground.ui.common.AbstractFragment
import dagger.hilt.android.AndroidEntryPoint

/** Fragment allowing the user to collect data to complete a task.  */
@AndroidEntryPoint
class DataCollectionTaskFragment : AbstractFragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.data_collection_step_frag, container, false)
}