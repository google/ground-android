package com.google.android.ground.ui.editsubmission

import android.graphics.Bitmap

data class PhotoResult(
  val taskId: String,
  val bitmap: Bitmap?,
  val path: String?,
  var isHandled: Boolean = false
) {
  fun isEmpty(): Boolean = bitmap == null && path.isNullOrEmpty()
}