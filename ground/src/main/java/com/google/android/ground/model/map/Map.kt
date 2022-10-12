package com.google.android.ground.model.map

import com.google.android.ground.model.geometry.Coordinate
import com.google.common.collect.ImmutableList

interface Map {
  val mapTypes: ImmutableList<MapType>
  val viewport: Bounds
  val zoom: Float
  var mapType: MapType

  fun moveCamera(coordinate: Coordinate)
  fun moveCamera(coordinate: Coordinate, zoom: Float)
  fun renderLocationOfInterest(locationOfInterest: MapLocationOfInterest)
}