# Tiled cloud-optimized GeoTIFFs (TCOGs)

This package supports real-time and batch download and rendering of image tiles defined in a collection of specialized [cloud-optimized GeoTIFFs](http://cogeo.org) hosted in Google Cloud Storage or other HTTP file server.

The package supports a narrow subset of the COG spec and requires collections of COG to be structured according to specific rules. For convenience, we'll call such file collections "tiled COGs" or "TCOGs".

TCOG collections are organized as follows:

* The world is sliced along the extents of web mercator tile extents at a particular zoom level. The collection contains one non-overlapping TCOG for each of these extents.
* Each TCOG file contains one image for each zoom level, from the zoom level used for slicing (`tileSetMinZoomLevel`), up to the max. zoom level included in the collection (`tileSetMaxZoomLevel`).
* A "world TCOG" may also be defined to provide lower resolution images of the entire world from zoom level `0` up to `tileSetMinZoomLevel-1`.
* Each of these TCOGs is evenly divided into 256x256 JPEG compressed tiles.

<!-- TODO: Give example. -->
<!-- TODO: Provide illustration. -->

See [Map and Tile Coordinate])https://developers.google.com/maps/documentation/android-sdk/coordinates) in the Google Maps Platform docs for info.

* Structure: One or more COGs rendered with extents of web mercator tiles at a particular zoom level.

All files in the collection must also satisfy the following constraints:

* File format: [Cloud optimized GeoTIFF](https://github.com/cogeotiff/cog-spec/blob/master/spec.md)
* Projection: Web Mercator ([EPSG:3857](https://epsg.io/3857))
* Tile size: 256x256
* Image compression: JPEG
* ACLs: Public read access

<!-- TODO: Include example usage. -->