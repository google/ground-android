package com.google.android.ground.ui.home.mapcontainer

import androidx.annotation.StringRes
import com.google.android.ground.proto.Survey
import com.google.android.ground.ui.home.mapcontainer.jobs.DataCollectionEntryPointData

sealed interface HomeScreenMapContainerEvent {

  data class ShowDataSharingTermsDialog(
    val data: DataCollectionEntryPointData,
    val dataSharingTerms: Survey.DataSharingTerms,
  ) : HomeScreenMapContainerEvent

  data class NavigateToDataCollectionFragment(val data: DataCollectionEntryPointData) :
    HomeScreenMapContainerEvent

  data class ShowErrorToast(@StringRes val messageResId: Int) : HomeScreenMapContainerEvent
}
