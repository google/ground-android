# Google Maps Optimized GeoTIFFs (MOGs)

Google Maps Optimized GeoTIFFs (MOGs) are normal [Cloud Optimized GeoTIFFs](http://cogeo.org) which
have been specially clipped and configured for download and rendering in Google Maps Platform. This
package supports real-time and batch download and rendering of map tiles defined in collections of
MOGs hosted in Google Cloud Storage or other HTTP file server.

In this model, these specialized COGs are treated as tile pyramids, while each full-resolution and
overview image in a COG provide a tile matrix for particular zoom level.

To be considered a MOG, a file must satisfy the following constraints:

* Format: [Cloud optimized GeoTIFF](https://github.com/cogeotiff/cog-spec/blob/master/spec.md)
* Projection: Web Mercator ([EPSG:3857](https://epsg.io/3857))
* Tile size: 256x256
* Image compression: JPEG
* Cloud Storage ACLs: Public read access
* Extent: Clipped to the exact bounds of a web mercator tile. This ensures contained images tiles
  align properly with the web tile coordinates system.

* Note that since the MOGs are sliced exactly along the extents of web mercator tiles, the width and
  height of images at each zoom level are always integer multiples of 256, with the lowest
  resolution image in each MOG consisting of a single 256x256 tile.

To simplify discovery and retrieval, MOGs are organized in collections structured as follows:

* *Hi-res MOGs*: A series of MOGs partitioned by the extents of web mercator tiles at a particular
  zoom level (`hiResMogMinZoom`). A MOG collection consists of one or more MOG files clipped to
  these extents. As such, each MOG can be uniquely identified by the (X, Y) coordinates of the web
  tile used to determine the extents of the MOG tile pyramid. Each hi-res MOGs contains one image
  for each zoom level from `hiResMogMinZoom` to `hiResMogMaxZoom`.
* *World MOG*: A single lower resolution MOG whose extents cover the entire web mercator coordinates
  space, or tile (0, 0) at zoom level 0. The world MOG consists of one image for each zoom level
  from `0` to `hiResMogMinZoom - 1`.

See [Map and Tile Coordinates](https://developers.google.com/maps/documentation/android-sdk/coordinates)
in the Google Maps Platform docs for info.

