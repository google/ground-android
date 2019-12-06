package com.google.android.gnd.persistence.local.room;

import androidx.room.TypeConverter;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

public class LatLngBoundsConverter {
    @TypeConverter
    public static LatLngBounds boundsFromString(String id) {
        String[] coords = id.split(",");
        LatLng NE = new LatLng(Double.parseDouble(coords[0]), Double.parseDouble(coords[1]));
        LatLng SW = new LatLng(Double.parseDouble(coords[2]), Double.parseDouble(coords[3]));
        return new LatLngBounds(NE, SW);
    }

    @TypeConverter
    public static String boundsToString(LatLngBounds bounds) {
        return bounds.toString();
    }
}
