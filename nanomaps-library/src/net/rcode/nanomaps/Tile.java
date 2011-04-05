package net.rcode.nanomaps;

import android.graphics.drawable.Drawable;

/**
 * Base class for representing a Tile.  Ultimately a tile should resolve
 * to a Drawable, but this class supports tiles that have not yet loaded
 * or that may load multiple times (ie. an initial preview followed by
 * the full view).
 * 
 * @author stella
 *
 */
public class Tile {
	public static interface StateChangedListener {
		public void tileStateChanged(Tile tile);
	}
	
	public static final int STATE_NONE=0;
	public static final int STATE_PREVIEW=1;
	public static final int STATE_LOADED=2;
	public static final int STATE_ERROR=3;
	
	private TileKey key;
	private int state;
	private Drawable drawable;
	private StateChangedListener stateChangedListener;
	
	public Tile(TileKey key) {
		this.key=key;
	}
	
	/**
	 * @return the tile's key
	 */
	public final TileKey getKey() {
		return key;
	}
	
	/**
	 * @return The current drawable or null
	 */
	public Drawable getDrawable() {
		return drawable;
	}
	
	public void setPreview(Drawable drawable) {
		if (state!=STATE_LOADED) {
			updateContent(drawable, STATE_PREVIEW);
		}
	}
	
	/**
	 * Update the tile's content with the given drawable and state
	 * @param drawable
	 * @param state
	 */
	public void updateContent(Drawable drawable, int state) {
		//TEMP - uncomment to preserve preview images for debugging
		//if (this.drawable!=null) return;
		
		this.drawable=drawable;
		this.state=state;
		if (stateChangedListener!=null)
			stateChangedListener.tileStateChanged(this);
	}
	
	/**
	 * <ul>
	 * <li>STATE_NONE: No valid Drawable
	 * <li>STATE_PREVIEW: A valid Drawable is available but should be considered a preview
	 * <li>STATE_LOADED: A valid Drawable is available and should be considered final
	 * </ul>
	 * 
	 * @return The current state
	 */
	public int getState() {
		return state;
	}
	
	/**
	 * Set a listener to be notified of state/content changes
	 * @param stateChangedListener
	 */
	public void setStateChangedListener(
			StateChangedListener stateChangedListener) {
		this.stateChangedListener = stateChangedListener;
	}
	
	/**
	 * Called when the Tile is no longer needed.  No further methods will be called
	 * on tile by the requestor after this call.
	 */
	public void destroy() {
		this.stateChangedListener=null;
		this.drawable=null;
	}
	
	@Override
	public String toString() {
		return key.toString();
	}
}
