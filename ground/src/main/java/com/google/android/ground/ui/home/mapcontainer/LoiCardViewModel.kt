/*
 * Copyright 2023 Google LLC
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
package com.google.android.ground.ui.home.mapcontainer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.common.AbstractViewModel

class LoiCardViewModel(loi: LocationOfInterest) : AbstractViewModel() {
  val isIconVisible: @Hot(replays = true) LiveData<Boolean>
  val isNameVisible: @Hot(replays = true) LiveData<Boolean>
  val loiName: @Hot(replays = true) LiveData<String>
  val loiJobName: @Hot(replays = true) LiveData<String>

  init {
    isIconVisible = MutableLiveData(loi.caption?.isNotEmpty())
    isNameVisible = MutableLiveData(loi.caption?.isNotEmpty())
    loiName = MutableLiveData(loi.caption)
    loiJobName = MutableLiveData(loi.job.name)
  }
}
