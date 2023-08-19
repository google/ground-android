package com.google.android.ground.ui.common

import com.google.android.ground.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PermissionDeniedDialogViewModel
@Inject
internal constructor(private val navigator: Navigator, private val userRepository: UserRepository) :
  AbstractViewModel() {

  fun closeApp() {
    navigator.finishApp()
  }

  fun signOut() {
    userRepository.signOut()
  }
}
