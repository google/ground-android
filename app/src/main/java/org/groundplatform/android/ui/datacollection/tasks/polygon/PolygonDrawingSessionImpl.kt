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
package org.groundplatform.android.ui.datacollection.tasks.polygon

import androidx.annotation.VisibleForTesting
import kotlinx.collections.immutable.toImmutableList
import org.groundplatform.android.ui.datacollection.tasks.polygon.PolygonDrawingSession.Companion.DISTANCE_THRESHOLD_DP
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.geometry.LineString
import org.groundplatform.domain.util.isClosed
import org.groundplatform.domain.util.isSelfIntersecting

class PolygonDrawingSessionImpl : PolygonDrawingSession {
  private var _isTooClose: Boolean = false
  private var _isMarkedComplete: Boolean = false

  override val state: PolygonDrawingSession.State
    get() = PolygonDrawingSession.State(_isTooClose, _isMarkedComplete)

  private var _vertices: List<Coordinates> = listOf()

  override val vertices: List<Coordinates>
    get() = _vertices

  private var _tentativeVertex: Coordinates? = null

  override val tentativeVertex: Coordinates?
    get() = _tentativeVertex

  override val displayVertices: List<Coordinates>
    get() = _tentativeVertex?.let { _vertices + it } ?: _vertices

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  internal val redoVertexStack = mutableListOf<Coordinates>()

  override val hasSelfIntersection: Boolean
    get() = isSelfIntersecting(displayVertices)

  override fun setVertices(newVertices: List<Coordinates>) {
    _vertices = newVertices.toImmutableList()
    _tentativeVertex = null
    redoVertexStack.clear()
  }

  override fun updateTentativeVertex(
    target: Coordinates,
    calculateDistance: (Coordinates, Coordinates) -> Double,
  ) {
    if (isClosed(_vertices)) {
      _tentativeVertex = null
      _isTooClose = true
      return
    }

    val firstVertex = _vertices.firstOrNull()
    var updatedTarget = target
    if (firstVertex != null && _vertices.size >= 3) {
      val distance = calculateDistance(firstVertex, target)

      if (distance <= DISTANCE_THRESHOLD_DP) {
        updatedTarget = firstVertex
      }
    }

    val lastCommitted = _vertices.lastOrNull()
    _isTooClose =
      lastCommitted?.let { calculateDistance(it, target) <= DISTANCE_THRESHOLD_DP } == true

    _tentativeVertex = updatedTarget
  }

  override fun commitTentativeVertex(currentCameraTarget: Coordinates?): List<Coordinates>? {
    redoVertexStack.clear()
    val vertex = _tentativeVertex ?: currentCameraTarget
    return vertex?.let {
      _vertices = (_vertices + it).toImmutableList()
      _tentativeVertex = null
      _isTooClose = _vertices.size > 1
      _vertices
    }
  }

  override fun checkVertexIntersection(): Boolean {
    val intersected = isSelfIntersecting(_vertices)
    if (intersected) {
      _vertices = _vertices.dropLast(1).toImmutableList()
    }
    return intersected
  }

  override fun isValidPolygon(): Boolean = LineString(displayVertices).isClosed() &&
    !isSelfIntersecting(displayVertices) &&
    displayVertices.distinct().size >= 3

  override fun complete() {
    check(isValidPolygon()) { "Polygon is not valid" }
    check(!_isMarkedComplete) { "Already marked complete" }
    if (_tentativeVertex != null) {
      commitTentativeVertex(null)
    }
    _isMarkedComplete = true
  }

  override fun removeLastVertex(): Boolean {
    if (_tentativeVertex == null && _vertices.isEmpty()) return false

    _isMarkedComplete = false
    _tentativeVertex = null

    if (_vertices.isNotEmpty()) {
      redoVertexStack.add(_vertices.last())
      _vertices = _vertices.dropLast(1).toImmutableList()
    }

    return true
  }

  override fun redoLastVertex(): Coordinates? {
    if (redoVertexStack.isEmpty()) return null

    _isMarkedComplete = false
    val redoVertex = redoVertexStack.removeAt(redoVertexStack.lastIndex)
    _vertices = (_vertices + redoVertex).toImmutableList()
    _tentativeVertex = null
    return redoVertex
  }

  override fun canRedo(): Boolean = redoVertexStack.isNotEmpty()
}
