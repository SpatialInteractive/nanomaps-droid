package net.rcode.nanomaps;

import net.rcode.nanomaps.transition.LinearTransition;
import net.rcode.nanomaps.transition.TransitionController;
import net.rcode.nanomaps.util.Constants;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.RelativeLayout;

/**
 * Manages a map surface.  The MapSurface is a RelativeLayout that can
 * be used to position controls, etc.  It contains MapContentView children
 * corresponding to discrete display layers.
 * 
 * @author stella
 *
 */
public class MapSurface extends RelativeLayout implements MapStateAware, MapConstants {
	/**
	 * The background layer will contain background drawables, generally the
	 * thatch pattern that is below the map
	 */
	public static final int LAYER_BACKGROUND=0;
	
	/**
	 * The map layer should contain views for drawing the map (ie MapTileView)
	 * stacked in order of display.  Touch events are handled from this layer.
	 */
	public static final int LAYER_MAP=10;
	
	/**
	 * By convention, the SHADOW layer is where you will want to put overlay shadows
	 */
	public static final int LAYER_SHADOW=500;
	
	/**
	 * By convention, the OVERLAY layer is where map overlays and markers should
	 * be added
	 */
	public static final int LAYER_OVERLAY=1000;
	
	private TransitionController transitionController;
	static final boolean DEBUG=true;
	MapState mapState;
	boolean didFirstDraw;
	
	
	public MapSurface(Context context) {
		super(context);
		transitionController=new TransitionController();
		mapState=new MapState(WebMercatorProjection.DEFAULT);
		mapState.setListener(this);
		setBackgroundColor(Color.GRAY);
		
		// Allocate the background layer and set it up with
		// touch events
		MapLayer mapLayer=accessLayer(LAYER_MAP, true);
		setupTouchEvents(mapLayer);
	}

	/**
	 * Find a map layer by order, optionally creating it at the correct insertion
	 * point.  This method explicitly works for the no-layer case.
	 * @param order
	 * @param create
	 * @return layer or null (if !create)
	 */
	private MapLayer accessLayer(int order, boolean create) {
		int lastLayerIndex=-1;
		for (int i=0; i<getChildCount(); i++) {
			View child=getChildAt(i);
			if (child instanceof MapLayer) {
				MapLayer childLayer=(MapLayer) child;
				int childOrder=childLayer.getOrder();
				if (childOrder==order) return childLayer;
				if (childOrder>order) {
					// Insert here
					if (create) {
						childLayer=new MapLayer(getContext(), order);
						super.addView(childLayer, i, createFillLayoutParams());
						return childLayer;
					} else {
						return null;
					}
				} else {
					lastLayerIndex=i;
				}
			}
		}
		
		// not found
		if (create) {
			MapLayer childLayer=new MapLayer(getContext(), order);
			super.addView(childLayer, lastLayerIndex+1, createFillLayoutParams());
			return childLayer;
		} else {
			return null;
		}
	}
	
