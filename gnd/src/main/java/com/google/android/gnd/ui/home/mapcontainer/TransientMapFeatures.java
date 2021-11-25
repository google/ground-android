package com.google.android.gnd.ui.home.mapcontainer;

import static java8.util.stream.StreamSupport.stream;

import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.feature.PolygonFeature;
import com.google.android.gnd.model.layer.Style;
import com.google.android.gnd.ui.map.MapFeature;
import com.google.android.gnd.ui.map.MapPin;
import com.google.android.gnd.ui.map.MapPolygon;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public final class TransientMapFeatures {

  private TransientMapFeatures() {}

  private static MapFeature toMapPolygon(String id, ImmutableList<Point> vertices, Style style) {
    return MapPolygon.newBuilder().setId(id).setVertices(vertices).setStyle(style).build();
  }

  private static MapFeature toMapPin(String id, Point point, Style style) {
    return MapPin.newBuilder().setId(id).setPosition(point).setStyle(style).build();
  }

  public static ImmutableSet<MapFeature> fromPolygonFeature(PolygonFeature polygonFeature) {
    String id = polygonFeature.getId();
    ImmutableList<Point> vertices = polygonFeature.getVertices();
    Style style = polygonFeature.getLayer().getDefaultStyle();
    ImmutableSet.Builder<MapFeature> builder = ImmutableSet.builder();
    // Add MapPolygon
    builder.add(toMapPolygon(id, vertices, style));
    // Add MapPin for each vertex
    builder.addAll(stream(vertices).map(point -> toMapPin(id, point, style)).toList());
    return builder.build();
  }
}
