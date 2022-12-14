package com.google.android.ground.ui.common

import com.google.android.ground.rx.annotations.Cold
import io.reactivex.Completable

abstract class AbstractPhotoViewModel : AbstractViewModel() {
  abstract fun clearPhoto(taskId: String)

  abstract fun obtainCapturePhotoPermissions(): @Cold Completable

  abstract fun obtainSelectPhotoPermissions(): @Cold Completable
}