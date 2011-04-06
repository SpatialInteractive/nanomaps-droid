package net.rcode.nanomaps.tile;

import net.rcode.nanomaps.util.CompositeDrawable;
import android.graphics.Rect;
import android.graphics.RectF;
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
	private boolean temporary;
	
	public Tile(TileKey key) {
		this.key=key;
	}
	
	/**
	 * @return the tile's key
	 */
	public final TileKey getKey() {
		return key;
	}
	
	public void setTemporary(boolean temporary) {
		this.temporary = temporary;
	}
	public boolean isTemporary() {
		return temporary;
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
	
	public void generatePreview(Rect displayRect, TileSet previewMaterial) {
		if (drawable==null) {
			Drawable p=generatePreview(key, displayRect, previewMaterial);
			if (p!=null) updateContent(p, STATE_PREVIEW);
		}
	}
	
	/**
	 * Attempt to assemble the tile from current material if possible.
	 * This may take current tiles and build a preview.
	 * @param displayRect the space that this tile occupies in the previewMaterial TileSet
	 * @param previewMaterial existing tiles
	 */
	protected static Drawable generatePreview(TileKey key, Rect displayRect, TileSet previewMaterial) {
		CompositeDrawable preview=null;
		
		int nativeSize=key.getSize();
		
		// The display area may be displaying a non-native scale
		// but we need to populate our preview with native scaled data
		float sx=(float)nativeSize / displayRect.width();
		float sy=(float)nativeSize / displayRect.height();
		
		for (TileSet.Record record: previewMaterial.records()) {
			if (record.displayRect==null || record.tile==null) continue;
			if (!Rect.intersects(displayRect, record.displayRect)) continue;
			
			Drawable source=record.tile.getDrawable();
			if (source==null) continue;
			if (preview==null) preview=new CompositeDrawable(nativeSize, nativeSize);
			
			RectF childBounds=new RectF(record.displayRect);
			childBounds.offset(-displayRect.left, -displayRect.top);
			
			// Scale the child back to our native size
			childBounds.left*=sx;
			childBounds.right*=sx;
			childBounds.top*=sy;
			childBounds.bottom*=sy;
			
			preview.addChild(childBounds, source);
		}
		
		return preview;
	}	
}
