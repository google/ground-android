package com.google.android.ground.ui.datacollection

import com.google.android.ground.model.task.Option
import kotlinx.serialization.Serializable

/** ViewModel for representing Multiple Choice [Option]s */
@Serializable
data class OptionViewModel(val option: Option, var isChecked: Boolean)