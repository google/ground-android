package com.google.android.gnd.ui.map;

import com.google.android.gnd.model.feature.Point;

public class GroundCameraPosition {

  private final Point target;
  private final Float zoomLevel;

  public GroundCameraPosition(Point target, Float zoomLevel) {
    this.target = target;
    this.zoomLevel = zoomLevel;
  }
  ;

  public Point getTarget() {
    return target;
  }

  public Float getZoomLevel() {
    return zoomLevel;
  }

  public String toString() {
    return "Position: " + target + " Zoom level: " + zoomLevel;
  }
}
