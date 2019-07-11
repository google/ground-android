package com.cocoahero.android.gmaps.addons.util;

import java.util.List;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;

public class PolygonUtils {

    public static LatLng getCentroid(Polygon polygon) {
        List<LatLng> points = polygon.getPoints();

        if (points.size() <= 2) {
            return null;
        }
        
        double x0 = 0.0;
        double y0 = 0.0;
        double x1 = 0.0;
        double y1 = 0.0;
        double wa = 0.0;
        double sa = 0.0;
        double cx = 0.0;
        double cy = 0.0;

        int i = 0;

        for (i = 0; i < points.size() - 1; ++i) {
            x0 = points.get(i).latitude;
            y0 = points.get(i).longitude;
            x1 = points.get(i + 1).latitude;
            y1 = points.get(i + 1).longitude;

            sa += wa = (x0 * y1) - (x1 * y0);

            cx += (x0 + x1) * wa;
            cy += (y0 + y1) * wa;
        }

        x0 = points.get(i).latitude;
        y0 = points.get(i).longitude;
        x1 = points.get(0).latitude;
        y1 = points.get(0).longitude;

        sa += wa = (x0 * y1) - (x1 * y0);

        cx += (x0 + x1) * wa;
        cy += (y0 + y1) * wa;

        sa *= 0.5;

        cx /= (6.0 * sa);
        cy /= (6.0 * sa);

        return new LatLng(cx, cy);
    }

}
