nanomaps-droid
==============

This is a rough port of my nanomaps javascript library to android.  It follows the same philosophy that a map library is really less about the map and more about integrating easily into the native widget environment.  Most map libraries try to do everything for you.  The nanomaps libraries just try to be as fast as possible doing the basics of displaying map imagery and georeferencing your views.  Otherwise, they get out of the way.

I first opened the editor on this on April 2 and just committed enough of the basic implementation to display tiled maps and do concurrent pipelined http for the tiles on April 4th.  Next up is extending the MapContentView so that you can set the Latitude and Longitude in your LayoutParams.  At that point it should have just about everything it needs as far as an abstraction goes.

The core is factored so that the zoom levels are continuous.  Adding support for transitions is therefore pretty easy, but there is still a bit of work to do there.  The three transitions I will be adding are

* When switching between native zoom levels, generate composite preview tiles prior to throwing the old tiles away.  Most of what is needed is there but its just not done yet.  This will provide the smooth zoom with tiles that transition to their native resolution once loading.
* Alpha animation when drawing newly arrived tiles
* Animated pan/zoom to nearby areas

For now, to see it in action, load the projects up in eclipse and run the nanomaps-sample.
