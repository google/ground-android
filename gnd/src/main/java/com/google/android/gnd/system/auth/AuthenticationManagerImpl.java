package com.google.android.gnd.system;

import com.google.android.gnd.model.User;
import io.reactivex.Observable;

public interface AuthenticationManager {

  Observable<SignInState> getSignInState();

  void signOut();

  User getCurrentUser();

  void signIn();

  void init();
}

