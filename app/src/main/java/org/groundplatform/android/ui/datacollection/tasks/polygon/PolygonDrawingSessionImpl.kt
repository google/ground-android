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
import org.groundplatform.domain.util.isSelfIntersecting

class PolygonDrawingSessionImpl : PolygonDrawingSession {
  private var _isTooClose: Boolean = false
  private var _isMarkedComplete: Boolean = false

  override val state: PolygonDrawingSession.State
    get() = PolygonDrawingSession.State(_isTooClose, _isMarkedComplete)

  private var _vertices: List<Coordinates> = listOf()

  override val vertices: List<Coordinates>
    get() = _vertices

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  internal val redoVertexStack = mutableListOf<Coordinates>()

  override val hasSelfIntersection: Boolean
    get() = isSelfIntersecting(_vertices)

  private fun addVertex(vertex: Coordinates, shouldOverwriteLastVertex: Boolean) {
    val updatedVertices = _vertices.toMutableList()

    if (shouldOverwriteLastVertex && updatedVertices.isNotEmpty()) {
      updatedVertices.removeAt(updatedVertices.lastIndex)
    }

    updatedVertices.add(vertex)

    _vertices = updatedVertices.toImmutableList()
  }

  override fun setVertices(newVertices: List<Coordinates>) {
    _vertices = newVertices.toImmutableList()
  }

  override fun updateTentativeVertex(
    target: Coordinates,
    calculateDistance: (Coordinates, Coordinates) -> Double,
  ) {
    val firstVertex = _vertices.firstOrNull()
    var updatedTarget = target

    // Only allow snapping to the first vertex if we have at least 3 committed points.
    // _vertices size includes the tentative vertex, so we need > 3.
    if (firstVertex != null && _vertices.size > 3) {
      val distance = calculateDistance(firstVertex, target)

      if (distance <= DISTANCE_THRESHOLD_DP) {
        updatedTarget = firstVertex
      }
    }

    _vertices.dropLast(1)

    if (_vertices.size > 1) {
      val first = _vertices.first()
      val prev = _vertices.last()

      val closeToPrev = calculateDistance(prev, target) <= DISTANCE_THRESHOLD_DP
      val closeToFirst = calculateDistance(first, target) <= DISTANCE_THRESHOLD_DP
      val cannotClose = _vertices.size <= 3

      // Prevent closing the polygon if we don't have enough committed points (need at least 3).
      // If we are too close to the first vertex but cannot close, mark it as too close to prevent
      // committing a degenerate shape.
      _isTooClose = closeToPrev || (cannotClose && closeToFirst)
    } else {
      _isTooClose = false
    }

    addVertex(updatedTarget, true)
  }

  override fun commitTentativeVertex(currentCameraTarget: Coordinates?): List<Coordinates>? {
    redoVertexStack.clear()
    val vertex = _vertices.lastOrNull() ?: currentCameraTarget
    return vertex?.let {
      _isTooClose = _vertices.size > 1
      addVertex(it, false)
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

  override fun isValidPolygon(): Boolean =
    LineString(_vertices).isClosed() && !isSelfIntersecting(_vertices)

  override fun complete() {
    check(isValidPolygon()) { "Polygon is not valid" }
    check(!_isMarkedComplete) { "Already marked complete" }
    _isMarkedComplete = true
  }

  override fun removeLastVertex(): Boolean {
    if (_vertices.isEmpty()) return false

    _isMarkedComplete = false
    redoVertexStack.add(_vertices.last())
    _vertices = _vertices.dropLast(1).toImmutableList()

    if (_vertices.isEmpty()) {
      redoVertexStack.clear()
    }

    return true
  }

  override fun redoLastVertex(): Coordinates? {
    if (redoVertexStack.isEmpty()) return null

    _isMarkedComplete = false
    val redoVertex = redoVertexStack.removeAt(redoVertexStack.lastIndex)
    _vertices = (_vertices + redoVertex).toImmutableList()
    return redoVertex
  }

  override fun canRedo(): Boolean = redoVertexStack.isNotEmpty()
}
