package com.google.android.ground

import android.os.Looper
import com.google.android.ground.system.ApplicationErrorManager
import com.google.android.ground.system.auth.SignInState
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED
import com.sharedtest.system.auth.FakeAuthenticationManager
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowProgressDialog
import org.robolectric.shadows.ShadowToast.getTextOfLatestToast

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class MainActivityTest : BaseHiltTest() {

  private lateinit var activity: MainActivity
  @Inject lateinit var errorManager: ApplicationErrorManager
  @Inject lateinit var fakeAuthenticationManager: FakeAuthenticationManager

  override fun setUp() {
    super.setUp()
    ShadowProgressDialog.reset()
  }

  @Test
  fun handleErrorManager_activityFinish() {
    Robolectric.buildActivity(MainActivity::class.java).use { controller ->
      controller.setup() // Moves Activity to RESUMED state
      activity = controller.get()

      errorManager.handleException(
        FirebaseFirestoreException("Permission denied", PERMISSION_DENIED)
      )
      shadowOf(Looper.getMainLooper()).idle()

      assertThat(getTextOfLatestToast()).isEqualTo("Permission denied! Check user pass-list.")
      assertThat(activity.isFinishing).isTrue()
    }
  }

  @Test
  fun signInProgressDialog_whenSigningIn_isDisplayed() {
    Robolectric.buildActivity(MainActivity::class.java).use { controller ->
      controller.setup() // Moves Activity to RESUMED state
      activity = controller.get()

      fakeAuthenticationManager.setState(SignInState.signingIn())
      shadowOf(Looper.getMainLooper()).idle()

      assertThat(ShadowProgressDialog.getLatestDialog().isShowing).isTrue()
    }
  }

  @Test
  fun signInProgressDialog_whenSignedOutAfterSignInState_isNotDisplayed() {
    Robolectric.buildActivity(MainActivity::class.java).use { controller ->
      controller.setup() // Moves Activity to RESUMED state
      activity = controller.get()

      fakeAuthenticationManager.setState(SignInState.signingIn())
      fakeAuthenticationManager.setState(SignInState.signedOut())
      shadowOf(Looper.getMainLooper()).idle()

      assertThat(ShadowProgressDialog.getLatestDialog().isShowing).isFalse()
    }
  }
}
