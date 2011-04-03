package net.rcode.nanomaps;

import net.rcode.nanomaps.MapTileSelector.TileKey;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;

/**
 * An unmanaged MapContentView child that displays a grid of
 * bitmap tiles.
 * 
 * @author stella
 *
 */
public class MapTileView extends View implements MapStateAware {
	private MapTileSelector selector;

	public MapTileView(Context context, MapTileSelector selector) {
		super(context);
		this.selector=selector;
	}
	
	public MapTileSelector getSelector() {
		return selector;
	}
	
	public MapContentView getContentView() {
		return (MapContentView) getParent();
	}
	
	public MapState getMapState() {
		return getContentView().getMapState();
	}
	
	@Override
	public void mapStateUpdated(MapState mapState, boolean full) {
		invalidate();
	}
	
	private class Blitter implements MapTileSelector.Handler {
		private Canvas canvas;
		
		public Blitter(Canvas canvas) {
			this.canvas=canvas;
		}

		@Override
		public void handleTile(TileKey key) {
			Log.d(Constants.LOG_TAG, "Blitter tile: " + key);
		}
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		MapState mapState=getMapState();
		
		Rect bounds=canvas.getClipBounds();
		Log.d(Constants.LOG_TAG, "MapTileView.onDraw(clip=" + canvas.getClipBounds() + ")");
		
		Blitter blitter=new Blitter(canvas);
		
		selector.select(mapState.getProjection(),
				mapState.getResolution(),
				mapState.getViewportProjectedX(bounds.left, bounds.top),
				mapState.getViewportProjectedY(bounds.left, bounds.top),
				mapState.getViewportProjectedX(bounds.right, bounds.bottom),
				mapState.getViewportProjectedY(bounds.right, bounds.bottom),
				blitter);
	}
	
}
