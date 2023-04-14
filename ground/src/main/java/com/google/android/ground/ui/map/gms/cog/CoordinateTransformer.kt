package com.google.android.ground.ui.map.gms.cog

import com.google.android.gms.maps.model.LatLng
import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateTransformFactory
import org.locationtech.proj4j.ProjCoordinate

object CoordinateTransformer {
  private val crsFactory = CRSFactory()
  private val webMercator = crsFactory.createFromName("epsg:3857")
  private val wgs84 = crsFactory.createFromName("epsg:4326")
  private val ctFactory = CoordinateTransformFactory()
  private val webMercatorToWgs84 = ctFactory.createTransform(webMercator, wgs84)

  fun webMercatorToWgs84(x: Double, y: Double): LatLng {
    val result = webMercatorToWgs84.transform(ProjCoordinate(x, y), ProjCoordinate())
    return LatLng(result.y, result.x)
  }
}