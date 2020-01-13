package com.google.android.gnd.ui.offlinearea.selector;

import androidx.fragment.app.Fragment;
import com.google.android.gnd.inject.FragmentScoped;
import com.google.android.gnd.ui.offlinearea.OfflineAreasFragment;
import dagger.Binds;
import dagger.Module;

@Module
public abstract class OfflineAreaSelectorModule {

  @Binds
  @FragmentScoped
  abstract Fragment fragment(OfflineAreasFragment fragment);
}
