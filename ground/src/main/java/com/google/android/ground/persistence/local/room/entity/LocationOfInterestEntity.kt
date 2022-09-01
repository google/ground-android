/*
 * Copyright 2019 Google LLC
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
package com.google.android.ground.persistence.local.room.entity

import androidx.room.*
import com.google.android.ground.model.AuditInfo
import com.google.android.ground.model.Survey
import com.google.android.ground.model.geometry.*
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.locationofinterest.LocationOfInterestType
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.persistence.local.LocalDataConsistencyException
import com.google.android.ground.persistence.local.room.entity.AuditInfoEntity.Companion.fromObject
import com.google.android.ground.persistence.local.room.entity.AuditInfoEntity.Companion.toObject
import com.google.android.ground.persistence.local.room.models.Coordinates
import com.google.android.ground.persistence.local.room.models.EntityState
import com.google.android.ground.util.toImmutableList
import com.google.common.collect.ImmutableList
import com.google.common.reflect.TypeToken
import com.google.gson.Gson

/**
 * Defines how Room persists LOIs in the local db. By default, Room uses the name of object fields
 * and their respective types to determine database column names and types.
 */
@Entity(tableName = "location_of_interest", indices = [Index("survey_id")])
data class LocationOfInterestEntity(
  @ColumnInfo(name = "id") @PrimaryKey val id: String,
  @ColumnInfo(name = "survey_id") val surveyId: String,
  @ColumnInfo(name = "job_id") val jobId: String,
  @ColumnInfo(name = "geo_json") val geoJson: String?,
  @ColumnInfo(name = "polygon_vertices") val polygonVertices: String?,
  @Embedded val location: Coordinates?,
  @ColumnInfo(name = "state") val state: EntityState, // TODO: Rename to DeletionState.
  @Embedded(prefix = "created_") val created: AuditInfoEntity,
  @Embedded(prefix = "modified_") val lastModified: AuditInfoEntity,
) {

  companion object {
    fun fromMutation(
      mutation: LocationOfInterestMutation,
      created: AuditInfo
    ): LocationOfInterestEntity {
      val authInfo = fromObject(created)
      return LocationOfInterestEntity(
        id = mutation.locationOfInterestId,
        surveyId = mutation.surveyId,
        jobId = mutation.jobId,
        state = EntityState.DEFAULT,
        created = authInfo,
        lastModified = authInfo,
        location = mutation.location.map { Coordinates.fromPoint(it) }.orElse(null),
        polygonVertices = formatVertices(mutation.polygonVertices),
        geoJson = null
      )
    }

    fun fromLocationOfInterest(locationOfInterest: LocationOfInterest): LocationOfInterestEntity {
      var location: Coordinates? = null
      var polygonVertices: String? = null

      when (locationOfInterest.type) {
        LocationOfInterestType.POINT ->
          location = Coordinates.fromPoint(locationOfInterest.geometry.vertices[0])
        // TODO(#1247): Add support for storing holes in the DB.
        else -> polygonVertices = formatVertices(locationOfInterest.geometry.vertices)
      }

      return LocationOfInterestEntity(
        id = locationOfInterest.id,
        surveyId = locationOfInterest.surveyId,
        jobId = locationOfInterest.job.id,
        state = EntityState.DEFAULT,
        created = fromObject(locationOfInterest.created),
        lastModified = fromObject(locationOfInterest.lastModified),
        location = location,
        polygonVertices = polygonVertices,
        geoJson = null
      )
    }

    fun toLocationOfInterest(
      locationOfInterestEntity: LocationOfInterestEntity,
      survey: Survey
    ): LocationOfInterest {
      if (locationOfInterestEntity.location != null) {
        return fillLocationOfInterest(
          locationOfInterestEntity,
          survey,
          locationOfInterestEntity.location.toPoint()
        )
      }
      if (locationOfInterestEntity.polygonVertices != null) {
        val points = parseVertices(locationOfInterestEntity.polygonVertices)
        val coordinates = points.map(Point::coordinate)
        val linearRing = LinearRing(coordinates)
        return fillLocationOfInterest(
          locationOfInterestEntity,
          survey,
          Polygon(linearRing, ImmutableList.of())
        )
      }
      throw LocalDataConsistencyException(
        "No geometry data found in location of interest " + locationOfInterestEntity.id
      )
    }

    @JvmStatic
    fun formatVertices(vertices: ImmutableList<Point>): String? {
      if (vertices.isEmpty()) {
        return null
      }
      val gson = Gson()
      val verticesArray =
        vertices
          .map { (coordinate): Point -> ImmutableList.of(coordinate.x, coordinate.y) }
          .toList()
      return gson.toJson(verticesArray)
    }

    @JvmStatic
    fun parseVertices(vertices: String?): ImmutableList<Point> {
      if (vertices == null || vertices.isEmpty()) {
        return ImmutableList.of()
      }
      val gson = Gson()
      val verticesArray =
        gson.fromJson<List<List<Double>>>(
          vertices,
          object : TypeToken<List<List<Double?>?>?>() {}.type
        )
      return verticesArray
        .map { vertex: List<Double> -> Point(Coordinate(vertex[0], vertex[1])) }
        .toImmutableList()
    }

    private fun fillLocationOfInterest(
      locationOfInterestEntity: LocationOfInterestEntity,
      survey: Survey,
      geometry: Geometry?
    ): LocationOfInterest {
      val id = locationOfInterestEntity.id
      val jobId = locationOfInterestEntity.jobId
      val job =
        survey.getJob(jobId).orElseThrow {
          LocalDataConsistencyException("Unknown jobId $jobId in location of interest $id")
        }
      return LocationOfInterest(
        id = id,
        surveyId = survey.id,
        job = job,
        created = toObject(locationOfInterestEntity.created),
        lastModified = toObject(locationOfInterestEntity.lastModified),
        geometry = geometry!!
      )
    }
  }
}
