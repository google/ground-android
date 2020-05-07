package com.google.android.gnd;

import com.google.android.gnd.inject.ActivityScoped;
import com.google.android.gnd.system.AuthenticationModule;
import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
abstract public class MainActivityInjectorModule {

  /** Causes Dagger Android to generate a sub-component for the MainActivity. */
  @ActivityScoped
  @ContributesAndroidInjector(modules = {MainActivityModule.class, AuthenticationModule.class})
  abstract MainActivity mainActivityInjector();


}
