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

  data class State(val isTooClose: Boolean = false, val isMarkedComplete: Boolean = false)

  /** The current state of the session. */
  val state: State

  /** The list of committed vertices in the current drawing session. */
  val vertices: List<Coordinates>

  /** The stack of vertices that have been removed and can be redone. */
  val redoVertexStack: List<Coordinates>

  /** Whether the current polygon geometry self-intersects. */
  val hasSelfIntersection: Boolean

  /**
   * Sets the vertices to a new list.
   *
   * @param newVertices The new list of vertices.
   */
  fun setVertices(newVertices: List<Coordinates>)

  /**
   * Updates the tentative vertex of the session with snapping logic.
   *
   * @param target The target coordinates (e.g., camera center).
   * @param calculateDistance A function to calculate distance in pixels between two coordinates.
   */
  fun updateTentativeVertex(
    target: Coordinates,
    calculateDistance: (Coordinates, Coordinates) -> Double,
  )

  /**
   * Commits the tentative vertex and returns the new committed list to signal persistence needs.
   *
   * @param currentCameraTarget The current camera target to use if tentative vertex is null.
   * @return The updated list of committed vertices if changed, or null.
   */
  fun commitTentativeVertex(currentCameraTarget: Coordinates?): List<Coordinates>?

  /**
   * Checks if the current polygon geometry self-intersects. If it does, it may remove the last
   * vertex to restore validity.
   *
   * @return true if self-intersection was detected, false otherwise.
   */
  fun checkVertexIntersection(): Boolean

  /**
   * Validates if the polygon can be completed.
   *
   * @return true if valid for completion, false otherwise.
   */
  fun isValidPolygon(): Boolean

  /** Completes the polygon drawing session. */
  fun complete()

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

  companion object {
    /** Min. distance in dp between two points for them be considered as overlapping. */
    const val DISTANCE_THRESHOLD_DP = 24
  }
}
