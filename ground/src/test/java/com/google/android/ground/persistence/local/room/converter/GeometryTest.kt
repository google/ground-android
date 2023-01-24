package com.google.android.ground.persistence.local.room.converter

import com.google.android.ground.model.geometry.*
import com.google.android.ground.persistence.remote.firebase.schema.Path
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GeometryTest {
  private val x = -42.121
  private val y = 28.482
  private val path1 =
    arrayOf(
      -89.63410225 to 41.89729784,
      -89.63805046 to 41.89525340,
      -89.63659134 to 41.88937530,
      -89.62886658 to 41.88956698,
      -89.62800827 to 41.89544507,
      -89.63410225 to 41.89729784
    )
  private val path2 =
    arrayOf(
      -89.63453141 to 41.89193106,
      -89.63118400 to 41.89090878,
      -89.63066902 to 41.89397560,
      -89.63358726 to 41.89480618,
      -89.63453141 to 41.89193106
    )
  private val path3 =
    arrayOf(
      -89.61006966 to 41.89333669,
      -89.61479034 to 41.89832003,
      -89.61719360 to 41.89455062,
      -89.61521950 to 41.89154771,
      -89.61006966 to 41.89333669
    )
  private val path4 =
    arrayOf(
      -89.61393204 to 41.89320891,
      -89.61290207 to 41.89429505,
      -89.61418953 to 41.89538118,
      -89.61513367 to 41.89416727,
      -89.61393204 to 41.89320891
    )

  @Test
  fun testPointSerialization() {
    val point = point(x, y)

    assertThat(point).isEqualTo(point.toLocalDataStoreObject().getGeometry())
  }

  @Test
  fun testPolygonSerialization() {
    val polygon = polygon(path1, path2)

    assertThat(polygon).isEqualTo(polygon.toLocalDataStoreObject().getGeometry())
  }

  @Test
  fun testMultiPolygonSerialization() {
    val multiPolygon = multiPolygon(polygon(path1, path2), polygon(path3, path4))

    assertThat(multiPolygon).isEqualTo(multiPolygon.toLocalDataStoreObject().getGeometry())
  }

  private fun point(x: Double, y: Double) = Point(Coordinate(x, y))

  private fun linearRing(path: Path) = LinearRing(toCoordinateList(path))

  private fun polygon(shell: Path, vararg holes: Path) =
    Polygon(linearRing(shell), holes.map(::linearRing))

  private fun multiPolygon(vararg polygons: Polygon) = MultiPolygon(polygons.asList())

  private fun toCoordinateList(path: Path): List<Coordinate> =
    path.map { Coordinate(it.first, it.second) }
}