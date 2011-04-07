nanomaps-droid
==============

This is a rough port of my nanomaps javascript library to android.  It follows the same philosophy that a map library is really less about the map and more about integrating easily into the native widget environment.  Most map libraries try to do everything for you.  The nanomaps libraries just try to be as fast as possible doing the basics of displaying map imagery and georeferencing your views.  Otherwise, they get out of the way.

The project was started on 4/2/2011, so you can get an idea of where its at.

Core rendering and IO management is mostly done and features the following:

* Continuous zoom levels so map can be displayed at arbitrary resolution
* Recompositing tiles on the fly when changing zoom levels to display rough previews
* Animated transitions between zoom levels and arbitrary pan+zoom operations
* HTTP pipelining for requesting tiles

Next up:

* Rotation.  This has been considered and shouldn't be that hard.  The core will handle the necessary math but most views will display in their standard orientation.  The TileMapView will be extended to be orientation aware so that the map itself rotates.

Example usage:
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

