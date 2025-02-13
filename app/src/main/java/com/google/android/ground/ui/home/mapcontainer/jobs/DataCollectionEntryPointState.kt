package com.google.android.ground.ui.home.mapcontainer.jobs

/**
 * Represents the state of the Data Collection Entry Point screen.
 *
 * This data class holds the different UI states and data related to displaying the data collection
 * entry points where users choose how to start a data collection process (e.g., selecting an
 * existing LOI or creating a new one).
 *
 * @property selectedLoiSheetData Data representing the currently selected LOI. Null if no LOI is
 *   selected. Contains information needed to begin collecting data for a pre-existing LOI.
 * @property newLoiJobCardDataList A list of [AdHocDataCollectionButtonData] representing the adhoc
 *   jobs for creating a new LOI and collect data for it. Each item represents a card that, when
 *   clicked, initiates the process of creating a new LOI and collecting data for it.
 * @property showLoiSheet Boolean indicating whether the [LoiJobSheet] should be displayed.
 * @property showNewLoiJobSelectionModal Boolean indicating whether the [JobSelectionModal] should
 *   be displayed.
 */
data class DataCollectionEntryPointState(
  val selectedLoiSheetData: SelectedLoiSheetData? = null,
  val newLoiJobCardDataList: List<AdHocDataCollectionButtonData> = emptyList(),
  val showLoiSheet: Boolean = false,
  val showNewLoiJobSelectionModal: Boolean = false,
)
