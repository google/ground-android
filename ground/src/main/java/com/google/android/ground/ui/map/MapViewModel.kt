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

package com.google.android.ground.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MapViewModel : ViewModel() {
  private val _mapType = MutableLiveData<MapType>()
  val mapType: LiveData<MapType> by this::_mapType

  private val _cameraMoveStartGestureEvents =
    MutableSharedFlow<Unit>(onBufferOverflow = BufferOverflow.DROP_OLDEST)
  val cameraMoveStartGestureEvents = _cameraMoveStartGestureEvents.asSharedFlow()

  private val _cameraIdleEvents =
    MutableSharedFlow<CameraPosition>(onBufferOverflow = BufferOverflow.DROP_OLDEST)
  val cameraIdleEvents = _cameraIdleEvents.asSharedFlow()

  fun onCameraMoveStartGesture() = _cameraMoveStartGestureEvents.tryEmit(Unit)

  fun onCameraIdle(cameraPosition: CameraPosition) = _cameraIdleEvents.tryEmit(cameraPosition)
}
