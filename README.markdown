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

* Add to MapContentView so it supports setting LayoutParams with global coordinates so we can actually put views on the map
* Rotation.  This has been considered and shouldn't be that hard.  The core will handle the necessary math but most views will display in their standard orientation.  The TileMapView will be extended to be orientation aware so that the map itself rotates.

