package net.rcode.nanomaps;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.widget.RelativeLayout;

/**
 * Manages a map surface.  The MapSurface is a RelativeLayout that can
 * be used to position controls, etc.  It contains MapContentView children
 * corresponding to discrete display layers.
 * 
 * @author stella
 *
 */
public class MapSurface extends RelativeLayout implements MapStateAware {
	static final boolean DEBUG=true;
	MapState mapState;
	MapContentView backgroundLayer;
	
	public MapSurface(Context context) {
		super(context);
		mapState=new MapState(WebMercatorProjection.DEFAULT);
		mapState.setListener(this);
		setBackgroundColor(Color.GRAY);
		
		// Allocate the background layer
		backgroundLayer=createContentView();
		addView(backgroundLayer, createFillLayoutParams());
	}

	private MapContentView createContentView() {
		MapContentView mcv=new MapContentView(getContext());
		return mcv;
	}

	private LayoutParams createFillLayoutParams() {
		LayoutParams lp=new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		return lp;
	}
	
	public MapState getMapState() {
		return mapState;
	}
	
	public MapContentView getBackgroundLayer() {
		return backgroundLayer;
	}
	
	/**
	 * Called by an attached MapState when the map state is changed.
	 * 
	 * @param full true if all geometry is invalid, false if just the origin has changed
	 * other geometry remains the same
	 */
	public void mapStateUpdated(MapState mapState, boolean full) {
		if (DEBUG) Log.d(Constants.LOG_TAG, "mapStateUpdated(" + full + ")");
		
		// TODO: Iterate over all content views
		backgroundLayer.mapStateUpdated(mapState, full);
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		
		// TODO: We want to preserve center on size change
		if (DEBUG) Log.d(Constants.LOG_TAG, "onSizeChanged(" + w + "," + h + ")");
		mapState.setViewportSize(w, h);
	}
}
