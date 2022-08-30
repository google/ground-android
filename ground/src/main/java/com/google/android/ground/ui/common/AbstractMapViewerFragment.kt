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
package com.google.android.ground.ui.common

import android.os.Bundle
import android.view.View
import com.google.android.ground.R
import com.google.android.ground.ui.map.MapFragment
import javax.inject.Inject

/** Injects a [MapFragment] in the container with id "map". */
abstract class AbstractMapViewerFragment : AbstractFragment() {

  @Inject lateinit var mapFragment: MapFragment

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    mapFragment.attachToFragment(this, R.id.map) { onMapReady(it) }
  }

  protected abstract fun onMapReady(mapFragment: MapFragment)
}
