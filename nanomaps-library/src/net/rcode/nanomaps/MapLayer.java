package net.rcode.nanomaps;

import net.rcode.nanomaps.util.Constants;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

/**
 * MapLayers are direct parents of a MapSurface and are responsible
 * for managing the layout and display of children that are either
 * responsible for managing their own geographic context or have LayoutParams
 * indicating their position.
 * <p>
 * Each MapLayer occupies the entire area of its parent.
 * <p>
 * The MapLayer is a ViewGroup but overrides much ViewGroup behavior,
 * specifically relating to child drawing.  Since the MapLayer will consist
 * of many children that are not immediately visible, it opimizes child
 * drawing pretty heavily.  As a result, there is a lot of default behavior
 * that comes with a ViewGroup that may not work out of the box on a MapLayer
 * (ie. Animation, scroll offsets, etc).
 * 
 * @author stella
 *
 */
public class MapLayer extends ViewGroup implements MapStateAware, MapConstants {
	protected static class BaseLayoutParams extends ViewGroup.LayoutParams {
		public BaseLayoutParams() {
			super(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		}
		
		public boolean isManaged() {
			return false;
		}
	}
	public static class LayoutParams extends BaseLayoutParams {
		@Override
		public boolean isManaged() {
			return true;
		}
		
		// -- managed parameters
		/**
		 * For managed children, this is the location of the view's anchor
		 * point expressed in global coordinates
		 */
		public final Coordinate location;
		
		/**
		 * Offset in pixel space of the view relative to the anchor location.
		 * Can be OR'd with MapConstants.ALIGN_ constants to change what the
		 * measurement is relative to.
		 * Think of the result as the pixel coordinates in view-space where the
		 * anchor location is.
		 */
		public final int x, y;
		
		// -- internal cached settings for performance
		boolean initialized;
		
		/**
		 * The location is in global coordinates.  Here we cache the projected coordinates
		 * for whenever it changes
		 */
		double projectedX;
		double projectedY;
		boolean projectedSet;
		
		/**
		 * Resolution at which layout parameters were cached
		 */
		double resolution;
		
		/**
		 * Flag that can be ticked to false if this child is invalid for
		 * some reason and shouldn't be considered in managed layout calls.
		 */
		boolean shouldConsider;
		
		/**
		 * X and Y coordinates of the anchor location in display coordinates
		 */
		int displayX;
		int displayY;
		
		/**
		 * True if the view is in the viewport now
		 */
		boolean inView;
		
		/**
		 * Creates an unmanaged LayoutParams.  The view will be completely
		 * responsible for its own geographic positioning and will be laid
		 * out to fill its parent.
		 */
		public LayoutParams() {
			this.location=null;
			this.x=0; this.y=0;
		}
		
		/**
		 * Create a managed LayoutParams for displaying the view at the given
		 * global location using the view's native width/height
		 * @param location
		 */
		public LayoutParams(Coordinate location) {
			this.location=location;
			this.x=0;
			this.y=0;
		}
		
		public LayoutParams(Coordinate location, int x, int y) {
			this.location=location;
			this.x=x;
			this.y=y;
		}
		
		public LayoutParams modify(Coordinate newLocation) {
			return new LayoutParams(newLocation, x, y);
		}
		
		/**
		 * Updates the map layout info on a child's LayoutParams.  The LayoutParams
		 * will continue to be valid so long as the map's "vital" parameters don't change.
		 * Currently, this is just resolution but will include rotation when added.
		 * @param mapState
		 * @param child
		 * @param lp
		 */
		protected void updateFromMapState(MapState mapState, int childWidth, int childHeight, boolean force) {
			// Only update if vital stats have changed
			double resolution=mapState.getResolution();
			if (!force && this.resolution==resolution) return;
			
			// Update projected coordinates if needed
			if (!this.projectedSet) {
				if (this.location==null) {
					this.shouldConsider=false;
					return;
				}
				
				this.projectedX=mapState.getProjection().forwardX(this.location.getX());
				this.projectedY=mapState.getProjection().forwardY(this.location.getY());
				this.projectedSet=true;
			}
			
			this.resolution=resolution;
			this.shouldConsider=true;
			this.displayX=(int) Math.round(mapState.projectedToDisplayX(this.projectedX));
			this.displayY=(int) Math.round(mapState.projectedToDisplayY(this.projectedY));
			
			// Bias the displayX/Y by x and y settings
			int biasX=decodeBias(this.x, childWidth);
			int biasY=decodeBias(this.y, childHeight);
			
			this.displayX-=biasX;
			this.displayY-=biasY;
			
			this.initialized=true;
		}
	}

	private static int decodeBias(int align, int max) {
		int modifier=align & BIAS_MASK;
		align=align & ~ BIAS_MASK;
		
		switch (modifier) {
		case BIAS_NONE:
		case BIAS_LEFT:
			break;
		case BIAS_RIGHT:
			align=max-align;
			break;
		case BIAS_CENTER:
			align=max/2 + align;
			break;
		}
		return align;
	}
	
	// -- Pre-allocated objects used in draw
	private Rect viewportRect=new Rect();
	private Rect childRect=new Rect();
	private Rect clipRect=new Rect();
	
