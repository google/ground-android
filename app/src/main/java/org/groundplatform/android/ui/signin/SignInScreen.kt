/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.groundplatform.android.ui.signin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_CANCELLED
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import org.groundplatform.android.BuildConfig
import org.groundplatform.android.R
import org.groundplatform.android.system.auth.SignInState
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.components.LoadingDialog
import org.groundplatform.android.ui.components.PermissionDeniedDialog
import org.groundplatform.android.ui.theme.AppTheme
import org.groundplatform.android.util.isPermissionDeniedException

const val BUTTON_TEST_TAG = "google_sign_in_button"

/**
 * Displays the sign-in screen, handling network connectivity status and authentication state.
 *
 * @param viewModel the view model used to manage sign-in state and network connectivity.
 */
@Composable
fun SignInScreen(onCloseApp: () -> Unit, viewModel: SignInViewModel = hiltViewModel()) {
  val connected by viewModel.networkAvailable.collectAsStateWithLifecycle()
  val signInState by viewModel.signInState.collectAsStateWithLifecycle()

  SignInContent(
    connected = connected,
    signInState = signInState,
    onSignInClick = viewModel::onSignInButtonClick,
    onSignOutClick = viewModel::onSignOutButtonClick,
    onCloseApp = onCloseApp,
  )
}

@Composable
private fun SignInContent(
  connected: Boolean,
  signInState: SignInState,
  onSignInClick: () -> Unit,
  onSignOutClick: () -> Unit,
  onCloseApp: () -> Unit,
) {
  val snackbarHostState = remember { SnackbarHostState() }
  val networkErrorMessage = stringResource(R.string.network_error_when_signing_in)

  LaunchedEffect(connected) {
    if (!connected) {
      snackbarHostState.showSnackbar(networkErrorMessage)
    }
  }

  if (signInState is SignInState.SigningIn) {
    LoadingDialog(R.string.signing_in)
  } else if (signInState is SignInState.Error) {
    SignInErrorUi(signInState, onSignOutClick, onCloseApp, snackbarHostState)
  }

  Box(modifier = Modifier.fillMaxSize()) {

    // Background image
    Image(
      painter = painterResource(id = R.drawable.splash_background),
      modifier = Modifier.fillMaxSize(),
      contentScale = ContentScale.Crop,
      contentDescription = null,
    )

    BackgroundOverlay()

    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceAround,
    ) {
      LogoAndTitle()
      GoogleSignInButton(enabled = connected && signInState.shouldAllowSignIn()) { onSignInClick() }
    }

    SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
  }
}

@Composable
private fun SignInErrorUi(
  signInState: SignInState.Error,
  onSignOutClick: () -> Unit,
  onCloseApp: () -> Unit,
  snackbarHostState: SnackbarHostState,
) {
  when {
    signInState.error.isPermissionDeniedException() -> {
      PermissionDeniedDialog(
        // TODO: Read url from Firestore config/properties/signUpUrl
        // Issue URL: https://github.com/google/ground-android/issues/2402
        signupLink = BuildConfig.SIGNUP_FORM_LINK,
        onSignOut = onSignOutClick,
        onCloseApp = onCloseApp,
      )
    }

    signInState.error.isSignInCancelledException() -> {
      /* Do nothing, as this was a user choice, not a system error. */
    }

    // For any other type of error, show a generic message.
    else -> {
      val unknownErrorMessage = stringResource(R.string.something_went_wrong)
      LaunchedEffect(signInState.error) { snackbarHostState.showSnackbar(unknownErrorMessage) }
    }
  }
}

private fun Throwable.isSignInCancelledException() =
  this is ApiException && statusCode == SIGN_IN_CANCELLED

@Composable
private fun BackgroundOverlay(modifier: Modifier = Modifier) {
  Column(modifier.fillMaxSize().background(color = Color(0x66146C2E))) {
    Spacer(Modifier.weight(1f))
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .weight(1f)
          .background(
            brush = Brush.verticalGradient(colors = listOf(Color(0x00006E2C), Color(0xFF003917)))
          )
    )
  }
}

@Composable
private fun LogoAndTitle(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Image(
      painter = painterResource(id = R.drawable.ground_logo),
      modifier = Modifier.size(120.dp),
      contentDescription = null,
    )
    Text(
      text = stringResource(id = R.string.app_name),
      color = Color.White,
      fontSize = 60.sp,
      fontFamily = FontFamily(Font(R.font.display_500)),
      letterSpacing = 0.6.sp,
    )
  }
}

@Composable
private fun GoogleSignInButton(
  enabled: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier.wrapContentSize().testTag(BUTTON_TEST_TAG),
    colors =
      ButtonDefaults.buttonColors(
        containerColor = Color.White,
        contentColor = Color.DarkGray,
        disabledContainerColor = Color.LightGray,
        disabledContentColor = Color.Gray,
      ),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        painter = painterResource(id = R.drawable.ic_google_logo),
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        tint = Color.Unspecified,
      )
      Spacer(modifier = Modifier.padding(horizontal = 8.dp))
      Text(text = stringResource(id = R.string.sign_in_with_google), fontSize = 16.sp)
    }
  }
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun SignInScreenSignedOutPreview() {
  AppTheme {
    SignInContent(
      connected = true,
      signInState = SignInState.SignedOut,
      onSignInClick = {},
      onSignOutClick = {},
      onCloseApp = {},
    )
  }
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun SignInScreenSigningInPreview() {
  AppTheme {
    SignInContent(
      connected = true,
      signInState = SignInState.SigningIn,
      onSignInClick = {},
      onSignOutClick = {},
      onCloseApp = {},
    )
  }
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun SignInScreenNotConnectedPreview() {
  AppTheme {
    SignInContent(
      connected = false,
      signInState = SignInState.SignedOut,
      onSignInClick = {},
      onSignOutClick = {},
      onCloseApp = {},
    )
  }
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun SignInScreenPermissionDeniedErrorPreview() {
  AppTheme {
    val error = FirebaseFirestoreException("Permission denied", Code.PERMISSION_DENIED)
    SignInContent(
      connected = true,
      signInState = SignInState.Error(error),
      onSignInClick = {},
      onSignOutClick = {},
      onCloseApp = {},
    )
  }
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun SignInScreenUserCancelledErrorPreview() {
  AppTheme {
    val error = ApiException(Status(SIGN_IN_CANCELLED))
    SignInContent(
      connected = true,
      signInState = SignInState.Error(error),
      onSignInClick = {},
      onSignOutClick = {},
      onCloseApp = {},
    )
  }
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun SignInScreenUnknownErrorPreview() {
  AppTheme {
    SignInContent(
      connected = true,
      signInState = SignInState.Error(Error("Unknown error")),
      onSignInClick = {},
      onSignOutClick = {},
      onCloseApp = {},
    )
  }
}
