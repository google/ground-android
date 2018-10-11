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
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.gnd.GndApplication;
import com.google.android.gnd.R;
import com.google.android.gnd.inject.ActivityScoped;
import com.google.android.gnd.system.AuthenticationManager.AuthStatus.State;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import javax.inject.Inject;

@ActivityScoped
public class AuthenticationManager {
  private static final String TAG = AuthenticationManager.class.getSimpleName();
  private static final int SIGN_IN_REQUEST_CODE = AuthenticationManager.class.hashCode() & 0xffff;
  private final GoogleSignInOptions googleSignInOptions;
  private final Subject<AuthStatus> authStateSubject;
  private final FirebaseAuth firebaseAuth;

  // TODO: Update Fragments to access via DataRepository rather than directly.
  @Inject
  public AuthenticationManager(GndApplication application) {
    this.authStateSubject = BehaviorSubject.create();
    this.firebaseAuth = FirebaseAuth.getInstance();
    this.googleSignInOptions =
      new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(application.getResources().getString(R.string.default_web_client_id))
        .requestEmail()
        .requestProfile()
        .build();
  }

  public Observable<AuthStatus> getAuthStatus() {
    return authStateSubject;
  }

  public Flowable<User> withUser() {
    return getAuthStatus().map(AuthStatus::getUser).toFlowable(BackpressureStrategy.LATEST);
  }

  public void init() {
    authStateSubject.onNext(getStatus());
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

  public void signIn(Activity activity) {
    authStateSubject.onNext(new AuthStatus(State.SIGNING_IN));
    Intent signInIntent = getGoogleSignInClient(activity).getSignInIntent();
    activity.startActivityForResult(signInIntent, SIGN_IN_REQUEST_CODE);
  }

  public void signOut(Activity activity) {
    getGoogleSignInClient(activity).signOut();
    firebaseAuth.signOut();
    authStateSubject.onNext(new AuthStatus(State.SIGNED_OUT));
  }

  @NonNull
  private GoogleSignInClient getGoogleSignInClient(Activity activity) {
    return GoogleSignIn.getClient(activity, googleSignInOptions);
  }

  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    if (requestCode == SIGN_IN_REQUEST_CODE) {
      // The Task returned from getSignedInAccountFromIntent is always completed, so no need to
      // attach a listener.
      onGoogleSignInResult(GoogleSignIn.getSignedInAccountFromIntent(intent));
    }
  }

  private void onGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
    try {
      signInToFirebase(completedTask.getResult(ApiException.class));
    } catch (ApiException e) {
      Log.w(TAG, "Sign in failed, GoogleSignInStatusCodes:  " + e.getStatusCode());
      authStateSubject.onNext(new AuthStatus(e));
    }
  }

  private void signInToFirebase(GoogleSignInAccount account) {
    firebaseAuth
        .signInWithCredential(getAuthCredential(account))
        .addOnSuccessListener(this::onFirebaseAuthSuccess)
        .addOnFailureListener(t -> authStateSubject.onNext(new AuthStatus(t)));
  }

  private void onFirebaseAuthSuccess(AuthResult authResult) {
    // TODO: Store/update user profile in Firestore.
    // TODO: Store/update user profile and image locally.
    authStateSubject.onNext(new AuthStatus(new User(authResult.getUser())));
  }

  @NonNull
  private static AuthCredential getAuthCredential(GoogleSignInAccount account) {
    return GoogleAuthProvider.getCredential(account.getIdToken(), null);
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
