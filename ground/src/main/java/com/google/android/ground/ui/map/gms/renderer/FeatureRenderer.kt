package com.google.android.ground.ui.map.gms.renderer

import com.google.android.ground.ui.map.Feature

sealed interface FeatureRenderer {
  fun removeStaleFeatures(features: Set<Feature>)
  fun removeAllFeatures()
}
