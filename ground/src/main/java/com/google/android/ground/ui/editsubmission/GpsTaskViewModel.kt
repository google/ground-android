package com.google.android.ground.ui.editsubmission

import android.content.res.Resources
import com.google.android.ground.model.submission.GpsTaskData
import javax.inject.Inject

class GpsTaskViewModel @Inject constructor(resources: Resources) :
  AbstractTaskViewModel(resources) {

  fun updateResponse(response: String) {
    setResponse(GpsTaskData.fromLocation())
  }
}
