package net.rcode.nanomaps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import net.rcode.nanomaps.TileSet.Record;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;

/**
 * An unmanaged MapContentView child that displays a grid of
 * bitmap tiles.
 * 
 * @author stella
 *
 */
public class MapTileView extends View implements MapStateAware, Tile.StateChangedListener {
	static final Paint CLEAR_PAINT=new Paint();
	static {
		CLEAR_PAINT.setARGB(0, 0, 0, 0);
	}

	private TileSelector selector;
	private TileSet currentTileSet=new TileSet();
	private TileSet oldTileSet=new TileSet();
	private ArrayList<TileKey> updatedKeys=new ArrayList<TileKey>(32);
	private ArrayList<TileSet.Record> newTileRecords=new ArrayList<TileSet.Record>(32);
	
	public MapTileView(Context context, TileSelector selector) {
		super(context);
		this.selector=selector;
	}
	
	public TileSelector getSelector() {
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
		currentTileSet.resetMarks();
		int right=getWidth()-1, bottom=getHeight()-1;
		
		// Clear our shared state for a new run
		boolean generatePreviews=full;
		updatedKeys.clear();
		newTileRecords.clear();
		
		// Select tile keys that intersect our display area
		selector.select(mapState.getProjection(),
				mapState.getResolution(),
				mapState.getViewportProjectedX(0, 0),
				mapState.getViewportProjectedY(0, 0),
				mapState.getViewportProjectedX(right, bottom),
				mapState.getViewportProjectedY(right, bottom),
				updatedKeys);

		// Match them up against what we are already displaying
		for (int i=0; i<updatedKeys.size(); i++) {
			TileKey key=updatedKeys.get(i);
			TileSet.Record record=currentTileSet.get(key);
			if (record==null) {
				record=currentTileSet.create(key);
				record.displayRect=new Rect();
				newTileRecords.add(record);
			} else {
				Log.d(Constants.LOG_TAG, "Recycling tile from TileSet: " + key);
			}
			record.marked=true;
			
			// Update its display rect
			mapTileToDisplay(mapState, key, record.displayRect);
		}
		
		// If we are generating previews, then we need to sweep
		// no longer used tiles into the oldTileSet and update their
		// display metrics so that the new tiles can use them for
		// previews
		if (generatePreviews && !newTileRecords.isEmpty()) {
			currentTileSet.sweepInto(oldTileSet);
			for (TileSet.Record record: oldTileSet.records()) {
				mapTileToDisplay(mapState, record.key, record.displayRect);
			}
		} else {
			// Just sweep
			currentTileSet.sweep();
		}
		
		// newTileRecords now contains all records that have been newly allocated.
		// We initialize them here in this ass-backwards way because we want to sort
		// them by proximity to the center but don't have the display information until
		// after we've iterated over all of them.  Think of this as the "initialize new
		// tiles" loop
		sortTileSetRecords(newTileRecords);
		for (int i=0; i<newTileRecords.size(); i++) {
			TileSet.Record record=newTileRecords.get(i);
			if (generatePreviews) {
				record.tile=selector.resolveWithPreview(record.key, record.displayRect, oldTileSet);
			} else {
				record.tile=selector.resolve(record.key);
			}
			if (record.tile.getState()!=Tile.STATE_LOADED) {
				record.tile.setStateChangedListener(MapTileView.this);
			}
		}

		// Remove/destroy any tiles that were not visited
		// Important that this comes after adding new since we generate
		// previews from old tiles
		currentTileSet.sweep();
		oldTileSet.clear();
		
		invalidate();
	}
	
	private class TileCentroidComparator implements Comparator<TileSet.Record> {
		private int centerY;
		private int centerX;
		public TileCentroidComparator() {
			this.centerX=getWidth()/2;
			this.centerY=getHeight()/2;
		}
		@Override
		public int compare(Record object1, Record object2) {
			int score1=Math.abs(object1.displayRect.centerX()-centerX) + Math.abs(object1.displayRect.centerY()-centerY);
			int score2=Math.abs(object2.displayRect.centerX()-centerX) + Math.abs(object2.displayRect.centerY()-centerY);
			return score1-score2;
		}
	}
	
	private void sortTileSetRecords(ArrayList<TileSet.Record> newTileRecords) {
		Collections.sort(newTileRecords, new TileCentroidComparator());
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
		double scaledSize=tile.getSize() * tile.getResolution() / mapState.getResolution();
		double left=mapState.projectedToDisplayX(tile.getScaledX() * tile.getResolution()) - mapState.getViewportOriginX();
		double top=mapState.projectedToDisplayY(tile.getScaledY() * tile.getResolution()) - mapState.getViewportOriginY();
		
		rect.left=(int) Math.round(left);
		rect.top=(int) Math.round(top);
		rect.right=rect.left + (int) Math.ceil(scaledSize);
		rect.bottom=rect.top + (int) Math.ceil(scaledSize);
	}
	
	
	@Override
	protected void onDraw(final Canvas canvas) {
		// On first draw we may not have had a map state update yet
		if (currentTileSet.isEmpty()) {
			mapStateUpdated(getMapState(), true);
		}
		
		Rect clip=canvas.getClipBounds();
		
		for (TileSet.Record record: currentTileSet.records()) {
			if (Rect.intersects(clip, record.displayRect)) {
				Log.d(Constants.LOG_TAG, "DRAW TILE: " + record.tile);
				Drawable drawable=record.tile.getDrawable();
				if (drawable!=null) {
					// Draw it
					drawable.setBounds(record.displayRect);
					drawable.draw(canvas);
				} else {
					// Clear the area
					Log.d(Constants.LOG_TAG, "Clearing " + record.displayRect);
					canvas.drawRect(record.displayRect, CLEAR_PAINT);
				}
			}
		}
	}

	@Override
	public void tileStateChanged(Tile tile) {
		Log.d(Constants.LOG_TAG, "Tile state changed: " + tile.getState());
		
		// If it is still in the current set, invalidate its bounds
		TileSet.Record record=currentTileSet.get(tile.getKey());
		if (record!=null) {
			invalidate(record.displayRect);
		}
	}
	
}
