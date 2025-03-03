package com.google.android.ground.ui.datacollection.tasks.instruction

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.google.android.ground.model.submission.TextTaskData
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map

class InstructionTaskViewModel @Inject constructor() : AbstractTaskViewModel() {

  val instructionsText: LiveData<String> =
    taskTaskData.filterIsInstance<TextTaskData?>().map { it?.text ?: "" }.asLiveData()
}
