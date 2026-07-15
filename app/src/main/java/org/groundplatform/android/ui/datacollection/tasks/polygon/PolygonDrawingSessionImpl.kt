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
  private var committedVertices: List<Coordinates> = listOf()

  /**
   * Tentative vertex that follows the map center when drawing, `null` if the user hasn't started.
   */
  private var cursor: Coordinates? = null

  private var _isTooClose: Boolean = false
  private var _isMarkedComplete: Boolean = false

  override val state: PolygonDrawingSession.State
    get() =
      PolygonDrawingSession.State(
        isTooClose = _isTooClose,
        isMarkedComplete = _isMarkedComplete,
        isClosed = LineString(vertices).isClosed(),
        canRedo = redoVertexStack.isNotEmpty(),
        hasSelfIntersection = isSelfIntersecting(vertices),
      )

  override val vertices: List<Coordinates>
    get() = cursor?.let { committedVertices + it } ?: committedVertices

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  internal val redoVertexStack = mutableListOf<Coordinates>()

  override val hasSelfIntersection: Boolean
    get() = isSelfIntersecting(vertices)

  private fun setGeometry(points: List<Coordinates>) {
    committedVertices = points.dropLast(1).toImmutableList()
    cursor = points.lastOrNull()
  }

  override fun setVertices(newVertices: List<Coordinates>) {
    setGeometry(newVertices)
  }

  override fun updateTentativeVertex(
    target: Coordinates,
    calculateDistance: (Coordinates, Coordinates) -> Double,
  ) {
    val firstVertex = vertices.firstOrNull()
    val shouldSnapToClose =
      firstVertex != null &&
        vertices.size > 2 &&
        calculateDistance(firstVertex, target) <= DISTANCE_THRESHOLD_DP
    cursor = if (shouldSnapToClose) firstVertex else target

    val lastCommitted = committedVertices.lastOrNull()
    _isTooClose =
      lastCommitted != null && calculateDistance(lastCommitted, target) <= DISTANCE_THRESHOLD_DP
  }

  override fun commitTentativeVertex(currentCameraTarget: Coordinates?): List<Coordinates>? {
    redoVertexStack.clear()
    // If the user taps "Add" before moving the map there is no cursor yet, so  fall back to the
    // current map center.
    val point = cursor ?: currentCameraTarget ?: return null
    committedVertices = committedVertices + point
    cursor = point
    _isTooClose = true
    return vertices
  }

  override fun checkVertexIntersection(): Boolean {
    val intersected = isSelfIntersecting(vertices)
    if (intersected) {
      setGeometry(vertices.dropLast(1))
    }
    return intersected
  }

  override fun isValidPolygon(): Boolean =
    LineString(vertices).isClosed() && !isSelfIntersecting(vertices)

  override fun complete() {
    check(isValidPolygon()) { "Polygon is not valid" }
    check(!_isMarkedComplete) { "Already marked complete" }
    _isMarkedComplete = true
  }

  override fun removeLastVertex(): Boolean {
    val currentVertices = vertices
    if (currentVertices.isEmpty()) return false

    _isMarkedComplete = false
    redoVertexStack.add(currentVertices.last())
    setGeometry(currentVertices.dropLast(1))
    return true
  }

  override fun redoLastVertex(): Coordinates? {
    if (redoVertexStack.isEmpty()) return null

    _isMarkedComplete = false
    val redoVertex = redoVertexStack.removeAt(redoVertexStack.lastIndex)
    setGeometry(vertices + redoVertex)
    return redoVertex
  }
}