	public TransitionController getTransitionController() {
		return transitionController;
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
	
	/**
	 * Called by an attached MapState when the map state is changed.
	 * 
	 * @param full true if all geometry is invalid, false if just the origin has changed
	 * other geometry remains the same
	 */
	public void mapStateUpdated(MapState mapState, boolean full) {
		if (!didFirstDraw) return;
		
		// Any child can be MapStateAware - dispatch to those that are
		for (int i=0; i<getChildCount(); i++) {
			View child=getChildAt(i);
			if (child instanceof MapStateAware) {
				((MapStateAware)child).mapStateUpdated(mapState, full);
			}
		}
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
	public MapLayer getLayer(int order) {
		return accessLayer(order, true);
	}
	
	public int getMapMaxLevel() {
		return mapState.getProjection().getMaxLevel();
	}
	public int getMapMinLevel() {
		return mapState.getProjection().getMinLevel();
	}
	
	/**
	 * Translates a child's coordinates (as most often found in a MotionEvent)
	 * to global viewport coordinates on this map.
	 * @param point
	 * @return true if successful, false if the view is not on this map
	 */
	public boolean translateChildToMap(View child, Point point) {
		while (child!=this && child!=null) {
			point.x+=child.getLeft();
			point.y+=child.getTop();
			
			ViewParent parent=child.getParent();
			if (parent instanceof View) {
				child=(View) parent;
			} else {
				return false;
			}
		}
		return child==this;
	}
	
	/**
	 * @return A new linear transition
	 */
	public LinearTransition createTransition() {
		return new LinearTransition(mapState);
	}
	
	/**
	 * If a map transition animation is in progress, finish it, jumping
	 * directly to the final state.
	 */
	public void finishMapTransition() {
		transitionController.finish();
	}
	
	/**
	 * Clamp the given level to allowable min/max.
	 * @param level
	 * @return clamped zoom level
	 */
	public double clampMapZoom(double level) {
		double max=getMapMaxLevel();
		double min=getMapMinLevel();
		// Not much to do here - but letting an NaN in is like inviting
		// a vampire into your house
		if (Double.isNaN(level)) return max;
		
		if (level<min) return min;
		if (level>max) return max;
		return level;
	}
	
	/**
	 * @return current map zoom level
	 */
	public double getMapZoom() {
		return mapState.getLevel();
	}
	
	/**
	 * Set the map zoom level relative to an arbitrary position
	 * @param level (clamped to bounds)
	 */
	public void setMapZoom(double level, int x, int y) {
		transitionController.finish();
		mapState.setLevel(clampMapZoom(level), x, y);
	}

	/**
	 * Set the map zoom level relative to an the map center
	 * @param level (clamped to bounds)
	 */
	public final void setMapZoom(double level) {
		setMapZoom(level, getWidth()/2, getHeight()/2);
	}

	/**
	 * Set the map zoom via a transition at an arbitrary point.  
	 * Based on policy, the transition may be an animation or
	 * just a jump 
	 * @param toLevel
	 * @param x
	 * @param y
	 * @return true if animating, false if jump
	 */
	public boolean transitionMapZoom(double toLevel, int x, int y) {
		LinearTransition t=new LinearTransition(getMapState());
		t.getFinalMapState().setLevel(clampMapZoom(toLevel), x, y);
		transitionController.start(t, 250);
		return true;
	}
	
	/**
	 * Set the map zoom via a transition at map center.  
	 * Based on policy, the transition may be an animation or
	 * just a jump 
	 * @param toLevel
	 * @param x
	 * @param y
	 * @return true if animating, false if jump
	 */
	public boolean transitionMapZoom(double toLevel) {
		return transitionMapZoom(toLevel, getWidth()/2, getHeight()/2);
	}	

	/**
	 * Get the current map location in global coordinates at an
	 * arbitrary point on the viewport
	 * @param x
	 * @param y
	 * @return coordinates
	 */
	public Coordinate getMapLocation(int x, int y) {
		double gx=mapState.getViewportGlobalX(x, y);
		double gy=mapState.getViewportGlobalY(x, y);
		return Coordinate.xy(gx, gy);
	}
	
	/**
	 * Get the current map location in global coordinates at the center
	 * @return coordinates
	 */
	public Coordinate getMapLocation() {
		return getMapLocation(getWidth()/2, getHeight()/2);
	}
	
	/**
	 * Set the map location for an arbitrary pixel coordinate in
	 * the viewport.
	 * @param global
	 * @param x
	 * @param y
	 */
	public void setMapLocation(Coordinate global, int x, int y) {
		transitionController.finish();
		mapState.setViewportGlobal(global.getX(), global.getY(), x, y);
	}
	
	/**
	 * Set the map location at the center
	 * @param global
	 */
	public final void setMapLocation(Coordinate global) {
		setMapLocation(global, getWidth()/2, getHeight()/2);
	}
	
	/**
	 * Transition the map to a new location relative to arbitrary viewport
	 * coordinates.
	 * @param global
	 * @param x
	 * @param y
	 * @return true if transitioned, false if jumped
	 */
	public boolean transitionMapLocation(Coordinate global, int x, int y) {
		LinearTransition t=new LinearTransition(getMapState());
		t.getFinalMapState().setViewportGlobal(global.getX(), global.getY(), x, y);
		transitionController.start(t, 1000);
		return true;
	}
	
	/**
	 * Transition the map to a new location relative to the map center
	 * 
	 * @param global
	 * @return true if transitioned, false if jumped
	 */
	public final boolean transitionMapLocation(Coordinate global) {
		return transitionMapLocation(global, getWidth()/2, getHeight()/2);
	}
	
	/**
	 * Sets the map location and zoom in one operation at an arbitrary coordinate.
	 * @param global
	 * @param level
	 * @param x
	 * @param y
	 */
	public void setMapLocationZoom(Coordinate global, double level, int x, int y) {
		transitionController.finish();
		mapState.lock();
		mapState.setLevel(clampMapZoom(level), x, y);
		mapState.setViewportGlobal(global.getX(), global.getY(), x, y);
		mapState.unlock();
	}
	
	/**
	 * Sets the map location and zoom in one operation at map center.
	 * @param global
	 * @param level
	 */
	public final void setMapLocationZoom(Coordinate global, double level) {
		setMapLocationZoom(global, level, getWidth()/2, getHeight()/2);
	}

	/**
	 * Transition the map to an arbitrary new location and zoom level relative
	 * to arbitrary coordinates on the viewport.
	 * @param global
	 * @param level
	 * @param x
	 * @param y
	 * @return true if transitioning, false if jump
	 */
	public boolean transitionMapLocationZoom(Coordinate global, double level, int x, int y) {
		LinearTransition t=new LinearTransition(getMapState());
		
		t.getFinalMapState().setLevel(clampMapZoom(level), x, y);
		t.getFinalMapState().setViewportGlobal(global.getX(), global.getY(), x, y);
		
		transitionController.start(t, 1000);
		return true;
	}


	/**
	 * Transition the map to an arbitrary new location and zoom level relative
	 * to the map center
	 * @param global
	 * @param level
	 * @param x
	 * @param y
	 * @return true if transitioning, false if jump
	 */
	public final boolean transitionMapLocationZoom(Coordinate global, int level) {
		return transitionMapLocationZoom(global, level, getWidth()/2, getHeight()/2);
	}
	
}
