package net.rcode.nanomaps;

import net.rcode.nanomaps.util.Constants;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

/**
 * MapContentViews are direct parents of a MapSurface and are responsible
 * for managing the layout and display of children that are either
 * responsible for managing their own geographic context or have LayoutParams
 * indicating their position.
 * <p>
 * Each MapContentView occupies the entire area of its parent.
 * 
 * @author stella
 *
 */
public class MapContentView extends ViewGroup implements MapStateAware {
	public static class LayoutParams extends ViewGroup.LayoutParams {
		public boolean managed;
		
		/**
		 * Creates an unmanaged LayoutParams.  The view will be completely
		 * responsible for its own geographic positioning and will be laid
		 * out to fill its parent.
		 */
		public LayoutParams() {
			super(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
			managed=false;
		}
	}
	
	public MapContentView(Context context) {
		super(context);
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
		return p!=null && p instanceof LayoutParams;
	}
	
	@Override
	protected android.view.ViewGroup.LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams();
	}
	
	public void mapStateUpdated(MapState mapState, boolean full) {
		Log.d(Constants.LOG_TAG, "MapContentView.mapStateUpdated(" + full + ")");
		int childCount=getChildCount();
		for (int i=0; i<childCount; i++) {
			View child=getChildAt(i);
			if (child instanceof MapStateAware) {
				// The child handles everything about its mapstate
				((MapStateAware)child).mapStateUpdated(mapState, full);
			} else {
				// TODO: Need to handle it ourselves
			}
		}
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		int childCount=getChildCount();
		for (int i=0; i<childCount; i++) {
			View child=getChildAt(i);
			LayoutParams lp=(LayoutParams)child.getLayoutParams();
			
			// Unmanaged view - occupies the entire parent area
			// and has no other layout done to it
			if (!lp.managed) {
				child.layout(l, t, r, b);
				continue;
			}
		}
	}

}
