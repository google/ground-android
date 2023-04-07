/*
 * Copyright 2021 Google LLC
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
package com.google.android.ground.ui.datacollection.tasks.polygon

import android.content.res.Resources
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import com.google.android.ground.model.geometry.LinearRing
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.persistence.uuid.OfflineUuidGenerator
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.common.SharedViewModel
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskViewModel
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.FeatureType
import io.reactivex.BackpressureStrategy
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList

@SharedViewModel
class PolygonDrawingViewModel
@Inject
internal constructor(private val uuidGenerator: OfflineUuidGenerator, resources: Resources) :
  AbstractTaskViewModel(resources) {
  private val partialPolygonFlowable: @Hot Subject<Polygon> = PublishSubject.create()

  val polygonLiveData: @Hot LiveData<Polygon>

  /** [Feature]s drawn by the user but not yet saved. */
  val features: @Hot LiveData<Set<Feature>>

  /** Represents the current state of polygon that is being drawn. */
  private var polygon: Polygon = Polygon.EMPTY_POLYGON

  /** Represents whether the user has completed drawing the polygon or not. */
  private var isMarkedComplete: Boolean = false

  init {
    val polygonFlowable = partialPolygonFlowable.toFlowable(BackpressureStrategy.LATEST).share()
    polygonLiveData = LiveDataReactiveStreams.fromPublisher(polygonFlowable)
    features = LiveDataReactiveStreams.fromPublisher(polygonFlowable.map { createFeatures(it) })
  }

  /**
   * Adds another vertex at the given point if the distance is pixels between first vertex and new
   * target is more than the configured threshold. Otherwise, snaps to the first vertex.
   */
  fun addVertexAndMaybeCompletePolygon(
    newTarget: Point,
    calculateDistanceInPixels: (point1: Point, point2: Point) -> Double
  ) {
    if (isMarkedComplete) return

    val firstVertex = polygon.firstVertex
    var updatedTarget: Point = newTarget

    if (firstVertex != null && polygon.size > 2) {
      val distance = calculateDistanceInPixels(firstVertex, newTarget)
      if (distance <= DISTANCE_THRESHOLD_DP) {
        updatedTarget = firstVertex
      }
    }

    addVertex(updatedTarget, true)
  }

  override fun clearResponse() {
    removeLastVertex()
  }

  /** Attempts to remove the last vertex of drawn polygon, if any. */
  fun removeLastVertex() {
    // Do nothing if there are no vertices to remove.
    if (polygon.isEmpty) return

    // Reset complete status
    isMarkedComplete = false

    // Remove last vertex and update polygon
    val updatedVertices = polygon.vertices.toMutableList()
    updatedVertices.removeLast()
    updatePolygon(updatedVertices.toImmutableList())
  }

  /** Adds the last vertex to the polygon. */
  fun addLastVertex() = polygon.lastVertex?.let { addVertex(it, false) }

  /**
   * Adds a new vertex to the polygon.
   *
   * @param vertex
   * @param shouldOverwriteLastVertex
   */
  private fun addVertex(vertex: Point, shouldOverwriteLastVertex: Boolean) {
    val updatedVertices = polygon.vertices.toMutableList()

    // Maybe remove the last vertex before adding the new vertex.
    if (shouldOverwriteLastVertex && updatedVertices.isNotEmpty()) {
      updatedVertices.removeLast()
    }

    // Add the new vertex
    updatedVertices.add(vertex)

    // Render changes to UI
    updatePolygon(updatedVertices.toImmutableList())
  }

  private fun updatePolygon(newVertices: List<Point>) {
    val polygon = Polygon(LinearRing(newVertices.map { point -> point.coordinate }))
    partialPolygonFlowable.onNext(polygon)
    this.polygon = polygon
  }

  fun onCompletePolygonButtonClick() {
    check(polygon.isComplete) { "Polygon is not complete" }
    isMarkedComplete = true
    // TODO: Serialize the polygon and update response
  }

  /** Returns a set of [Feature] to be drawn on map for the given [Polygon]. */
  private fun createFeatures(polygon: Polygon): Set<Feature> {
    if (polygon.isEmpty) {
      return setOf()
    }

    return setOf(
      Feature(
        id = uuidGenerator.generateUuid(),
        type = FeatureType.USER_POLYGON.ordinal,
        geometry = polygon
      )
    )
  }

  companion object {
    /** Min. distance in dp between two points for them be considered as overlapping. */
    const val DISTANCE_THRESHOLD_DP = 24
  }
}
