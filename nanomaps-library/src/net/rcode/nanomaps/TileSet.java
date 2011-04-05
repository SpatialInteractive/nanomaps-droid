package net.rcode.nanomaps;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.graphics.Rect;
import android.util.Log;

/**
 * Represents a set of active tiles identified by TileKey instances.
 * Typically, the MapTileView will maintain a TileSet of the tiles
 * which are active on the screen.  On each full redraw, it will
 * recycle any tiles that were previously on the screen and discard/cancel
 * any of those that are no longer.  To implement this, the TileSet
 * has resetMarks() and sweep() methods.  See those for the contract.
 * 
 * @author stella
 *
 */
public class TileSet {
	public static class Record {
		public TileKey key;
		public Tile tile;
		public Rect displayRect;
		public boolean marked;
	}
	
	/**
	 * Typical active sizes will be about 12 tiles.  While redrawing the
	 * screen or working at intermediate scales, this can double or
	 * quadruple.
	 */
	private Map<TileKey, Record> contents=new HashMap<TileKey, Record>(97);
	
	public boolean isEmpty() {
		return contents.isEmpty();
	}
	
	/**
	 * @param tk
	 * @return tile with key or null
	 */
	public final Record get(TileKey tk) {
		return contents.get(tk);
	}
	
	/**
	 * Puts a tile by key.  If there is already a tile with the key and
	 * it is different than this one, then it is removed and its destroy()
	 * method is called.  The way this method works is a little weird because
	 * I was trying to make sure it had the right purvue to destroy unneeded tiles.
	 * @param tk
	 * @return current Record
	 */
	public final Record create(TileKey tk) {
		Record current=new Record();
		current.key=tk;
		Record prev=contents.put(tk, current);
		if (prev!=null && prev.tile!=null) {
			prev.tile.destroy();
		}
		return current;
	}
	
	/**
	 * Returns a live collection of keys.  This should not be modified (not enforced).
	 * @return all current keys
	 */
	public final Collection<TileKey> keys() {
		return contents.keySet();
	}
	
	/**
	 * @return Live collection of records.  This should not be modified (not enforced).
	 */
	public final Collection<Record> records() {
		return contents.values();
	}
	
	/**
	 * Reset all marks on tiles to false
	 */
	public final void resetMarks() {
		for (Record record: contents.values()) {
			record.marked=false;
		}
	}
	
	/**
	 * Remove and destroy all unmarked tiles
	 */
	public final void sweep() {
		int keepCount=0, removeCount=0;
		
		Iterator<Record> iter=contents.values().iterator();
		while (iter.hasNext()) {
			Record record=iter.next();
			if (!record.marked) {
				iter.remove();
				if (record.tile!=null) record.tile.destroy();
				removeCount++;
			} else {
				keepCount++;
			}
		}
		
		Log.d(Constants.LOG_TAG, "Sweep TileSet.  Keep=" + keepCount + ", Remove=" + removeCount);
	}
	
	/**
	 * Clears and destroys all tiles
	 */
	public final void clear() {
		for (Record record: contents.values()) {
			if (record.tile!=null) record.tile.destroy();
		}
		contents.clear();
	}
}
