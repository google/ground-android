package com.google.android.gnd.system.auth;

import android.util.Log;
import com.google.android.gnd.model.User;
import io.reactivex.Observable;
import javax.inject.Inject;

public class FakeAuthenticationManager implements AuthenticationManager {

  private static final String TAG = FakeAuthenticationManager.class.toString();

  @Inject
  public FakeAuthenticationManager(){
    Log.d(TAG, "FakeAuthMgr ctor");
  }

  @Override
  public Observable<SignInState> getSignInState() {
    return null;
  }

  @Override
  public void signOut() {

  }

  @Override
  public User getCurrentUser() {
    return null;
  }

  @Override
  public void signIn() {

  }

  @Override
  public void init() {
    Log.d(TAG, "FakeAuthMgr init");
  }
}
