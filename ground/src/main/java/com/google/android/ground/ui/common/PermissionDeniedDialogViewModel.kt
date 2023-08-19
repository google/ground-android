package com.google.android.ground.ui.common

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PermissionDeniedDialogViewModel
@Inject
internal constructor(private val navigator: Navigator) : AbstractViewModel() {

  fun closeApp() {
    navigator.finishApp()
  }

  fun signOut() {
    navigator.navigate(PermissionDeniedDialogFragmentDirections.showSignInScreen())
  }
}
