package com.google.android.ground.model.geometry

/**
 * Indicates the type of a geometric model object. Used to interpret other objects (e.g. map
 * features) back into model objects.
 */
enum class ModelTypeTag {
  UNKNOWN,
  LOCATION_OF_INTEREST,
  USER_POINT,
  USER_POLYGON,
}
