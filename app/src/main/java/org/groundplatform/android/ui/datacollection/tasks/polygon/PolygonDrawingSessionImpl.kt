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
import org.groundplatform.domain.model.geometry.Coordinates

class PolygonDrawingSessionImpl : PolygonDrawingSession {
  private var _vertices: List<Coordinates> = listOf()

  override val vertices: List<Coordinates>
    get() = _vertices

  private val _redoVertexStack = mutableListOf<Coordinates>()

  override val redoVertexStack: List<Coordinates>
    get() = _redoVertexStack

  override fun addVertex(vertex: Coordinates, shouldOverwriteLastVertex: Boolean) {
    val updatedVertices = _vertices.toMutableList()

    if (shouldOverwriteLastVertex && updatedVertices.isNotEmpty()) {
      updatedVertices.removeAt(updatedVertices.lastIndex)
    }

    updatedVertices.add(vertex)

    _vertices = updatedVertices.toImmutableList()
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
