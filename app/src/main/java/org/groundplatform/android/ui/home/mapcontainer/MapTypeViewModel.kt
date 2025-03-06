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
package org.groundplatform.android.ui.home.mapcontainer

import javax.inject.Inject
import org.groundplatform.android.repository.MapStateRepository
import org.groundplatform.android.ui.common.AbstractViewModel

class MapTypeViewModel
@Inject
internal constructor(private val mapStateRepository: MapStateRepository) : AbstractViewModel() {

  var isOfflineImageryEnabled by mapStateRepository::isOfflineImageryEnabled
  var mapType by mapStateRepository::mapType

  fun offlineImageryPreferenceUpdated(isChecked: Boolean) {
    mapStateRepository.isOfflineImageryEnabled = isChecked
  }
}