	private int order;
	
	public MapLayer(Context context, int order) {
		super(context);
		this.order=order;
	}
	
	public final int getOrder() {
		return order;
	}
	
	/**
	 * @return The owning MapSurface (must be the immediate parent)
	 */
	public MapSurface getMapSurface() {
		return (MapSurface) getParent();
	}
	
	public MapState getMapState() {
		return getMapSurface().getMapState();
	}
	
	@Override
	protected boolean checkLayoutParams(android.view.ViewGroup.LayoutParams p) {
		return p!=null && p instanceof BaseLayoutParams;
	}
	
	@Override
	protected android.view.ViewGroup.LayoutParams generateDefaultLayoutParams() {
		return new BaseLayoutParams();
	}
	
	public void mapStateUpdated(MapState mapState, boolean full) {
		int childCount=getChildCount();
		for (int i=0; i<childCount; i++) {
			View child=getChildAt(i);
			BaseLayoutParams blp=(BaseLayoutParams) child.getLayoutParams();
			// If the child supports MapStateAware, pass it on
			if (child instanceof MapStateAware) {
				// The child handles everything about its mapstate
				((MapStateAware)child).mapStateUpdated(mapState, full);
			}				
			
			// Update managed layout
			if (blp.isManaged()) {
				((LayoutParams)blp).updateFromMapState(mapState, child.getMeasuredWidth(), child.getMeasuredHeight(), false);
			}
		}
		invalidate();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		Log.d(Constants.LOG_TAG, "onMeasure(" + widthMeasureSpec + "," + heightMeasureSpec);
		
		// Go through all of the children and measure them.  We don't use ViewGroup
		// support methods for this because they do a lot of extra work assuming that
		// views want to match their parent's layout in some way and we just want
		// natural size
		int childCount=getChildCount();
		for (int i=0; i<childCount; i++) {
			View child=getChildAt(i);
			child.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
		}
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		int childCount=getChildCount();
		for (int i=0; i<childCount; i++) {
			View child=getChildAt(i);
			if (child.getVisibility()!=View.VISIBLE) continue;
			
			BaseLayoutParams lp=(BaseLayoutParams)child.getLayoutParams();
			
			if (!lp.isManaged()) {
				// Unmanaged view - occupies the entire parent area
				// and has no other layout done to it
				child.layout(l, t, r, b);
			} else {
				// Managed layouts respect the size their children want to be
				// All other work happens in draw()
				child.layout(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight());
			}
		}
	}
	
	@Override
	protected void dispatchDraw(Canvas canvas) {
		MapState ms=getMapState();
		
		canvas.getClipBounds(clipRect);
		
		// Set viewportRect to the integral display coordinates of the damaged area of
		// the viewport.  We are only interested in managed children in this rect.
		// Integers here are fine.  We only have higher precision in the mapstate to make
		// sure we don't suffer rounding errors on transition
		viewportRect.left=(int)Math.round(ms.getViewportOriginX());
		viewportRect.top=(int)Math.round(ms.getViewportOriginY());
		viewportRect.right=viewportRect.left + ms.getViewportWidth();
		viewportRect.bottom=viewportRect.top + ms.getViewportHeight();
		
		int childCount=getChildCount();
		for (int i=0; i<childCount; i++) {
			View child=getChildAt(i);
			BaseLayoutParams blp=(BaseLayoutParams) child.getLayoutParams();
			if (!blp.isManaged()) {
				// For unmanaged views, the world's a stage.  Give it to her.
				drawChild(canvas, child, getDrawingTime());
				continue;
			}
			
			// If here, then the view is managed
			LayoutParams mlp=(LayoutParams) blp;
			if (!mlp.initialized) {
				// First draw - refresh cache
				mlp.updateFromMapState(ms, child.getMeasuredWidth(), child.getMeasuredHeight(), true);
			}
			if (!mlp.shouldConsider) continue;
			
			// Compute childRect in display coordinates
			childRect.left=mlp.displayX;
			childRect.top=mlp.displayY;
			childRect.right=childRect.left + child.getMeasuredWidth();
			childRect.bottom=childRect.top + child.getMeasuredHeight();
			
			// Ignore if not in the map display area
			if (!Rect.intersects(viewportRect, childRect)) continue;
			
			// Shift the childRect to viewport coordinates
			childRect.offset(-viewportRect.left, -viewportRect.top);
			
			// Not in damage area
			if (!Rect.intersects(childRect, clipRect)) continue;

			// Update the child's left/top parameters
			setChildPosition(child, childRect.left, childRect.top);

			// Setup the canvas to receive it
			int saveState=canvas.save();
			// No need to translate because we set the child position
			drawChild(canvas, child, getDrawingTime());
			
			canvas.restoreToCount(saveState);
		}
	}

	/**
	 * Yes this is for real.  Methods to directly set left/top were not in place
	 * until api 11.  We're counting on the fact that directly setting these
	 * vs calling setFrame just updates internal pointers. IE: No layout happens
	 * @param left
	 * @param top
	 */
	private void setChildPosition(View child, int left, int top) {
		child.offsetLeftAndRight(left-child.getLeft());
		child.offsetTopAndBottom(top-child.getTop());
	}
}
