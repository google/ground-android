/*
 * Copyright 2022 Google LLC
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
package com.google.android.ground.converter

import com.google.android.ground.model.geometry.*
import com.google.android.ground.persistence.local.room.entity.GeometryEntity
import com.google.android.ground.persistence.local.room.models.Coordinates
import com.google.android.ground.persistence.local.room.models.GeometryType
import com.google.android.ground.util.toImmutableList
import com.google.common.collect.ImmutableList
import com.google.common.reflect.TypeToken
import com.google.gson.Gson

/** Converts [Geometry] model objects to/from a persisted database representation. */
object GeometryModelToLocalDbConverter : Converter<Geometry, GeometryEntity> {
  override fun convertTo(model: Geometry): GeometryEntity? =
    when (model) {
      is Point -> fromPointModel(model)
      is Polygon -> fromPolygonModel(model)
      else -> null
    }

  override fun convertFrom(entity: GeometryEntity): Geometry? =
    when (entity.geometryType) {
      GeometryType.POINT.name -> toPointModel(entity)
      GeometryType.POLYGON.name -> toPolygonModel(entity)
      else -> null
    }

  private fun toPointModel(record: GeometryEntity): Geometry? = record.location?.toPoint()

  private fun toPolygonModel(record: GeometryEntity): Geometry {
    val shell = LinearRing(parseVertices(record.vertices).map { it.coordinate })
    val holes = parseHoles(record.holes).map { LinearRing(it.map(Point::coordinate)) }

    return Polygon(shell, holes)
  }

  private fun fromPointModel(point: Point): GeometryEntity =
    GeometryEntity(GeometryType.POINT.name, Coordinates.fromPoint(point))

  private fun fromPolygonModel(polygon: Polygon): GeometryEntity {
    val shell = formatVertices(polygon.vertices)
    val holes = formatHoles(polygon.holes.map { it.vertices })

    return GeometryEntity(GeometryType.POLYGON.name, null, shell, holes)
  }

  private fun formatHoles(holes: List<ImmutableList<Point>>): String? {
    if (holes.isEmpty()) {
      return null
    }

    val gson = Gson()
    val holeArray =
      holes.map { hole -> hole.map { ImmutableList.of(it.coordinate.x, it.coordinate.y) } }

    return gson.toJson(holeArray)
  }

  fun formatVertices(vertices: ImmutableList<Point>): String? {
    if (vertices.isEmpty()) {
      return null
    }
    val gson = Gson()
    val verticesArray =
      vertices.map { (coordinate): Point -> ImmutableList.of(coordinate.x, coordinate.y) }.toList()
    return gson.toJson(verticesArray)
  }

  private fun parseHoles(holes: String?): List<ImmutableList<Point>> {
    if (holes.isNullOrEmpty()) {
      return ImmutableList.of()
    }

    val gson = Gson()
    val holesArray =
      gson.fromJson<List<List<List<Double>>>>(
        holes,
        object : TypeToken<List<List<List<Double?>?>?>?>() {}.type
      )

    return holesArray.map {
      it.map { vertex -> Point(Coordinate(vertex[0], vertex[1])) }.toImmutableList()
    }
  }

  fun parseVertices(vertices: String?): ImmutableList<Point> {
    if (vertices.isNullOrEmpty()) {
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
}
