package com.google.android.gnd.system.auth;

import com.google.android.gnd.inject.ActivityScoped;
import com.google.android.gnd.system.auth.AuthenticationManager;
import com.google.android.gnd.system.auth.AuthenticationManagerImpl;
import dagger.Binds;
import dagger.Module;

@Module
public abstract class AuthenticationModule {

  @ActivityScoped
  @Binds
  abstract AuthenticationManager bind(AuthenticationManagerImpl implementation);

}
