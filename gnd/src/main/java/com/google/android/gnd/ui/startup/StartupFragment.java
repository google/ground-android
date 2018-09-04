package com.google.android.gnd.ui.startup;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gnd.R;
import com.google.android.gnd.system.AuthenticationManager;
import com.google.android.gnd.system.GoogleApiManager;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.EphemeralPopups;

import javax.inject.Inject;

import io.reactivex.Completable;

import static com.google.android.gnd.rx.RxAutoDispose.autoDisposable;

public class StartupFragment extends AbstractFragment {
  private static final String TAG = StartupFragment.class.getSimpleName();
  @Inject
  GoogleApiManager googleApiManager;
  @Inject
  AuthenticationManager authenticationManager;

  @Override
  public void onStart() {
    super.onStart();

    googleApiManager
        .installGooglePlayServices(getActivity())
        .doOnError(this::onGooglePlayServicesInstallError)
        .andThen(signIn())
        .as(autoDisposable(this))
        .subscribe(this::openHomeScreen, __ -> quit());

    //    if (account == null) {
    //      account = GoogleSignIn.getLastSignedInAccount(this);
    //    }
    //    if (account == null) {
    //      signIn();
    //    }
    // TODO: Implement sign out with:
    //    FirebaseAuth.getInstance().signOut();
  }

  private void openHomeScreen() {
  }

  private void onGooglePlayServicesInstallError(Throwable throwable) {
    Log.e(TAG, "Google Play Services install failed", throwable);
    EphemeralPopups.showError(getContext(), R.string.google_api_install_failed);
  }

  private void quit() {
    getActivity().finish();
  }

  private Completable signIn() {
    authenticationManager.signIn(getActivity());
    return Completable.complete();
  }

  @Override
  public View onCreateView(LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.startup_frag, container, false);
  }
}
