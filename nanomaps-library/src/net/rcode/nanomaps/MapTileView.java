package net.rcode.nanomaps;

import net.rcode.nanomaps.MapTileSelector.TileKey;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
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
	private static final int COLORS[] = new int[] {
		Color.BLUE, Color.GREEN, Color.CYAN, Color.LTGRAY, Color.RED, Color.MAGENTA, Color.YELLOW
	};
	
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
	
	/**
	 * Given a MapState and TileKey, fill in a Rect with the pixel coordinates
	 * of the tile for the current state.
	 * This assumes rectangular display.  For rotation, this will all need to
	 * be reworked.
	 * @param mapState
	 * @param tile
	 */
	static void mapTileToDisplay(MapState mapState, TileKey tile, Rect rect) {
		double scaledSize=tile.size * tile.resolution / mapState.getResolution();
		double left=mapState.projectedToDisplayX(tile.scaledX * tile.resolution) - mapState.getViewportOriginX();
		double top=mapState.projectedToDisplayY(tile.scaledY * tile.resolution) - mapState.getViewportOriginY();
		
		rect.left=(int) Math.round(left);
		rect.top=(int) Math.round(top);
		rect.right=rect.left + (int) Math.ceil(scaledSize);
		rect.bottom=rect.top + (int) Math.ceil(scaledSize);
	}
	
	private class Blitter implements MapTileSelector.Handler {
		private MapState mapState;
		private Canvas canvas;
		
		public Blitter(MapState mapState, Canvas canvas) {
			this.mapState=mapState;
			this.canvas=canvas;
		}

		@Override
		public void handleTile(TileKey tile) {
			Rect rect=new Rect();
			mapTileToDisplay(mapState, tile, rect);
			Log.d(Constants.LOG_TAG, "Blitter tile: " + tile + " -> " + rect);
			
			Paint paint=new Paint();
			paint.setColor(COLORS[tile.hashCode() % COLORS.length]);
			canvas.drawRoundRect(new RectF(rect), 10, 10, paint);
			
			paint.setColor(Color.BLACK);
			canvas.drawText(tile.toString(), rect.left+5, rect.centerY(), paint);
		}
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		MapState mapState=getMapState();
		
		Rect bounds=canvas.getClipBounds();
		Log.d(Constants.LOG_TAG, "MapTileView.onDraw(clip=" + canvas.getClipBounds() + ")");
		
		Blitter blitter=new Blitter(mapState, canvas);
		
		selector.select(mapState.getProjection(),
				mapState.getResolution(),
				mapState.getViewportProjectedX(bounds.left, bounds.top),
				mapState.getViewportProjectedY(bounds.left, bounds.top),
				mapState.getViewportProjectedX(bounds.right, bounds.bottom),
				mapState.getViewportProjectedY(bounds.right, bounds.bottom),
				blitter);
	}
	
}
