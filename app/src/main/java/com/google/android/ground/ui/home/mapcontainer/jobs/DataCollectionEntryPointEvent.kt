package com.google.android.ground.ui.home.mapcontainer.jobs

/**
 * Represents events that can occur within the data collection entry points. These events typically
 * signal user interactions or state changes related to starting, managing, or dismissing the data
 * collection entry points.
 */
sealed interface DataCollectionEntryPointEvent {

  data object DismissSelectedLoiJobSheet : DataCollectionEntryPointEvent

  data object ShowNewLoiJobSelectionModal : DataCollectionEntryPointEvent

  data object DismissNewLoiJobSelectionModal : DataCollectionEntryPointEvent

  data class StartDataCollection(val data: DataCollectionEntryPointData) :
    DataCollectionEntryPointEvent

  data class UpdateState(
    val selectedLoiSheetData: SelectedLoiSheetData?,
    val newLoiJobCardDataList: List<AdHocDataCollectionButtonData>,
  ) : DataCollectionEntryPointEvent
}
