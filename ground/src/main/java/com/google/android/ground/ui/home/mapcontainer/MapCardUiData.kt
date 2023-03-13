package com.google.android.ground.ui.home.mapcontainer

/** Data used to populate the Map cards (either an Loi card, or a Suggest Loi card). */
data class MapCardUiData(
  val name: String,
  val subtitle: String?,
  val surveyId: String,
  val submissionText: String? = null,
  val loiId: String? = null
)
