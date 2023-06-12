package com.google.android.ground.ui.map.gms.renderer

import com.google.android.gms.maps.GoogleMap
import com.google.android.ground.model.geometry.Geometry
import com.google.android.ground.ui.map.Feature

sealed class FeatureRenderer(val map: GoogleMap) {
  abstract fun addFeature(feature: Feature, geometry: Geometry)
  abstract fun removeStaleFeatures(features: Set<Feature>)
  abstract fun removeAllFeatures()
}
