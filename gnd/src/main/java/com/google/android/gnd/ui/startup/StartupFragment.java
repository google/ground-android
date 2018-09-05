package com.google.android.gnd.ui.startup;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

import androidx.navigation.fragment.NavHostFragment;
import butterknife.BindView;
import butterknife.OnClick;

import static com.google.android.gnd.rx.RxAutoDispose.autoDisposable;

public class StartupFragment extends AbstractFragment {
  private static final String TAG = StartupFragment.class.getSimpleName();
  @Inject GoogleApiManager googleApiManager;
  @Inject AuthenticationManager authenticationManager;

  @BindView(R.id.sign_in_button)
  View signInButton;

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    signInButton.setEnabled(false);
  }

  @Override
  public void onResume() {
    super.onResume();

    // Run through required first-time steps. We do this in onResume() so that they'll be rerun if
    // the user clicks "back" from sign in flow as well.
    googleApiManager
        .installGooglePlayServices(getActivity())
        .doOnError(this::onGooglePlayServicesInstallError)
        .as(autoDisposable(getActivity()))
        .subscribe(this::onGooglePlaceServiceReady, __ -> quit());

    // TODO: Implement sign out with:
    //    FirebaseAuth.getInstance().signOut();
  }

  private void onGooglePlaceServiceReady() {
    authenticationManager
        .refresh(getActivity())
        .as(autoDisposable(getActivity()))
        .subscribe(
            success -> {
              if (success) {
                navigateToHomeScreen();
              } else {
                signInButton.setEnabled(true);
              }
            },
            __ -> EphemeralPopups.showError(getContext()));
  }

  @OnClick(R.id.sign_in_button)
  public void onSignInButtonClick() {
    authenticationManager
        .signIn(getActivity())
        .as(autoDisposable(getActivity()))
        .subscribe(
            this::navigateToHomeScreen,
            e -> EphemeralPopups.showError(getContext(), R.string.sign_in_unsuccessful));
  }

  private void navigateToHomeScreen() {
    NavHostFragment.findNavController(this)
        .navigate(StartupFragmentDirections.proceedToHomeScreen());
  }

  private void onGooglePlayServicesInstallError(Throwable throwable) {
    Log.e(TAG, "Google Play Services install failed", throwable);

    EphemeralPopups.showError(getContext(), R.string.google_api_install_failed);
  }

  private void quit() {
    getActivity().finish();
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.startup_frag, container, false);
  }
}
