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
import android.support.annotation.NonNull;
import android.util.Log;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.gnd.R;
import com.google.android.gnd.inject.ActivityScoped;
import com.google.android.gnd.system.ActivityStreams.ActivityResult;
import com.google.android.gnd.system.AuthenticationManager.AuthStatus.State;
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
  private final Subject<AuthStatus> authStatus;
  private final FirebaseAuth firebaseAuth;
  private final ActivityStreams activityStreams;
  private final Disposable activityResultsSubscription;

  // TODO: Update Fragments to access via DataRepository rather than directly.
  @Inject
  public AuthenticationManager(Application application, ActivityStreams activityStreams) {
    this.authStatus = BehaviorSubject.create();
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

  public Observable<AuthStatus> getAuthStatus() {
    return authStatus;
  }

  public Observable<User> getUser() {
    return getAuthStatus().map(AuthStatus::getUser);
  }

  public void init() {
    authStatus.onNext(getStatus());
  }

  public boolean isSignedIn() {
    return firebaseAuth.getCurrentUser() != null;
  }

  private AuthStatus getStatus() {
    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
    if (firebaseUser == null) {
      return new AuthStatus(State.SIGNED_OUT);
    } else {
      return new AuthStatus(new User(firebaseUser));
    }
  }

  public void signIn() {
    authStatus.onNext(new AuthStatus(State.SIGNING_IN));
    activityStreams.withActivity(
        activity -> {
          Intent signInIntent = getGoogleSignInClient(activity).getSignInIntent();
          activity.startActivityForResult(signInIntent, SIGN_IN_REQUEST_CODE);
        });
  }

  public void signOut() {
    firebaseAuth.signOut();
    authStatus.onNext(new AuthStatus(State.SIGNED_OUT));
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
      authStatus.onNext(new AuthStatus(e));
    }
  }

  private void onGoogleSignIn(GoogleSignInAccount googleAccount) {
    firebaseAuth
        .signInWithCredential(getFirebaseAuthCredential(googleAccount))
        .addOnSuccessListener(this::onFirebaseAuthSuccess)
        .addOnFailureListener(t -> authStatus.onNext(new AuthStatus(t)));
  }

  private void onFirebaseAuthSuccess(AuthResult authResult) {
    // TODO: Store/update user profile in Firestore.
    // TODO: Store/update user profile and image locally.
    authStatus.onNext(new AuthStatus(new User(authResult.getUser())));
  }

  @NonNull
  private static AuthCredential getFirebaseAuthCredential(GoogleSignInAccount googleAccount) {
    return GoogleAuthProvider.getCredential(googleAccount.getIdToken(), null);
  }

  @Override
  protected void finalize() throws Throwable {
    activityResultsSubscription.dispose();
  }

  public static class AuthStatus extends AbstractResource<AuthStatus.State, User> {
    public enum State {
      SIGNED_OUT,
      SIGNING_IN,
      SIGNED_IN,
      ERROR
    }

    private AuthStatus(State state) {
      super(state, null, null);
    }

    private AuthStatus(User user) {
      super(State.SIGNED_IN, user, null);
    }

    private AuthStatus(Throwable error) {
      super(State.ERROR, null, error);
    }

    public boolean isSignedIn() {
      return getState().equals(State.SIGNED_IN);
    }

    public User getUser() {
      return getData().orElse(User.ANONYMOUS);
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
