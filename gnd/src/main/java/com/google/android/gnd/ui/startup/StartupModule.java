package com.google.android.gnd.ui.startup;

import android.support.v4.app.Fragment;

import com.google.android.gnd.inject.PerFragment;
import com.google.android.gnd.ui.common.AbstractFragmentModule;

import dagger.Binds;
import dagger.Module;

// TODO: Merge app-scoped Dagger modules.
@Module(includes = AbstractFragmentModule.class)
public abstract class StartupModule {
  @Binds
  @PerFragment
  abstract Fragment fragment(StartupFragment fragment);
}
