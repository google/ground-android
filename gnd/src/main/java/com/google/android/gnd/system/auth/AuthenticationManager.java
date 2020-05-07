package com.google.android.gnd.system.auth;

import com.google.android.gnd.model.User;
import com.google.android.gnd.system.auth.SignInState;
import io.reactivex.Observable;

public interface AuthenticationManager {

  Observable<SignInState> getSignInState();

  void signOut();

  User getCurrentUser();

  void signIn();

  void init();
}

