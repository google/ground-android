package com.google.android.gnd.system;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gnd.GndApplication;
import com.google.android.gnd.R;
import com.google.android.gnd.inject.PerActivity;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import javax.inject.Inject;

@PerActivity
public class AuthenticationManager {
  private static final String TAG = AuthenticationManager.class.getSimpleName();
  private static final int SIGN_IN_REQUEST_CODE = AuthenticationManager.class.hashCode() & 0xffff;
  private final GoogleSignInOptions googleSignInOptions;
  // TODO: Remove field if not needed.
  private GoogleSignInAccount account;

  @Inject
  public AuthenticationManager(GndApplication application) {
    this.googleSignInOptions =
        new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(application.getResources().getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build();
  }

  public void signIn(Activity activity) {
    Intent signInIntent = GoogleSignIn.getClient(activity, googleSignInOptions).getSignInIntent();
    activity.startActivityForResult(signInIntent, SIGN_IN_REQUEST_CODE);
  }

  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    if (requestCode == SIGN_IN_REQUEST_CODE) {
      // The Task returned from this call is always completed, no need to attach
      // a listener.
      Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(intent);
      handleSignInResult(task);
    }
  }

  private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
    try {
      account = completedTask.getResult(ApiException.class);
      firebaseAuthWithGoogle(account);

      // Signed in successfully, show authenticated UI.
      //      updateUI(account);
    } catch (ApiException e) {
      // The ApiException status code indicates the detailed failure reason.
      // Please refer to the GoogleSignInStatusCodes class reference for more information.
      Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
      //      updateUI(null);
    }
  }

  private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
    Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

    AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
    FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    firebaseAuth
        .signInWithCredential(credential)
        .addOnCompleteListener(
            new OnCompleteListener<AuthResult>() {
              @Override
              public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                  // Sign in success, update UI with the signed-in user's information
                  Log.d(TAG, "signInWithCredential:success");
                  // TODO: Update UI
                  //                  FirebaseUser user = firebaseAuth.getCurrentUser();

                  Log.i(TAG, "User logged in");
                  // TODO: Move into its own fragment.
                  // TODO: Move into AuthenticationService?
                  // TODO: Store/update user profile in Firestore.
                  // TODO: Store/update user profile and image locally.
                  //               updateUI(user);
                } else {
                  // If sign in fails, display a message to the user.
                  Log.w(TAG, "signInWithCredential:failure", task.getException());
                  // TODO: Log error.
                  //               Snackbar
                  //                 .make(findViewById(R.id.main_layout), "Authentication Failed.",
                  // Snackbar.LENGTH_SHORT).show();
                  //               updateUI(null);
                }

                // ...
              }
            });
  }
}
