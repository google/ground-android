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

package com.google.android.gnd;

import static com.google.android.gnd.rx.RxAutoDispose.autoDisposable;
import static com.google.android.gnd.util.Debug.logLifecycleEvent;

import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import androidx.navigation.fragment.NavHostFragment;
import butterknife.ButterKnife;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gnd.repository.DataRepository;
import com.google.android.gnd.rx.RxDebug;
import com.google.android.gnd.service.RemoteDataService;
import com.google.android.gnd.system.PermissionsManager;
import com.google.android.gnd.system.PermissionsManager.PermissionsRequest;
import com.google.android.gnd.system.SettingsManager;
import com.google.android.gnd.system.SettingsManager.SettingsChangeRequest;
import com.google.android.gnd.ui.common.OnBackListener;
import com.google.android.gnd.ui.common.TwoLineToolbar;
import com.google.android.gnd.ui.common.ViewModelFactory;
import com.google.android.gnd.ui.util.DrawableUtil;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import dagger.android.AndroidInjection;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import io.reactivex.plugins.RxJavaPlugins;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MainActivity extends AppCompatActivity implements HasSupportFragmentInjector {

  private static final String TAG = MainActivity.class.getSimpleName();
  private static final int RC_SIGN_IN = 1234;

  @Inject ViewModelFactory viewModelFactory;
  @Inject PermissionsManager permissionsManager;
  @Inject SettingsManager settingsManager;
  @Inject RemoteDataService remoteDataService;
  @Inject DataRepository model;
  @Inject DispatchingAndroidInjector<Fragment> fragmentInjector;

  private NavHostFragment navHostFragment;
  private MainViewModel viewModel;
  private GoogleSignInClient googleSignInClient;
  private GoogleSignInAccount account;
  private FirebaseAuth firebaseAuth;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    logLifecycleEvent(this);

    // Prevent RxJava from force-quitting on unhandled errors.
    RxJavaPlugins.setErrorHandler(t -> RxDebug.logEnhancedStackTrace(t));

    super.onCreate(savedInstanceState);

    AndroidInjection.inject(this);

    setContentView(R.layout.main_act);

    ButterKnife.bind(this);

    navHostFragment =
      (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

    viewModel = viewModelFactory.get(this, MainViewModel.class);

    ViewCompat.setOnApplyWindowInsetsListener(
      getWindow().getDecorView().getRootView(), viewModel::onApplyWindowInsets);

    permissionsManager
      .getPermissionsRequests()
      .as(autoDisposable(this))
      .subscribe(this::onPermissionsRequest);

    settingsManager
      .getSettingsChangeRequests()
      .as(autoDisposable(this))
      .subscribe(this::onSettingsChangeRequest);

    GoogleSignInOptions gso =
      new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(getString(R.string.default_web_client_id))
        .requestEmail()
        .requestProfile()
        .build();
    this.googleSignInClient = GoogleSignIn.getClient(this, gso);
    this.firebaseAuth = FirebaseAuth.getInstance();
  }

  private void signIn() {
    Intent signInIntent = googleSignInClient.getSignInIntent();
    startActivityForResult(signInIntent, RC_SIGN_IN);
  }

  private void onPermissionsRequest(PermissionsRequest permissionsRequest) {
    Log.d(TAG, "Sending permissions request to system");
    ActivityCompat.requestPermissions(
        this, permissionsRequest.getPermissions(), permissionsRequest.getRequestCode());
  }

  private void onSettingsChangeRequest(SettingsChangeRequest settingsChangeRequest) {
    try {
      // The result of this call is received by {@link #onActivityResult}.
      Log.d(TAG, "Sending settings resolution request");
      settingsChangeRequest
          .getException()
          .startResolutionForResult(this, settingsChangeRequest.getRequestCode());
    } catch (SendIntentException e) {
      // TODO: Report error.
      Log.e(TAG, e.toString());
    }
  }

  @Override
  protected void onStart() {
    logLifecycleEvent(this);
    super.onStart();
    if (account == null) {
      account = GoogleSignIn.getLastSignedInAccount(this);
    }
    if (account == null) {
      signIn();
    }
    // TODO: Implement sign out with:
    //    FirebaseAuth.getInstance().signOut();
  }

  @Override
  protected void onResume() {
    logLifecycleEvent(this);
    super.onResume();
  }

  @Override
  protected void onPause() {
    logLifecycleEvent(this);
    super.onPause();
  }

  @Override
  protected void onStop() {
    logLifecycleEvent(this);
    super.onStop();
  }

  @Override
  protected void onDestroy() {
    logLifecycleEvent(this);
    super.onDestroy();
  }

  /**
   * The Android permissions API requires this callback to live in an Activity; here we dispatch the
   * result back to the PermissionManager for handling.
   */
  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    Log.d(TAG, "Permission result received");
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  /**
   * The Android settings API requires this callback to live in an Activity; here we dispatch the
   * result back to the SettingsManager for handling.
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    settingsManager.onActivityResult(requestCode, resultCode);
    if (requestCode == RC_SIGN_IN) {
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
    firebaseAuth
      .signInWithCredential(credential)
      .addOnCompleteListener(
        this,
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

  @Override
  public final AndroidInjector<Fragment> supportFragmentInjector() {
    return fragmentInjector;
  }

  public void setActionBar(TwoLineToolbar toolbar, int upIconId) {
    setActionBar(toolbar);
    // We override the color here programmatically since calling setHomeAsUpIndicator uses the color
    // of the set icon, not the applied theme. This allows us to change the primary color
    // programmatically without needing to remember to update the icon.
    Drawable icon = new DrawableUtil(getResources()).getDrawable(upIconId, R.color.colorPrimary);
    getSupportActionBar().setHomeAsUpIndicator(icon);
  }

  public void setActionBar(TwoLineToolbar toolbar) {
    setSupportActionBar(toolbar);

    // Workaround to get rid of application title from toolbar. Simply setting "" here or in layout
    // XML doesn't work.
    getSupportActionBar().setDisplayShowTitleEnabled(false);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setDisplayShowHomeEnabled(true);

    // TODO: Remove this workaround once setupActionBarWithNavController() works with custom
    // Toolbars (https://issuetracker.google.com/issues/109868820).
    toolbar.setNavigationOnClickListener(__ -> navigateUp());
  }

  private void navigateUp() {
    if (!callFragmentBackHandler()) {
      navHostFragment.getNavController().navigateUp();
    }
  }

  @Override
  public void onBackPressed() {
    if (!callFragmentBackHandler()) {
      super.onBackPressed();
    }
  }

  private boolean callFragmentBackHandler() {
    Fragment currentFragment = getCurrentFragment();
    return currentFragment instanceof OnBackListener && ((OnBackListener) currentFragment).onBack();
  }

  private Fragment getCurrentFragment() {
    return navHostFragment.getChildFragmentManager().findFragmentById(R.id.nav_host_fragment);
  }
}
