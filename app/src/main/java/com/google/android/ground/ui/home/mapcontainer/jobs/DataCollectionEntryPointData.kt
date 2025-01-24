package com.google.android.ground.ui.home.mapcontainer.jobs

import com.google.android.ground.model.job.Job
import com.google.android.ground.model.locationofinterest.LocationOfInterest

/** Data classes used to populate the Map cards (either an Loi card, or a Suggest Loi card). */
sealed interface DataCollectionEntryPointData {

  data class SelectedLoiSheetData(val loi: LocationOfInterest) : DataCollectionEntryPointData

  data class AdHocDataCollectionButtonData(val job: Job) : DataCollectionEntryPointData
}