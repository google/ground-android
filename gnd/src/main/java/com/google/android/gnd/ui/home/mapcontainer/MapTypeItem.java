package com.google.android.gnd.ui.home.mapcontainer;

import com.google.android.gnd.ui.map.MapFragment;

/**
 * A map type item in the list view inside map container.
 */
public class MapTypeItem {
  private final int type;
  private final String label;
  private final MapFragment mapFragment;

  MapTypeItem(int type, String label, MapFragment mapFragment) {
    this.type = type;
    this.label = label;
    this.mapFragment = mapFragment;
  }

  public int getType() {
    return type;
  }

  public String getLabel() {
    return label;
  }

  public boolean isSelected() {
    return mapFragment.getMapType() == type;
  }
}
