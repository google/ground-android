package com.google.android.ground.ui.editsubmission

import android.graphics.Bitmap

/** Contains the bitmap or path to the photo a user captured or selected. */
data class PhotoResult @JvmOverloads constructor(
  val taskId: String,
  val bitmap: Bitmap? = null,
  val path: String? = null,
  var isHandled: Boolean = false
) {
  fun isEmpty(): Boolean = bitmap == null && path == null
}