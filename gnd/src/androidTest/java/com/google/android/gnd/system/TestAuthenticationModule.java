package com.google.android.gnd.system;

import com.google.android.gnd.inject.ActivityScoped;
import dagger.Binds;
import dagger.Module;

@Module
public abstract class TestAuthenticationModule {

  @ActivityScoped
  @Binds
  abstract AuthenticationManager bind(FakeAuthenticationManager implementation);

}
