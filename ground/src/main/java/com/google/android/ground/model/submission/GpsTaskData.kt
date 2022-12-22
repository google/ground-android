package com.google.android.ground.model.submission

import java8.util.Optional
import kotlinx.serialization.Serializable

@Serializable
data class GpsTaskData(val gps: String) : TaskData {

  override fun getDetailsText(): String = ""

  override fun isEmpty(): Boolean = true

  companion object {
    @JvmStatic fun fromLocation(): Optional<TaskData> = Optional.empty()
  }
}
