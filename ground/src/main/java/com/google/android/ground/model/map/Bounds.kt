package com.google.android.ground.model.map

import com.google.android.ground.model.geometry.Coordinate

/**
 * Represents a rectangular bound on a map. A bounds may be constructed using only southwest and
 * northeast coordinates.
 */
data class Bounds(val southwest: Coordinate, val northeast: Coordinate)
