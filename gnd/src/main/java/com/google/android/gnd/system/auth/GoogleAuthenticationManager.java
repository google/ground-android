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

package com.google.android.gnd.system.auth;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import androidx.annotation.NonNull;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.gnd.R;
import com.google.android.gnd.model.User;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.system.ActivityStreams;
import com.google.android.gnd.system.ActivityStreams.ActivityResult;
import com.google.android.gnd.system.auth.SignInState.State;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import java8.util.Optional;
import javax.inject.Inject;
import timber.log.Timber;

public class GoogleAuthenticationManager implements AuthenticationManager {

  private static final int SIGN_IN_REQUEST_CODE = AuthenticationManager.class.hashCode() & 0xffff;
  private final GoogleSignInOptions googleSignInOptions;

  @Hot(replays = true)
  private final Subject<SignInState> signInState = BehaviorSubject.create();

  private final FirebaseAuth firebaseAuth;
  private final ActivityStreams activityStreams;
  private final Disposable activityResultsSubscription;

  // TODO: Update Fragments to access via ProjectRepository rather than directly.
  @Inject
  public GoogleAuthenticationManager(Application application, ActivityStreams activityStreams) {
    this.firebaseAuth = FirebaseAuth.getInstance();
    this.googleSignInOptions =
        new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(application.getResources().getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build();
    this.activityStreams = activityStreams;
    this.activityResultsSubscription =
        activityStreams.getActivityResults(SIGN_IN_REQUEST_CODE).subscribe(this::onActivityResult);
  }

  public Observable<SignInState> getSignInState() {
    return signInState;
  }

  public Observable<Optional<User>> getUser() {
    return getSignInState().map(SignInState::getUser);
  }

  public void init() {
    signInState.onNext(getStatus());
  }

  private SignInState getStatus() {
    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
    if (firebaseUser == null) {
      return new SignInState(State.SIGNED_OUT);
    } else {
      return new SignInState(toUser(firebaseUser));
    }
  }

  public void signIn() {
    signInState.onNext(new SignInState(State.SIGNING_IN));
    activityStreams.withActivity(
        activity -> {
          Intent signInIntent = getGoogleSignInClient(activity).getSignInIntent();
          activity.startActivityForResult(signInIntent, SIGN_IN_REQUEST_CODE);
        });
  }

  public void signOut() {
    firebaseAuth.signOut();
    signInState.onNext(new SignInState(State.SIGNED_OUT));
    activityStreams.withActivity(activity -> getGoogleSignInClient(activity).signOut());
  }

  @NonNull
  private GoogleSignInClient getGoogleSignInClient(Activity activity) {
    // TODO: Use app context instead of activity?
    return GoogleSignIn.getClient(activity, googleSignInOptions);
  }

  private void onActivityResult(ActivityResult activityResult) {
    // The Task returned from getSignedInAccountFromIntent is always completed, so no need to
    // attach a listener.
    try {
      Task<GoogleSignInAccount> googleSignInTask =
          GoogleSignIn.getSignedInAccountFromIntent(activityResult.getData());
      onGoogleSignIn(googleSignInTask.getResult(ApiException.class));
    } catch (ApiException e) {
      Timber.e(e, "Sign in failed");
      signInState.onNext(new SignInState(e));
    }
  }

  private void onGoogleSignIn(GoogleSignInAccount googleAccount) {
    firebaseAuth
        .signInWithCredential(getFirebaseAuthCredential(googleAccount))
        .addOnSuccessListener(this::onFirebaseAuthSuccess)
        .addOnFailureListener(t -> signInState.onNext(new SignInState(t)));
  }

  private void onFirebaseAuthSuccess(AuthResult authResult) {
    // TODO: Store/update user profile in Firestore.
    // TODO: Store/update user profile and image locally.
    signInState.onNext(new SignInState(toUser(authResult.getUser())));
  }

  @NonNull
  private static AuthCredential getFirebaseAuthCredential(GoogleSignInAccount googleAccount) {
    return GoogleAuthProvider.getCredential(googleAccount.getIdToken(), null);
  }

  @Override
  protected void finalize() throws Throwable {
    activityResultsSubscription.dispose();
    super.finalize();
  }

  private static User toUser(FirebaseUser firebaseUser) {
    return User.builder()
        .setId(firebaseUser.getUid())
        .setEmail(firebaseUser.getEmail())
        .setDisplayName(firebaseUser.getDisplayName())
        .build();
  }

  /**
   * Returns the current user, blocking until a user logs in. Only call from code where user is
   * guaranteed to be authenticated.
   */
  public User getCurrentUser() {
    return getUser().filter(Optional::isPresent).map(Optional::get).blockingFirst();
  }
}
