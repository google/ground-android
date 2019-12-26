/*
 * Copyright 2018 Google LLC
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

package com.google.android.gnd.system;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.gnd.R;
import com.google.android.gnd.inject.ActivityScoped;
import com.google.android.gnd.rx.ValueOrError;
import com.google.android.gnd.system.ActivityStreams.ActivityResult;
import com.google.android.gnd.system.AuthenticationManager.SignInState.State;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import javax.inject.Inject;

@ActivityScoped
public class AuthenticationManager {
  private static final String TAG = AuthenticationManager.class.getSimpleName();
  private static final int SIGN_IN_REQUEST_CODE = AuthenticationManager.class.hashCode() & 0xffff;
  private final GoogleSignInOptions googleSignInOptions;
  private final Subject<SignInState> signInState;
  private final FirebaseAuth firebaseAuth;
  private final ActivityStreams activityStreams;
  private final Disposable activityResultsSubscription;

  // TODO: Update Fragments to access via DataRepository rather than directly.
  @Inject
  public AuthenticationManager(Application application, ActivityStreams activityStreams) {
    this.signInState = BehaviorSubject.create();
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

  public Observable<User> getUser() {
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
      return new SignInState(new User(firebaseUser));
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
      Log.w(TAG, "Sign in failed, GoogleSignInStatusCodes:  " + e.getStatusCode());
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
    signInState.onNext(new SignInState(new User(authResult.getUser())));
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

  public static class SignInState extends ValueOrError<User> {

    private final State state;

    public enum State {
      SIGNED_OUT,
      SIGNING_IN,
      SIGNED_IN,
      ERROR
    }

    private SignInState(State state) {
      super(null, null);
      this.state = state;
    }

    private SignInState(User user) {
      super(user, null);
      this.state = State.SIGNED_IN;
    }

    private SignInState(Throwable error) {
      super(null, error);
      this.state = State.ERROR;
    }

    public State state() {
      return state;
    }

    public User getUser() {
      return value().orElse(User.ANONYMOUS);
    }
  }

  public static class User {

    public static final User ANONYMOUS = new User("", "", "");

    private final String uid;
    private final String email;
    private final String displayName;

    public User(String uid, String email, String displayName) {
      this.uid = uid;
      this.email = email;
      this.displayName = displayName;
    }

    private User(FirebaseUser firebaseUser) {
      this(firebaseUser.getUid(), firebaseUser.getEmail(), firebaseUser.getDisplayName());
    }

    public String getId() {
      return uid;
    }

    public String getEmail() {
      return email;
    }

    public String getDisplayName() {
      return displayName;
    }
  }
}
