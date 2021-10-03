package com.google.android.gnd.ui.map.gms;

import androidx.fragment.app.Fragment;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gnd.R;
import com.google.android.gnd.ui.map.MapFragmentFactory;
import com.google.android.gnd.ui.map.MapType;
import com.google.common.collect.ImmutableList;
import javax.inject.Inject;

public class GoogleMapsMapFragmentFactory implements MapFragmentFactory {

  @Inject
  GoogleMapsMapFragmentFactory() {}

  @Override
  public Fragment createFragment() {
    return new GoogleMapsFragment();
  }

  @Override
  public ImmutableList<MapType> getMapTypes() {
    return ImmutableList.<MapType>builder()
        .add(new MapType(GoogleMap.MAP_TYPE_NORMAL, R.string.normal))
        .add(new MapType(GoogleMap.MAP_TYPE_SATELLITE, R.string.satellite))
        .add(new MapType(GoogleMap.MAP_TYPE_TERRAIN, R.string.terrain))
        .add(new MapType(GoogleMap.MAP_TYPE_HYBRID, R.string.hybrid))
        .build();
  }
}
