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
import org.groundplatform.domain.model.geometry.Coordinates
import org.junit.Before
import org.junit.Test

class PolygonDrawingSessionTest {

  private lateinit var session: PolygonDrawingSessionImpl

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
  fun `setVertices updates vertices list`() {
    val newVertices = listOf(COORDINATE_1, COORDINATE_2)
    session.setVertices(newVertices)
    assertThat(session.vertices).containsExactlyElementsIn(newVertices).inOrder()
  }

  @Test
  fun `updateTentativeVertex adds vertex when empty`() {
    session.updateTentativeVertex(COORDINATE_1) { _, _ -> DISTANCE_ABOVE_THRESHOLD }
    assertThat(session.vertices).containsExactly(COORDINATE_1)
  }

  @Test
  fun `updateTentativeVertex overwrites last vertex`() {
    session.setVertices(listOf(COORDINATE_1))
    session.updateTentativeVertex(COORDINATE_2) { _, _ -> DISTANCE_ABOVE_THRESHOLD }
    assertThat(session.vertices).containsExactly(COORDINATE_2)
  }

  @Test
  fun `updateTentativeVertex snaps to first vertex when close`() {
    session.setVertices(listOf(COORDINATE_1, COORDINATE_2, COORDINATE_3))

    // Target is close to COORDINATE_1
    session.updateTentativeVertex(COORDINATE_4) { _, _ -> DISTANCE_BELOW_THRESHOLD }
    assertThat(session.vertices.last()).isEqualTo(COORDINATE_1)
  }

  @Test
  fun `updateTentativeVertex does not snap when far from first vertex`() {
    session.setVertices(listOf(COORDINATE_1, COORDINATE_2, COORDINATE_3))

    // Target is far from COORDINATE_1
    session.updateTentativeVertex(COORDINATE_4) { _, _ -> DISTANCE_ABOVE_THRESHOLD }
    assertThat(session.vertices.last()).isEqualTo(COORDINATE_4)
  }

  @Test
  fun `updateTentativeVertex sets isTooClose true when close to previous`() {
    session.setVertices(listOf(COORDINATE_1, COORDINATE_2))

    // Target is close to COORDINATE_2
    session.updateTentativeVertex(COORDINATE_3) { _, _ -> DISTANCE_BELOW_THRESHOLD }
    assertThat(session.state.isTooClose).isTrue()
  }

  @Test
  fun `updateTentativeVertex sets isTooClose false when far from previous`() {
    session.setVertices(listOf(COORDINATE_1, COORDINATE_2))

    // Target is far from COORDINATE_2
    session.updateTentativeVertex(COORDINATE_3) { _, _ -> DISTANCE_ABOVE_THRESHOLD }
    assertThat(session.state.isTooClose).isFalse()
  }

  @Test
  fun `commitTentativeVertex clears redo stack`() {
    session.setVertices(listOf(COORDINATE_1, COORDINATE_2))
    session.removeLastVertex()
    assertThat(session.redoVertexStack).isNotEmpty()

    session.commitTentativeVertex(COORDINATE_3)
    assertThat(session.redoVertexStack).isEmpty()
  }

  @Test
  fun `commitTentativeVertex adds vertex to committed list`() {
    session.commitTentativeVertex(COORDINATE_1)
    assertThat(session.vertices).containsExactly(COORDINATE_1)
  }

  @Test
  fun `commitTentativeVertex sets isTooClose true when size greater than 1`() {
    session.setVertices(listOf(COORDINATE_1, COORDINATE_2))
    session.commitTentativeVertex(COORDINATE_3)
    assertThat(session.state.isTooClose).isTrue()
  }

  @Test
  fun `commitTentativeVertex returns null when no vertex to commit`() {
    assertThat(session.commitTentativeVertex(null)).isNull()
  }

  @Test
  fun `removeLastVertex returns false when empty`() {
    assertThat(session.removeLastVertex()).isFalse()
  }

  @Test
  fun `removeLastVertex removes last vertex and adds to redo stack`() {
    session.setVertices(listOf(COORDINATE_1, COORDINATE_2))

    assertThat(session.removeLastVertex()).isTrue()
    assertThat(session.vertices).containsExactly(COORDINATE_1)
    assertThat(session.redoVertexStack).containsExactly(COORDINATE_2)
  }

  @Test
  fun `removeLastVertex clears redo stack when vertices become empty`() {
    session.setVertices(listOf(COORDINATE_1))

    assertThat(session.removeLastVertex()).isTrue()
    assertThat(session.vertices).isEmpty()
    assertThat(session.redoVertexStack).isEmpty()
  }

  @Test
  fun `redoLastVertex returns null when redo stack is empty`() {
    assertThat(session.redoLastVertex()).isNull()
  }

  @Test
  fun `redoLastVertex restores last removed vertex`() {
    session.setVertices(listOf(COORDINATE_1, COORDINATE_2))
    session.removeLastVertex()

    assertThat(session.redoLastVertex()).isEqualTo(COORDINATE_2)
    assertThat(session.vertices).containsExactly(COORDINATE_1, COORDINATE_2).inOrder()
    assertThat(session.redoVertexStack).isEmpty()
  }

  @Test
  fun `checkVertexIntersection detects intersection and drops vertex`() {
    session.setVertices(listOf(C1, C2, C3, C4))
    assertThat(session.checkVertexIntersection()).isTrue()
    assertThat(session.vertices).containsExactly(C1, C2, C3).inOrder()
    assertThat(session.hasSelfIntersection).isFalse()
  }

  @Test
  fun `checkVertexIntersection returns false when no intersection`() {
    session.setVertices(listOf(C1, C3, C2, C4)) // Valid square
    assertThat(session.checkVertexIntersection()).isFalse()
  }

  @Test
  fun `validatePolygonCompletion returns false when size less than 3`() {
    session.setVertices(listOf(C1, C2))
    assertThat(session.isValidPolygon()).isFalse()
  }

  @Test
  fun `validatePolygonCompletion returns false when self-intersecting`() {
    session.setVertices(listOf(C1, C2, C3, C4))
    assertThat(session.isValidPolygon()).isFalse()
    assertThat(session.hasSelfIntersection).isTrue()
  }

  @Test
  fun `validatePolygonCompletion returns true when valid`() {
    session.setVertices(listOf(C1, C3, C2, C4, C1))
    assertThat(session.isValidPolygon()).isTrue()
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
  fun `complete() throws when polygon is not closed`() {
    session.setVertices(listOf(C1, C2, C3))
    org.junit.Assert.assertThrows(IllegalStateException::class.java) { session.complete() }
  }

  @Test
  fun `complete() marks session as complete`() {
    session.setVertices(listOf(C1, C3, C2, C4, C1))
    session.complete()
    assertThat(session.state.isMarkedComplete).isTrue()
  }

  @Test
  fun `removeLastVertex resets completion state`() {
    session.setVertices(listOf(C1, C3, C2, C4, C1))
    session.complete()
    assertThat(session.state.isMarkedComplete).isTrue()

    session.removeLastVertex()
    assertThat(session.state.isMarkedComplete).isFalse()
  }

  @Test
  fun `redoLastVertex resets completion state`() {
    session.setVertices(listOf(C1, C3, C2, C4, C1))
    session.complete()
    session.removeLastVertex()

    session.redoLastVertex()
    assertThat(session.state.isMarkedComplete).isFalse()
  }

  companion object {
    private const val DISTANCE_BELOW_THRESHOLD = 20.0
    private const val DISTANCE_ABOVE_THRESHOLD = 30.0

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
