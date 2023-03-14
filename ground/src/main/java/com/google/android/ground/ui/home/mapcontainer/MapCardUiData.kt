package com.google.android.ground.ui.home.mapcontainer

import com.google.android.ground.model.locationofinterest.LocationOfInterest

/** Data classes used to populate the Map cards (either an Loi card, or a Suggest Loi card). */
sealed interface MapCardUiData {

  data class LoiCardUiData(val loi: LocationOfInterest) : MapCardUiData
}
