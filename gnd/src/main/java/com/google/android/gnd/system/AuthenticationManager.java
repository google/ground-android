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
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.gnd.GndApplication;
import com.google.android.gnd.R;
import com.google.android.gnd.inject.PerActivity;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import java8.util.Optional;
import java8.util.function.Consumer;
import javax.inject.Inject;

@PerActivity
public class AuthenticationManager {
  private static final String TAG = AuthenticationManager.class.getSimpleName();
  private static final int SIGN_IN_REQUEST_CODE = AuthenticationManager.class.hashCode() & 0xffff;
  private final GoogleSignInOptions googleSignInOptions;
  private final Subject<AuthStatus> authStatusSubject;
  private final Subject<Throwable> authErrorSubject;

  @Inject
  public AuthenticationManager(GndApplication application) {
    this.authStatusSubject = BehaviorSubject.createDefault(AuthStatus.unknown());
    this.authErrorSubject = PublishSubject.create();
    this.googleSignInOptions =
        new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(application.getResources().getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build();
  }

  public Observable<AuthStatus> getStatus() {
    return authStatusSubject;
  }

  public Observable<Throwable> getErrors() {
    return authErrorSubject;
  }

  public boolean refresh(Activity activity) {
    GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(activity);
    if (account == null) {
      return false;
    }
    FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
    if (firebaseUser != null) {
      onAuthSuccess(new User());
      return true;
    }
    signInToFirebase(account);
    return false;
  }

  private void onAuthSuccess(User user) {
    // TODO: Store/update user profile and image locally.
    authStatusSubject.onNext(AuthStatus.authenticated(user));
  }

  public void signIn(Activity activity) {
    Intent signInIntent = GoogleSignIn.getClient(activity, googleSignInOptions).getSignInIntent();
    activity.startActivityForResult(signInIntent, SIGN_IN_REQUEST_CODE);
  }

  public void signOut(Activity activity) {
    GoogleSignIn.getClient(activity, googleSignInOptions).signOut();
    FirebaseAuth.getInstance().signOut();
    authStatusSubject.onNext(AuthStatus.unknown());
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
      if (authStatusSubject == null) {
        Log.e(TAG, "Sign in subject not initialized");
        return;
      }
      signInToFirebase(completedTask.getResult(ApiException.class));
    } catch (ApiException e) {
      Log.w(TAG, "Sign in failed, GoogleSignInStatusCodes:  " + e.getStatusCode());
      onAuthError(e);
    }
  }

  private void signInToFirebase(GoogleSignInAccount account) {
    FirebaseAuth.getInstance().signInWithCredential(getAuthCredential(account))
        .addOnSuccessListener(this::onFirebaseAuthSuccess)
        .addOnFailureListener(this::onAuthError);

  }

  private void onFirebaseAuthSuccess(AuthResult authResult) {
    // TODO: Store/update user profile in Firestore.
    onAuthSuccess(new User());
  }

  private void onAuthError(Throwable t) {
    authErrorSubject.onNext(t);
  }

  @NonNull
  private static AuthCredential getAuthCredential(GoogleSignInAccount account) {
    return GoogleAuthProvider.getCredential(account.getIdToken(), null);
  }

  public static class AuthStatus {

    private Optional<User> user;

    private AuthStatus(Optional<User> user) {
      this.user = user;
    }

    public static AuthStatus unknown() {
      return new AuthStatus(Optional.empty());
    }

    public static AuthStatus authenticated(User user) {
      return new AuthStatus(Optional.of(user));
    }

    public boolean isAuthenticated() {
      return !user.isEmpty();
    }

    public void ifAuthenticated(Consumer<User> action) {
      user.ifPresent(action);
    }
  }

  public static class User {

  }
}
