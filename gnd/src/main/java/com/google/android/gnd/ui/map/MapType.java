package com.google.android.gnd.ui.map;

import androidx.annotation.StringRes;

/**
 * MapType refers to the basemap shown below map features and offline satellite imagery. It's called
 * "map styles" in Mapbox and "basemaps" in Leaflet.
 */
public class MapType {

  public final int type;
  public final @StringRes int labelId;

  public MapType(int type, @StringRes int labelId) {
    this.type = type;
    this.labelId = labelId;
  }
}
