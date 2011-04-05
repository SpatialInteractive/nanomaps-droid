package net.rcode.nanomaps;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
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
	boolean didFirstDraw;
	
	public MapSurface(Context context) {
		super(context);
		mapState=new MapState(WebMercatorProjection.DEFAULT);
		mapState.setListener(this);
		setBackgroundColor(Color.GRAY);
		
		// Allocate the background layer
		backgroundLayer=createContentView();
		addView(backgroundLayer, createFillLayoutParams());
		setupTouchEvents(backgroundLayer);
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
		if (!didFirstDraw) return;
		
		if (DEBUG) Log.d(Constants.LOG_TAG, "mapStateUpdated(" + full + ")");
		
		// TODO: Iterate over all content views
		backgroundLayer.mapStateUpdated(mapState, full);
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		
		// Preserve the center point on size change
		double oldx=mapState.getViewportDisplayX(oldw/2, oldh/2);
		double oldy=mapState.getViewportDisplayY(oldw/2, oldh/2);
		
		mapState.setViewportSize(w, h);
		
		mapState.setViewportDisplay(oldx, oldy, w/2, h/2);
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		didFirstDraw=false;
	}
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		didFirstDraw=false;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		// Make sure to get map update events primed on first draw
		if (!didFirstDraw) {
			didFirstDraw=true;
			mapStateUpdated(mapState, true);
		}
		super.onDraw(canvas);
	}
	
	// -- touch handling
	protected static final int TOUCH_STATE_NONE=0;
	protected static final int TOUCH_STATE_SINGLE=1;
	protected int touchState;
	protected float touchAnchorX, touchAnchorY;
	
	protected void setupTouchEvents(final View target) {
		target.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				handleTouchEvent(event);
				return true;
			}
		});
	}
	
	
	protected void handleTouchEvent(MotionEvent event) {
		// Newer api levels support multi-touch and getters to separate them.
		// We just do the bitwise math.  Lower 8bits==action, next 8bits==pointer index.
		int action=event.getAction() & 0xff;
		int pid=(event.getAction() & 0xff00) >> 8;
		
		switch (action) {
		case MotionEvent.ACTION_MOVE:
			// Accumulate
			if (pid==0) {
				handleTouchMovePrimary(event);
			}
			break;
		case MotionEvent.ACTION_DOWN:
			// Initiate a gesture
			if (pid==0) {
				handleTouchStart(event);
			}
			break;
		case MotionEvent.ACTION_UP:
			// End the gesture
			if (pid==0) {
				handleTouchDone(event, false);
			}
			break;
			
		case MotionEvent.ACTION_CANCEL:
			// Framework cancelled the gesture
			if (pid==0) {
				handleTouchDone(event, true);
			}
			break;
		}
	}
	
	protected void clearTouchState() {
		touchState=TOUCH_STATE_NONE;
	}
	
	protected void handleTouchStart(MotionEvent event) {
		clearTouchState();
		touchState=TOUCH_STATE_SINGLE;
		touchAnchorX=event.getX();
		touchAnchorY=event.getY();
		if (DEBUG) Log.d(Constants.LOG_TAG, "Start touch: " + event);
	}
	
	protected void handleTouchMovePrimary(MotionEvent event) {
		if (touchState==TOUCH_STATE_SINGLE) {
			//if (DEBUG) Log.d(Constants.LOG_TAG, "Touch move: " + event);
			
			float currentX=event.getX();
			float currentY=event.getY();
			
			float deltaX=touchAnchorX - currentX;
			float deltaY=touchAnchorY - currentY;
			
			// Update the map state
			mapState.moveViewport(deltaX, deltaY);
			touchAnchorX=currentX;
			touchAnchorY=currentY;
		}
	}
	
	protected void handleTouchDone(MotionEvent event, boolean cancelled) {
		if (DEBUG) Log.d(Constants.LOG_TAG, "Touch done: " + event);

		clearTouchState();
	}
	
	// -- public api
	public int getMapMaxLevel() {
		return mapState.getProjection().getMaxLevel();
	}
	public int getMapMinLevel() {
		return mapState.getProjection().getMinLevel();
	}
	
}
