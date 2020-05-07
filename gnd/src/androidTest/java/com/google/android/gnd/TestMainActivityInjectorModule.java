package com.google.android.gnd;

import com.google.android.gnd.inject.ActivityScoped;
import com.google.android.gnd.system.TestAuthenticationModule;
import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
abstract public class TestMainActivityInjectorModule {

  /** Causes Dagger Android to generate a sub-component for the MainActivity. */
  @ActivityScoped
  @ContributesAndroidInjector(modules = {MainActivityModule.class, TestAuthenticationModule.class})
  abstract MainActivity mainActivityInjector();


}
