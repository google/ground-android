/*
 * Copyright 2025 Google LLC
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
package org.groundplatform.android.ui.map.gms.features

import android.content.res.Resources
import com.google.android.gms.maps.GoogleMap
import javax.inject.Inject
import org.groundplatform.android.model.geometry.LineString
import org.groundplatform.android.ui.util.formatDistance
import org.groundplatform.android.util.midPointToLastSegment
import org.groundplatform.android.util.tooltipDistanceIfLineStringClosed

class LineStringOverlayRenderer
@Inject
constructor(
  private val resources: Resources,
  private val tooltipMarkerRenderer: TooltipMarkerRenderer,
) {

  fun renderOverlayFor(map: GoogleMap, line: LineString) {
    if (line.coordinates.size < 2) {
      remove()
      return
    }
    val coords = line.coordinates
    val distance = coords.tooltipDistanceIfLineStringClosed() ?: return
    val midPoint = coords.midPointToLastSegment() ?: return
    val distanceText = formatDistance(resources, distance)

    tooltipMarkerRenderer.update(map, midPoint, distanceText)
  }

  fun remove() {
    tooltipMarkerRenderer.remove()
  }
}
