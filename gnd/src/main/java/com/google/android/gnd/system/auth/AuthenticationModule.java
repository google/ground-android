package com.google.android.gnd.system.auth;

import com.google.android.gnd.inject.ActivityScoped;
import dagger.Binds;
import dagger.Module;

@Module
public abstract class AuthenticationModule {

  @ActivityScoped
  @Binds
  abstract AuthenticationManager bind(GoogleAuthenticationManager implementation);

}
