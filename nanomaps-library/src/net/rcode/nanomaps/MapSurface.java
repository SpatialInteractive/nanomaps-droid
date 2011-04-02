package net.rcode.nanomaps;

import android.content.Context;
import android.graphics.Color;
import android.view.View;

/**
 * Manages a map surface.
 * 
 * @author stella
 *
 */
public class MapSurface extends View {
	private MapState mapState;
	
	public MapSurface(Context context) {
		super(context);
		mapState=new MapState(WebMercatorProjection.DEFAULT);
		mapState.setSurface(this);
		
		setBackgroundColor(Color.BLUE);
	}

	public MapState getMapState() {
		return mapState;
	}
}
