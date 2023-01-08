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
package com.google.android.ground.ui.home.mapcontainer

import com.google.android.ground.model.geometry.Point
import com.google.android.ground.rx.Nil
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.android.ground.ui.common.SharedViewModel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

@SharedViewModel
class LocationOfInterestRepositionViewModel @Inject internal constructor() : AbstractViewModel() {
  private val confirmButtonClicks: @Hot Subject<Point> = PublishSubject.create()
  private val cancelButtonClicks: @Hot Subject<Nil> = PublishSubject.create()
  private var cameraTarget: Point? = null

  // TODO: Disable the confirm button until the map has not been moved
  fun onConfirmButtonClick() {
    cameraTarget?.let { confirmButtonClicks.onNext(it) }
  }

  fun onCancelButtonClick() {
    cancelButtonClicks.onNext(Nil.NIL)
  }

  fun getConfirmButtonClicks(): @Hot Observable<Point> = confirmButtonClicks

  fun getCancelButtonClicks(): @Hot Observable<Nil> = cancelButtonClicks

  fun onCameraMoved(newTarget: Point) {
    cameraTarget = newTarget
  }
}
