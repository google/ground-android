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
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.system.ActivityResult
import com.google.android.ground.system.ActivityStreams
import com.google.firebase.auth.*
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import timber.log.Timber

private val signInRequestCode = AuthenticationManager::class.java.hashCode() and 0xffff

class GoogleAuthenticationManager
@Inject
constructor(
  resources: Resources,
  private val activityStreams: ActivityStreams,
  private val firebaseAuth: FirebaseAuth,
  @ApplicationScope private val externalScope: CoroutineScope
) : AuthenticationManager {

  private val googleSignInOptions: GoogleSignInOptions

  init {
    googleSignInOptions =
      GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(resources.getString(R.string.default_web_client_id))
        .requestEmail()
        .requestProfile()
        .build()

    externalScope.launch {
      activityStreams.getActivityResults(signInRequestCode).asFlow().collect {
        onActivityResult(it)
      }
    }
  }

  override val signInState: @Hot(replays = true) Subject<SignInState> = BehaviorSubject.create()

  /**
   * Returns the current user, blocking until a user logs in. Only call from code where user is
   * guaranteed to be authenticated.
   */
  override val currentUser: User
    get() =
      signInState
        .filter { it.state == SignInState.State.SIGNED_IN }
        .map { it.result.getOrNull()!! }
        .blockingFirst() // TODO: Should this be blocking?

  override fun init() {
    externalScope.launch {
      signInState.onNext(
        getFirebaseUser()?.let { SignInState.signedIn(it) } ?: SignInState.signedOut()
      )
    }
  }

  override fun signIn() {
    signInState.onNext(SignInState.signingIn())
    showSignInDialog()
  }

  private fun showSignInDialog() =
    activityStreams.withActivity {
      val signInIntent = getGoogleSignInClient(it).signInIntent
      it.startActivityForResult(signInIntent, signInRequestCode)
    }

  override fun signOut() {
    externalScope.launch {
      firebaseAuth.signOut()
      signInState.onNext(SignInState.signedOut())
      activityStreams.withActivity { getGoogleSignInClient(it).signOut() }
    }
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
      signInState.onNext(SignInState.error(e))
    }
  }

  private fun onGoogleSignIn(googleAccount: GoogleSignInAccount) =
    firebaseAuth
      .signInWithCredential(getFirebaseAuthCredential(googleAccount))
      .addOnSuccessListener { authResult: AuthResult -> onFirebaseAuthSuccess(authResult) }
      .addOnFailureListener { signInState.onNext(SignInState.error(it)) }

  private fun onFirebaseAuthSuccess(authResult: AuthResult) =
    signInState.onNext(SignInState.signedIn(authResult.user!!.toUser()))

  private fun getFirebaseAuthCredential(googleAccount: GoogleSignInAccount): AuthCredential =
    GoogleAuthProvider.getCredential(googleAccount.idToken, null)

  private fun getFirebaseUser(): User? = firebaseAuth.currentUser?.toUser()

  private fun FirebaseUser.toUser(): User =
    User(uid, email.orEmpty(), displayName.orEmpty(), photoUrl.toString())
}
