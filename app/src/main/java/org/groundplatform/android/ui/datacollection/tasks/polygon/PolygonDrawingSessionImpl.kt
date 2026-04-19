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

import kotlinx.collections.immutable.toImmutableList
import org.groundplatform.android.ui.datacollection.tasks.polygon.PolygonDrawingSession.Companion.DISTANCE_THRESHOLD_DP
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.util.isSelfIntersecting

class PolygonDrawingSessionImpl : PolygonDrawingSession {
  private var _vertices: List<Coordinates> = listOf()

  override val vertices: List<Coordinates>
    get() = _vertices

  private val _redoVertexStack = mutableListOf<Coordinates>()

  override val redoVertexStack: List<Coordinates>
    get() = _redoVertexStack

  private var _isTooClose: Boolean = false
  override val isTooClose: Boolean
    get() = _isTooClose

  private var _hasSelfIntersection: Boolean = false
  override val hasSelfIntersection: Boolean
    get() = _hasSelfIntersection

  override fun addVertex(vertex: Coordinates, shouldOverwriteLastVertex: Boolean) {
    val updatedVertices = _vertices.toMutableList()

    if (shouldOverwriteLastVertex && updatedVertices.isNotEmpty()) {
      updatedVertices.removeAt(updatedVertices.lastIndex)
    }

    updatedVertices.add(vertex)

    _vertices = updatedVertices.toImmutableList()
  }

  override fun updateTentativeVertex(
    target: Coordinates,
    calculateDistance: (Coordinates, Coordinates) -> Double,
  ) {
    val firstVertex = _vertices.firstOrNull()
    var updatedTarget = target
    if (firstVertex != null && _vertices.size > 2) {
      val distance = calculateDistance(firstVertex, target)

      if (distance <= DISTANCE_THRESHOLD_DP) {
        updatedTarget = firstVertex
      }
    }

    val prev = _vertices.dropLast(1).lastOrNull()
    _isTooClose =
      _vertices.size > 1 &&
        prev?.let { calculateDistance(it, target) <= DISTANCE_THRESHOLD_DP } == true

    addVertex(updatedTarget, true)
  }

  override fun commitTentativeVertex(currentCameraTarget: Coordinates?): List<Coordinates>? {
    _redoVertexStack.clear()
    val vertex = _vertices.lastOrNull() ?: currentCameraTarget
    return if (vertex != null) {
      _isTooClose = _vertices.size > 1
      addVertex(vertex, false)
      _vertices
    } else {
      null
    }
  }

  override fun checkVertexIntersection(): Boolean {
    _hasSelfIntersection = isSelfIntersecting(_vertices)
    if (_hasSelfIntersection) {
      _vertices = _vertices.dropLast(1).toImmutableList()
    }
    return _hasSelfIntersection
  }

  override fun validatePolygonCompletion(): Boolean {
    if (_vertices.size < 3) {
      return false
    }

    val ring =
      if (_vertices.first() != _vertices.last()) {
        _vertices + _vertices.first()
      } else {
        _vertices
      }

    _hasSelfIntersection = isSelfIntersecting(ring)
    return !_hasSelfIntersection
  }

  override fun removeLastVertex(): Boolean {
    if (_vertices.isEmpty()) return false

    _redoVertexStack.add(_vertices.last())
    _vertices = _vertices.dropLast(1).toImmutableList()
    return true
  }

  override fun redoLastVertex(): Coordinates? {
    if (_redoVertexStack.isEmpty()) return null

    val redoVertex = _redoVertexStack.removeAt(_redoVertexStack.lastIndex)
    _vertices = (_vertices + redoVertex).toImmutableList()
    return redoVertex
  }

  override fun clearRedoStack() {
    _redoVertexStack.clear()
  }

  override fun setVertices(newVertices: List<Coordinates>) {
    _vertices = newVertices.toImmutableList()
  }
}
