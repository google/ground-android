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
package com.google.android.gnd.system.auth

import android.app.Activity
import android.content.res.Resources
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gnd.R
import com.google.android.gnd.model.User
import com.google.android.gnd.rx.annotations.Hot
import com.google.android.gnd.system.ActivityResult
import com.google.android.gnd.system.ActivityStreams
import com.google.android.gnd.system.auth.SignInState
import com.google.firebase.auth.*
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import timber.log.Timber
import javax.inject.Inject

private val SIGN_IN_REQUEST_CODE = AuthenticationManager::class.java.hashCode() and 0xffff

class GoogleAuthenticationManager @Inject constructor(
    resources: Resources,
    private val activityStreams: ActivityStreams
) : AuthenticationManager {

    private val activityResultsSubscription: Disposable
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val googleSignInOptions: GoogleSignInOptions

    // TODO: Update Fragments to access via ProjectRepository rather than directly.
    init {
        googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(resources.getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()

        // TODO: Dispose the subscription when object is destroyed
        activityResultsSubscription = activityStreams.getActivityResults(SIGN_IN_REQUEST_CODE)
            .subscribe { onActivityResult(it) }
    }

    override val signInState: @Hot(replays = true) Subject<SignInState> = BehaviorSubject.create()

    /**
     * Returns the current user, blocking until a user logs in. Only call from code where user is
     * guaranteed to be authenticated.
     */
    override val currentUser: User
        get() = signInState.map(SignInState::user)
            .filter { it.isPresent }
            .map { it.get() }
            .blockingFirst() // TODO: Should this be blocking?

    override fun init() = signInState.onNext(status)

    private val status: SignInState
        get() {
            val firebaseUser = firebaseAuth.currentUser
            return if (firebaseUser == null) {
                SignInState(SignInState.State.SIGNED_OUT)
            } else {
                SignInState(firebaseUser.toUser())
            }
        }

    override fun signIn() {
        signInState.onNext(SignInState(SignInState.State.SIGNING_IN))
        activityStreams.withActivity {
            val signInIntent = getGoogleSignInClient(it).signInIntent
            it.startActivityForResult(signInIntent, SIGN_IN_REQUEST_CODE)
        }
    }

    override fun signOut() {
        firebaseAuth.signOut()
        signInState.onNext(SignInState(SignInState.State.SIGNED_OUT))
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
            signInState.onNext(SignInState(e))
        }
    }

    private fun onGoogleSignIn(googleAccount: GoogleSignInAccount) =
        firebaseAuth
            .signInWithCredential(getFirebaseAuthCredential(googleAccount))
            .addOnSuccessListener { authResult: AuthResult -> onFirebaseAuthSuccess(authResult) }
            .addOnFailureListener { signInState.onNext(SignInState(it)) }

    private fun onFirebaseAuthSuccess(authResult: AuthResult) =
    // TODO: Store/update user profile in Firestore.
        // TODO: Store/update user profile and image locally.
        signInState.onNext(SignInState(authResult.user!!.toUser()))

    private fun getFirebaseAuthCredential(googleAccount: GoogleSignInAccount): AuthCredential =
        GoogleAuthProvider.getCredential(googleAccount.idToken, null)

    private fun FirebaseUser.toUser(): User =
        User.builder()
            .setId(uid)
            .setEmail(email)
            .setDisplayName(displayName)
            .setPhotoUrl(photoUrl.toString())
            .build()
}
