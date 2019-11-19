package com.google.android.gnd.ui.offlinearea;

import androidx.fragment.app.Fragment;

import com.google.android.gnd.inject.FragmentScoped;
import com.google.android.gnd.ui.offlinearea.OfflineAreaManagerFragment;

import dagger.Binds;
import dagger.Module;

@Module
public abstract class OfflineAreaManagerModule {

  @Binds
  @FragmentScoped
  abstract Fragment fragment(OfflineAreaManagerFragment fragment);
}
