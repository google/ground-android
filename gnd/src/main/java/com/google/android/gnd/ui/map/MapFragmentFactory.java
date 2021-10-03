package com.google.android.gnd.ui.map;

import androidx.fragment.app.Fragment;
import com.google.common.collect.ImmutableList;

public interface MapFragmentFactory {

  Fragment createFragment();

  ImmutableList<MapType> getMapTypes();
}
