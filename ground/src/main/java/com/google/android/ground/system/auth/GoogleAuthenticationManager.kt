/*
 * Copyright 2020 Google LLC
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
package com.google.android.ground.system.auth

import android.app.Activity
import android.content.res.Resources
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.ground.R
import com.google.android.ground.coroutines.ApplicationScope
import com.google.android.ground.model.User
import com.google.android.ground.system.ActivityResult
import com.google.android.ground.system.ActivityStreams
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import timber.log.Timber

private val signInRequestCode = AuthenticationManager::class.java.hashCode() and 0xffff

class GoogleAuthenticationManager
@Inject
constructor(
  resources: Resources,
  private val activityStreams: ActivityStreams,
  private val firebaseAuth: FirebaseAuth,
  @ApplicationScope private val externalScope: CoroutineScope,
) : BaseAuthenticationManager() {

  private val googleSignInOptions: GoogleSignInOptions =
    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
      .requestIdToken(resources.getString(R.string.default_web_client_id))
      .requestEmail()
      .requestProfile()
      .build()

  init {
    externalScope.launch {
      activityStreams.getActivityResults(signInRequestCode).collect { onActivityResult(it) }
    }
  }

  private val _signInStateFlow = MutableStateFlow<SignInState?>(null)
  override val signInState: Flow<SignInState> = _signInStateFlow.asStateFlow().filterNotNull()

  override fun initInternal() {
    firebaseAuth.addAuthStateListener { auth ->
      val user = auth.currentUser?.toUser()
      setState(if (user == null) SignInState.signedOut() else SignInState.signedIn(user))
    }
  }

  private fun setState(nextState: SignInState) {
    externalScope.launch { _signInStateFlow.emit(nextState) }
  }

  override fun signIn() {
    setState(SignInState.signingIn())
    showSignInDialog()
  }

  private fun showSignInDialog() =
    activityStreams.withActivity {
      val signInIntent = getGoogleSignInClient(it).signInIntent
      it.startActivityForResult(signInIntent, signInRequestCode)
    }

  override fun signOut() {
    firebaseAuth.signOut()
    setState(SignInState.signedOut())
    activityStreams.withActivity { getGoogleSignInClient(it).signOut() }
  }

  private fun getGoogleSignInClient(activity: Activity): GoogleSignInClient =
    // TODO: Use app context instead of activity?
    GoogleSignIn.getClient(activity, googleSignInOptions)

  private fun onActivityResult(activityResult: ActivityResult) {
    // The Task returned from getSignedInAccountFromIntent is always completed, so no need to
    // attach a listener.
    try {
      val googleSignInTask = GoogleSignIn.getSignedInAccountFromIntent(activityResult.data)
      googleSignInTask.getResult(ApiException::class.java)?.let { onGoogleSignIn(it) }
    } catch (e: ApiException) {
      Timber.e(e, "Sign in failed")
      setState(SignInState.error(e))
    }
  }

  private fun onGoogleSignIn(googleAccount: GoogleSignInAccount) =
    firebaseAuth
      .signInWithCredential(getFirebaseAuthCredential(googleAccount))
      .addOnSuccessListener { authResult: AuthResult -> onFirebaseAuthSuccess(authResult) }
      .addOnFailureListener { setState(SignInState.error(it)) }

  private fun onFirebaseAuthSuccess(authResult: AuthResult) {
    setState(SignInState.signedIn(authResult.user!!.toUser()))
  }

  private fun getFirebaseAuthCredential(googleAccount: GoogleSignInAccount): AuthCredential =
    GoogleAuthProvider.getCredential(googleAccount.idToken, null)

  private fun FirebaseUser.toUser(): User =
    User(uid, email.orEmpty(), displayName.orEmpty(), photoUrl.toString())
}
