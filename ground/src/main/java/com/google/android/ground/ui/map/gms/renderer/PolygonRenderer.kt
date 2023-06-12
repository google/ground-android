package com.google.android.ground.ui.map.gms.renderer

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.Polygon as MapsPolygon
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.ground.model.geometry.Geometry
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.FeatureType
import com.google.android.ground.ui.map.gms.toLatLng

class PolygonRenderer(
  map: GoogleMap,
  private val strokeWidth: Float,
  private val fillColor: Int,
  private val strokeColor: Int
) : FeatureRenderer(map) {

  private val polygons: MutableMap<Feature, MutableList<MapsPolygon>> = HashMap()

  override fun addFeature(feature: Feature, geometry: Geometry) {
    val polygon = geometry as Polygon
    val options = PolygonOptions()
    options.clickable(false)

    val shellVertices = polygon.shell.vertices.map { it.toLatLng() }
    options.addAll(shellVertices)

    val holes = polygon.holes.map { hole -> hole.vertices.map { point -> point.toLatLng() } }
    holes.forEach { options.addHole(it) }

    val mapsPolygon = map.addPolygon(options)
    mapsPolygon.tag = Pair(feature.tag.id, LocationOfInterest::javaClass)
    mapsPolygon.strokeWidth = strokeWidth
    mapsPolygon.fillColor = fillColor
    mapsPolygon.strokeColor = strokeColor
    mapsPolygon.strokeJointType = JointType.ROUND

    polygons.getOrPut(feature) { mutableListOf() }.add(mapsPolygon)
  }

  fun getPolygonsWithLoi(): Map<Feature, MutableList<MapsPolygon>> {
    return polygons.filter { it.key.tag.type == FeatureType.LOCATION_OF_INTEREST.ordinal }
  }

  override fun removeStaleFeatures(features: Set<Feature>) {
    val deletedIds = polygons.keys.map { it.tag.id } - features.map { it.tag.id }.toSet()
    val deletedPolygons = polygons.filter { deletedIds.contains(it.key.tag.id) }
    deletedPolygons.values.forEach { it.forEach(MapsPolygon::remove) }
    polygons.minusAssign(deletedPolygons.keys)
  }

  override fun removeAllFeatures() {
    polygons.values.forEach { it.forEach(MapsPolygon::remove) }
    polygons.clear()
  }
}
