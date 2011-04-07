nanomaps-droid
==============

This is a rough port of my nanomaps javascript library to android.  It follows the same philosophy that a map library is really less about the map and more about integrating easily into the native widget environment.  Most map libraries try to do everything for you.  The nanomaps libraries just try to be as fast as possible doing the basics of displaying map imagery and georeferencing your views.  Otherwise, they get out of the way.

The project was started on 4/2/2011, so you can get an idea of where its at.

Core rendering and IO management is mostly done and features the following:

* Continuous zoom levels so map can be displayed at arbitrary resolution
* Recompositing tiles on the fly when changing zoom levels to display rough previews
* Animated transitions between zoom levels and arbitrary pan+zoom operations
* HTTP pipelining for requesting tiles

Links
-----

* [JavaDoc](http://stellaeof.github.com/nanomaps-droid/javadoc/)
* [Current Jar File](http://stellaeof.github.com/nanomaps-droid/download/nanomaps-droid-0.1.0.jar)
* [All Jar Files](https://github.com/stellaeof/nanomaps-droid/tree/gh-pages/download)
* [Stupid Sample App APK](http://stellaeof.github.com/nanomaps-droid/download/nanomaps-sample-0.1.0.apk) - You can drag the pink pin around, swipe the uncertainty cone to move it and hit some buttons to play with transitions

Screenshot
----------

![Screenshot](http://stellaeof.github.com/nanomaps-droid/images/screenshot1.png)

Next up
-------

* Rotation.  This has been considered and shouldn't be that hard.  The core will handle the necessary math but most views will display in their standard orientation.  The TileMapView will be extended to be orientation aware so that the map itself rotates.
* Transition Policies.  Calling the transition* methods always starts an animation.  There should be a policy class that makes decisions to animate and for how long based on the parameters.
* Tile Caching.  The UriTileSelector can take a ResourceLoader, but the only implementation goes straight out to the network.  I will add a ResourceLoader implementation that interacts with a cache and then delegates to the DefaultResourceLoader.
* Network State.  The DefaultResourceLoader manages its IO tasks in queues.  If the network is down, it will quickly just fail all of the requests.  There should be some intelligence here to pause the http queues until the network resumes.  This shouldn't create a backlog of requests because as a request is not needed anymore, the TileMapView will cancel the request.
* Scrolling Background.  Just needs a simple hatch background that scrolls with the map.


Example usage
-------------

	// Parent container
	FrameLayout frame=new FrameLayout(this);

	// Construct the map
	final MapSurface map=new MapSurface(this);
	frame.addView(map, new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
	map.setMapLocationZoom(Coordinate.latLng(47.616159, -122.320944), 15);

	// Add tile layer using MapQuest OSM Tiles
	MapTileView mtv=new MapTileView(this, new UriTileSelector("http://otile${modulo:1}.mqcdn.com/tiles/1.0.0/osm/${level}/${tileX}/${tileY}.png"));
	map.getLayer(MapSurface.LAYER_MAP).addView(mtv);

	// Add a shiny pink marker anchored at its bottom center
	ImageView marker1=new ImageView(this);
	marker1.setImageResource(R.drawable.pin_pink);
	map.addOverlay(marker1, MapSurface.LAYER_OVERLAY, Coordinate.latLng(47.61402, -122.318457),
					MapSurface.BIAS_CENTER, MapSurface.BIAS_BOTTOM);
	attachClickEvents(map, marker1);

	// Add a shiny blue orb marker anchored at its center
	ImageView marker2=new ImageView(this);
	marker2.setImageResource(R.drawable.orb_blue);
	map.addOverlay(marker2, MapSurface.LAYER_OVERLAY, Coordinate.latLng(47.616523, -122.318407),
					MapSurface.BIAS_CENTER, MapSurface.BIAS_CENTER);

	// Add an "uncertainty" circle with a radius of 60m
	CircleOverlay cov=new CircleOverlay(this);
	cov.setRadius(60);
	map.addOverlay(cov, MapSurface.LAYER_OVERLAY, Coordinate.latLng(47.615016, -122.316574));

	// Add an "uncertainty circle plus heading cone" with radius
	// of 120m, cone overshoot of 10px, heading of 45 degrees and uncertainty of 20 degrees
	UncertaintyHeadingOverlay ohv=new UncertaintyHeadingOverlay(this);
	ohv.setRadius(120);
	ohv.setHeadingConeOvershoot(10);
	ohv.setHeading(45, 20);
	map.addOverlay(ohv, MapSurface.LAYER_OVERLAY, Coordinate.latLng(47.619095, -122.321499));
	attachChangeHeadingEvents(ohv);

Example Tile Uris
-----------------

* MapQuest OSM Tiles: http://otile${modulo:1}.mqcdn.com/tiles/1.0.0/osm/${level}/${tileX}/${tileY}.png
* Microsoft Aerial Tiles: http://h0.ortho.tiles.virtualearth.net/tiles/h${quadkey}.jpeg?g=131  (requires license/permission)

License
-------
(MIT with additional restriction)

Copyright (c) 2010 Stella Laurenzo, http://stella.laurenzo.org

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

As an additional restriction, it is not permitted to use this software in
a way that makes unlicensed use of anyone's content or infringes on third party
intellectual property (ie. make sure you have permission to display tiles
and or other map related content).

