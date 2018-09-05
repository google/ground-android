package com.google.android.gnd.system;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.gnd.GndApplication;
import com.google.android.gnd.R;
import com.google.android.gnd.inject.PerActivity;
import com.google.android.gnd.rx.RxTask;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.Single;
import io.reactivex.subjects.CompletableSubject;

@PerActivity
public class AuthenticationManager {
  private static final String TAG = AuthenticationManager.class.getSimpleName();
  private static final int SIGN_IN_REQUEST_CODE = AuthenticationManager.class.hashCode() & 0xffff;
  private final GoogleSignInOptions googleSignInOptions;
  @Nullable private CompletableEmitter signInResultEmitter;
  private CompletableSubject signInSubject;

  @Inject
  public AuthenticationManager(GndApplication application) {
    this.googleSignInOptions =
        new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(application.getResources().getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build();
  }

  public Single<Boolean> refresh(Activity activity) {
    return Single.create(
        src -> {
          GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(activity);
          if (account == null) {
            src.onSuccess(false);
            return;
          }
          FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
          if (firebaseUser != null) {
            src.onSuccess(true);
            return;
          }
          signInToFirebase(account).subscribe(() -> src.onSuccess(true), t -> src.onError(t));
        });
  }

  public Completable signIn(Activity activity) {
    signInSubject = CompletableSubject.create();
    Intent signInIntent = GoogleSignIn.getClient(activity, googleSignInOptions).getSignInIntent();
    activity.startActivityForResult(signInIntent, SIGN_IN_REQUEST_CODE);
    return signInSubject;
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
      if (signInSubject == null) {
        Log.e(TAG, "Sign in subject not initialized");
        return;
      }
      signInToFirebase(completedTask.getResult(ApiException.class)).subscribe(signInSubject);
    } catch (ApiException e) {
      Log.w(TAG, "Sign in failed, GoogleSignInStatusCodes:  " + e.getStatusCode());
      signInResultEmitter.onError(e);
    }
  }

  private static Completable signInToFirebase(GoogleSignInAccount account) {
    // TODO: Store/update user profile in Firestore.
    // TODO: Store/update user profile and image locally.
    return RxTask.toCompletable(
        () -> FirebaseAuth.getInstance().signInWithCredential(getAuthCredential(account)));
  }

  @NonNull
  private static AuthCredential getAuthCredential(GoogleSignInAccount account) {
    return GoogleAuthProvider.getCredential(account.getIdToken(), null);
  }

  private void onFirebaseSignInComplete(Task<AuthResult> task) {
    if (task.isSuccessful()) {
      signInResultEmitter.onComplete();

      Log.d(TAG, "Firebase sign in succeeded");
    } else {
      Log.d(TAG, "Firebase sign in failed", task.getException());
      signInResultEmitter.onError(task.getException());
    }
  }
}
