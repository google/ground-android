package com.google.android.gnd.system;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gnd.inject.PerActivity;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;

@PerActivity
public class GoogleApiManager {
  private static final int INSTALL_API_REQUEST_CODE =
      GoogleApiAvailability.class.hashCode() & 0xffff;
  private final GoogleApiAvailability googleApiAvailability;
  // TODO: Replace with CompletableSubject?
  @Nullable
  private CompletableEmitter installApiResultEmitter;
  private int googleApiAvailabilityResult;

  @Inject
  public GoogleApiManager() {
    this.googleApiAvailability = GoogleApiAvailability.getInstance();
    this.googleApiAvailabilityResult = ConnectionResult.UNKNOWN;
  }

  public Completable installGooglePlayServices(FragmentActivity activity) {
    return Completable.create(
        src -> {
          installApiResultEmitter = src;
          googleApiAvailabilityResult =
              googleApiAvailability.isGooglePlayServicesAvailable(activity);
          if (googleApiAvailabilityResult == ConnectionResult.SUCCESS) {
            src.onComplete();
          } else {
            googleApiAvailability.showErrorDialogFragment(
                activity,
                googleApiAvailabilityResult,
                INSTALL_API_REQUEST_CODE,
                di -> src.onError(cancelled()));
          }
        });
  }

  @NonNull
  private RuntimeException cancelled() {
    return new RuntimeException("Google Play Services install cancelled");
  }

  public void onActivityResult(int requestCode, int resultCode) {
    if (requestCode == INSTALL_API_REQUEST_CODE && installApiResultEmitter != null) {
      switch (resultCode) {
        case Activity.RESULT_OK:
          installApiResultEmitter.onComplete();
          break;
        case Activity.RESULT_CANCELED:
          installApiResultEmitter.onError(cancelled());
          break;
        default:
          break;
      }
    }
  }
}
