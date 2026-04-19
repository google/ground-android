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

import com.google.common.truth.Truth.assertThat
import org.groundplatform.android.ui.datacollection.tasks.polygon.PolygonDrawingSession.Companion.DISTANCE_THRESHOLD_DP
import org.groundplatform.domain.model.geometry.Coordinates
import org.junit.Before
import org.junit.Test

class PolygonDrawingSessionTest {

  private lateinit var session: PolygonDrawingSession

  @Before
  fun setUp() {
    session = PolygonDrawingSessionImpl()
  }

  @Test
  fun `Initial state is empty`() {
    assertThat(session.vertices).isEmpty()
    assertThat(session.redoVertexStack).isEmpty()
  }

  @Test
  fun `addVertex adds vertex to list`() {
    session.addVertex(COORDINATE_1, false)
    assertThat(session.vertices).containsExactly(COORDINATE_1)
  }

  @Test
  fun `addVertex can overwrite last vertex`() {
    session.addVertex(COORDINATE_1, false)
    session.addVertex(COORDINATE_2, true)
    assertThat(session.vertices).containsExactly(COORDINATE_2)
  }

  @Test
  fun `addVertex with overwrite does nothing if empty`() {
    session.addVertex(COORDINATE_1, true)
    assertThat(session.vertices).containsExactly(COORDINATE_1)
  }

  @Test
  fun `removeLastVertex returns false when empty`() {
    assertThat(session.removeLastVertex()).isFalse()
  }

  @Test
  fun `removeLastVertex removes last vertex and adds to redo stack`() {
    session.addVertex(COORDINATE_1, false)
    session.addVertex(COORDINATE_2, false)

    assertThat(session.removeLastVertex()).isTrue()
    assertThat(session.vertices).containsExactly(COORDINATE_1)
    assertThat(session.redoVertexStack).containsExactly(COORDINATE_2)
  }

  @Test
  fun `redoLastVertex returns null when redo stack is empty`() {
    assertThat(session.redoLastVertex()).isNull()
  }

  @Test
  fun `redoLastVertex restores last removed vertex`() {
    session.addVertex(COORDINATE_1, false)
    session.addVertex(COORDINATE_2, false)
    session.removeLastVertex()

    assertThat(session.redoLastVertex()).isEqualTo(COORDINATE_2)
    assertThat(session.vertices).containsExactly(COORDINATE_1, COORDINATE_2).inOrder()
    assertThat(session.redoVertexStack).isEmpty()
  }

  @Test
  fun `clearRedoStack clears the stack`() {
    session.addVertex(COORDINATE_1, false)
    session.removeLastVertex()
    assertThat(session.redoVertexStack).isNotEmpty()

    session.clearRedoStack()
    assertThat(session.redoVertexStack).isEmpty()
  }

  @Test
  fun `setVertices updates vertices list`() {
    val newVertices = listOf(COORDINATE_1, COORDINATE_2)
    session.setVertices(newVertices)
    assertThat(session.vertices).containsExactlyElementsIn(newVertices).inOrder()
  }

  @Test
  fun `updateTentativeVertex adds vertex when empty`() {
    session.updateTentativeVertex(COORDINATE_1) { _, _ -> 100.0 }
    assertThat(session.vertices).containsExactly(COORDINATE_1)
  }

  @Test
  fun `updateTentativeVertex overwrites last vertex`() {
    session.addVertex(COORDINATE_1, false)
    session.updateTentativeVertex(COORDINATE_2) { _, _ -> 100.0 }
    assertThat(session.vertices).containsExactly(COORDINATE_2)
  }

  @Test
  fun `updateTentativeVertex snaps to first vertex when close`() {
    session.addVertex(COORDINATE_1, false)
    session.addVertex(COORDINATE_2, false)
    session.addVertex(COORDINATE_3, false)

    // Target is close to COORDINATE_1
    session.updateTentativeVertex(COORDINATE_4) { _, _ -> DISTANCE_THRESHOLD_DP.toDouble() }
    assertThat(session.vertices.last()).isEqualTo(COORDINATE_1)
  }

  @Test
  fun `updateTentativeVertex sets isTooClose true when close to previous`() {
    session.addVertex(COORDINATE_1, false)
    session.addVertex(COORDINATE_2, false)

    // Target is close to COORDINATE_2
    session.updateTentativeVertex(COORDINATE_3) { _, _ -> DISTANCE_THRESHOLD_DP.toDouble() }
    assertThat(session.isTooClose).isTrue()
  }

  @Test
  fun `updateTentativeVertex sets isTooClose false when far from previous`() {
    session.addVertex(COORDINATE_1, false)
    session.addVertex(COORDINATE_2, false)

    // Target is far from COORDINATE_2
    session.updateTentativeVertex(COORDINATE_3) { _, _ -> DISTANCE_THRESHOLD_DP.toDouble() + 1 }
    assertThat(session.isTooClose).isFalse()
  }

  @Test
  fun `commitTentativeVertex clears redo stack`() {
    session.addVertex(COORDINATE_1, false)
    session.removeLastVertex()
    assertThat(session.redoVertexStack).isNotEmpty()

    session.commitTentativeVertex(COORDINATE_2)
    assertThat(session.redoVertexStack).isEmpty()
  }

  @Test
  fun `commitTentativeVertex adds vertex to committed list`() {
    session.commitTentativeVertex(COORDINATE_1)
    assertThat(session.vertices).containsExactly(COORDINATE_1)
  }

  @Test
  fun `checkVertexIntersection detects intersection and drops vertex`() {
    session.setVertices(listOf(C1, C2, C3, C4))
    assertThat(session.checkVertexIntersection()).isTrue()
    assertThat(session.vertices).containsExactly(C1, C2, C3).inOrder()
    assertThat(session.hasSelfIntersection).isFalse()
  }

  @Test
  fun `hasSelfIntersection updates dynamically when vertices change`() {
    session.setVertices(listOf(C1, C2, C3, C4))
    assertThat(session.hasSelfIntersection).isTrue()

    session.removeLastVertex()
    assertThat(session.hasSelfIntersection).isFalse()
  }

  @Test
  fun `validatePolygonCompletion returns false when size less than 3`() {
    session.setVertices(listOf(C1, C2))
    assertThat(session.validatePolygonCompletion()).isFalse()
  }

  @Test
  fun `validatePolygonCompletion returns false when self-intersecting`() {
    session.setVertices(listOf(C1, C2, C3, C4))
    assertThat(session.validatePolygonCompletion()).isFalse()
    assertThat(session.hasSelfIntersection).isTrue()
  }

  @Test
  fun `validatePolygonCompletion returns true when valid`() {
    session.setVertices(listOf(C1, C3, C2, C4))
    assertThat(session.validatePolygonCompletion()).isTrue()
    assertThat(session.hasSelfIntersection).isFalse()
  }

  companion object {
    private val COORDINATE_1 = Coordinates(10.0, 10.0)
    private val COORDINATE_2 = Coordinates(20.0, 20.0)
    private val COORDINATE_3 = Coordinates(30.0, 30.0)
    private val COORDINATE_4 = Coordinates(40.0, 40.0)

    private val C1 = Coordinates(0.0, 0.0)
    private val C2 = Coordinates(10.0, 10.0)
    private val C3 = Coordinates(10.0, 0.0)
    private val C4 = Coordinates(0.0, 10.0)
  }
}
