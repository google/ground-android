package com.google.android.gnd.system.service;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class ServiceModule {

  @ContributesAndroidInjector
  abstract ForegroundService contributeMyService();
}
