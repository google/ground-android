package com.google.android.gnd.ui.basemapselector;

import androidx.fragment.app.Fragment;
import com.google.android.gnd.inject.FragmentScoped;
import dagger.Binds;
import dagger.Module;

@Module
public abstract class BasemapSelectorModule {

  @Binds
  @FragmentScoped
  abstract Fragment fragment(BasemapSelectorFragment fragment);
}
