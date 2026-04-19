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

import org.groundplatform.domain.model.geometry.Coordinates

/** Encapsulates the state and basic operations for a polygon drawing session. */
interface PolygonDrawingSession {

  /** The list of committed vertices in the current drawing session. */
  val vertices: List<Coordinates>

  /** The stack of vertices that have been removed and can be redone. */
  val redoVertexStack: List<Coordinates>

  /**
   * Adds a new vertex to the polygon.
   *
   * @param vertex The coordinates of the vertex to add.
   * @param shouldOverwriteLastVertex If true, the last vertex will be replaced by the new one. This
   *   is typically used for updating the transient vertex following the camera.
   */
  fun addVertex(vertex: Coordinates, shouldOverwriteLastVertex: Boolean)

  /**
   * Attempts to remove the last vertex of the drawn polygon. The removed vertex is added to the
   * redo stack.
   *
   * @return true if a vertex was removed, false otherwise.
   */
  fun removeLastVertex(): Boolean

  /**
   * Redoes the last removed vertex by removing it from the redo stack and adding it back to the
   * vertices list.
   *
   * @return The restored vertex, or null if the redo stack was empty.
   */
  fun redoLastVertex(): Coordinates?

  /** Clears the redo stack. */
  fun clearRedoStack()

  /**
   * Sets the vertices to a new list.
   *
   * @param newVertices The new list of vertices.
   */
  fun setVertices(newVertices: List<Coordinates>)
}
