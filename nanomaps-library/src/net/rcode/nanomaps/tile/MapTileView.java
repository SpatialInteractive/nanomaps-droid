package net.rcode.nanomaps.tile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import net.rcode.nanomaps.MapLayer;
import net.rcode.nanomaps.MapState;
import net.rcode.nanomaps.MapStateAware;
import net.rcode.nanomaps.tile.TileSet.Record;
import net.rcode.nanomaps.transition.Transition;
import net.rcode.nanomaps.transition.TransitionController;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * An unmanaged MapContentView child that displays a grid of
 * bitmap tiles.
 * 
 * @author stella
 *
 */
public class MapTileView extends View implements MapStateAware, Tile.StateChangedListener, Transition.Callback {
	private static final boolean DEBUG_BOUNDS=false;
	
	static final Paint CLEAR_PAINT=new Paint();
	static {
		CLEAR_PAINT.setARGB(0, 0, 0, 0);
	}
	
	private MapState mapState;
	private TileSelector selector;
	private TileSet currentTileSet=new TileSet();
	private TileSet transitionTileSet=new TileSet();
	private boolean transitionLocked=false;
	private TileSet oldTileSet=new TileSet();
	private ArrayList<TileKey> updatedKeys=new ArrayList<TileKey>(32);
	private ArrayList<TileSet.Record> newTileRecords=new ArrayList<TileSet.Record>(32);
	private TransitionController transitionController;
	
	public MapTileView(Context context, TileSelector selector) {
		super(context);
		this.selector=selector;
	}
	
	public TileSelector getSelector() {
		return selector;
	}
	
	public void setSelector(TileSelector selector) {
		if (selector==this.selector) return;
		this.selector=selector;
		
		// Reset state
		currentTileSet.clear();
		transitionTileSet.clear();
		transitionLocked=false;
		oldTileSet.clear();
		updatedKeys.clear();
		newTileRecords.clear();
		transitionController=null;
		
		if (mapState!=null) {
			mapStateUpdated(mapState, true);
		}
	}
	
	public MapLayer getContentView() {
		return (MapLayer) getParent();
	}
	
	public TransitionController getTransitionController() {
		if (transitionController==null) {
			transitionController=getContentView().getMapSurface().getTransitionController();
		}
		return transitionController;
	}
	
	@Override
	public void onTransitionComplete(Transition t) {
		// If we are transition locked, unlock it, do an update
		// and clear any unused tiles
		if (transitionLocked) {
			transitionLocked=false;
			currentTileSet.removeTemporary();
			mapStateUpdated(t.getActiveMapState(), true);
			transitionTileSet.clear();
		}
	}
	
	/**
	 * At the beginning of a transition, preloads tiles into transitionTileSet
	 * from the final MapState.
	 * @param mapState
	 */
	protected void loadPendingTiles(MapState mapState) {
		int right=getWidth()-1, bottom=getHeight()-1;

		// Select tile keys that intersect our display area
		updatedKeys.clear();
		newTileRecords.clear();
		selector.select(mapState.getProjection(),
				mapState.getResolution(),
				mapState.getViewportProjectedX(0, 0),
				mapState.getViewportProjectedY(0, 0),
				mapState.getViewportProjectedX(right, bottom),
				mapState.getViewportProjectedY(right, bottom),
				updatedKeys);
		
		for (int i=0; i<updatedKeys.size(); i++) {
			TileKey key=updatedKeys.get(i);
			TileSet.Record record=transitionTileSet.get(key);
			if (record==null) {
				record=transitionTileSet.create(key);
				newTileRecords.add(record);
				mapTileToDisplay(mapState, key, record.displayRect);				
			}
		}
		
		sortTileSetRecords(newTileRecords);
		for (int i=0; i<newTileRecords.size(); i++) {
			TileSet.Record record=newTileRecords.get(i);
			if (record.tile==null) record.tile=selector.resolve(record.key);
		}		
	}
	
	@Override
	public void mapStateUpdated(MapState mapState, boolean full) {
		this.mapState=mapState;
		
		TransitionController tc=getTransitionController();
		if (!transitionLocked && tc.isTransitionActive()) {
			// Prime pending tiles and lock transition
			Transition transition=tc.getActiveTransition();
			loadPendingTiles(transition.getFinalMapState());
			transition.addCallback(this);
			transitionLocked=true;
		}

		currentTileSet.resetMarks();
		int right=getWidth()-1, bottom=getHeight()-1;
		
		// Clear our shared state for a new run
		boolean generatePreviews=true;
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
				// If we're in transition, then go look in the pendingTileSet.
				if (transitionLocked) {
					record=transitionTileSet.get(key);
					if (record!=null) {
						transitionTileSet.move(record, currentTileSet);
					} else {
						// Just make one
						record=currentTileSet.create(key);
					}
				} else {
					// Just create the record
					record=currentTileSet.create(key);
				}
				newTileRecords.add(record);
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
		}
		
		// newTileRecords now contains all records that have been newly allocated.
		// We initialize them here in this ass-backwards way because we want to sort
		// them by proximity to the center but don't have the display information until
		// after we've iterated over all of them.  Think of this as the "initialize new
		// tiles" loop
		sortTileSetRecords(newTileRecords);
		for (int i=0; i<newTileRecords.size(); i++) {
			TileSet.Record record=newTileRecords.get(i);
			
			if (record.tile==null) {
				// Initialize the tile.  When in transitionLocked, only create
				// temporary tiles (we don't want to do extra IO loading stuff that's going away)
				if (transitionLocked) {
					record.tile=new Tile(record.key);
					record.tile.setTemporary(true);
				}
				else record.tile=selector.resolve(record.key);
			}
			
			// If there is no image, give it a chance to create a preview
			if (record.tile.getDrawable()==null) {
				record.tile.generatePreview(record.displayRect, oldTileSet);
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
		Rect clip=canvas.getClipBounds();
		
		for (TileSet.Record record: currentTileSet.records()) {
			if (Rect.intersects(clip, record.displayRect)) {
				//Log.d(Constants.LOG_TAG, "DRAW TILE: " + record.tile);
				Drawable drawable=record.tile.getDrawable();
				if (drawable!=null) {
					// Draw it
					drawable.setBounds(record.displayRect);
					drawable.draw(canvas);
				} else {
					// Clear the area
					//Log.d(Constants.LOG_TAG, "Clearing " + record.displayRect);
					canvas.drawRect(record.displayRect, CLEAR_PAINT);
				}
				
				if (DEBUG_BOUNDS) {
					Paint stroke=new Paint();
					stroke.setStyle(Style.STROKE);
					stroke.setColor(0xffff0000);
					canvas.drawRect(record.displayRect, stroke);
				}
			}
		}
	}

	@Override
	public void tileStateChanged(Tile tile) {
		//Log.d(Constants.LOG_TAG, "Tile state changed: " + tile.getState());
		
		// If it is still in the current set, invalidate its bounds
		TileSet.Record record=currentTileSet.get(tile.getKey());
		if (record!=null) {
			invalidate(record.displayRect);
		}
	}
	
}
