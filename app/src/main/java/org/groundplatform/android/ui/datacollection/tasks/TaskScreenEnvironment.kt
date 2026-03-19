/*
 * Copyright 2026 Google LLC
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
package org.groundplatform.android.ui.datacollection.tasks

import androidx.fragment.app.FragmentManager
import javax.inject.Provider
import org.groundplatform.android.ui.datacollection.DataCollectionViewModel
import org.groundplatform.android.ui.datacollection.tasks.location.CaptureLocationTaskMapFragment
import org.groundplatform.android.ui.datacollection.tasks.point.DropPinTaskMapFragment
import org.groundplatform.android.ui.datacollection.tasks.polygon.DrawAreaTaskMapFragment
import org.groundplatform.android.ui.home.HomeScreenViewModel

data class TaskScreenEnvironment(
  val captureLocationTaskMapFragmentProvider: Provider<CaptureLocationTaskMapFragment>,
  val dataCollectionViewModel: DataCollectionViewModel,
  val drawAreaTaskMapFragmentProvider: Provider<DrawAreaTaskMapFragment>,
  val dropPinTaskMapFragmentProvider: Provider<DropPinTaskMapFragment>,
  val fragmentManager: FragmentManager,
  val homeScreenViewModel: HomeScreenViewModel,
)
