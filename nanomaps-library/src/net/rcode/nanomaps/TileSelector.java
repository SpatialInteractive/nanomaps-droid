package net.rcode.nanomaps;

import java.util.Collection;

import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * Selects meta-data about tiles to display for a displayable area
 * of a map.  The MapTileSelector.Key class is used to represent a tile
 * and can later be passed to resolveSource in order to get something
 * that can be loaded.
 * 
 * @author stella
 *
 */
public abstract class TileSelector {
	
	/**
	 * Selects all tiles at the closest native resolution to the
	 * given resolution starting at the origin
	 * (x/resolution, y/resolution) and proceeding by width and height
	 * pixels down and to the right.
	 * 
	 * @param projection
	 * @param resolution
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @param callback
	 */
	public abstract void select(Projection projection,
			double resolution,
			double x1,
			double y1,
			double x2,
			double y2,
			Collection<TileKey> destination);
	
	/**
	 * Resolve a TileKey to a Tile.
	 * Default implementation just calls resolveWithPreview(key, null, null).
	 * @param key
	 * @return Tile
	 */
	public abstract Tile resolve(TileKey key);
	
	/**
	 * Resolve a TileKey to a Tile, attempting to generate a preview if possible.
	 * The displayRect is taken as the area in the TileSet that this tile will occupy.
	 * The previewMaterial TileSet contains extra tiles that can be drawn from to
	 * generate previews.  The displayRect of each valid tile in previewMaterial
	 * will be set, so this tile can just do a quick intersects check to see if anything can
	 * contribute.
	 * 
	 * @param key
	 * @param displayRect
	 * @param previewMaterial
	 * @return Tile
	 */
	public Tile resolveWithPreview(TileKey key, Rect displayRect, TileSet previewMaterial) {
		Tile tile=resolve(key);
		if (tile.getState()!=Tile.STATE_LOADED && displayRect!=null && previewMaterial!=null) {
			Drawable preview=generatePreview(key, displayRect, previewMaterial);
			if (preview!=null) {
				tile.setPreview(preview);
			}
		}
		return tile;
	}
	
	/**
	 * Attempt to assemble the tile from current material if possible.
	 * This may take current tiles and build a preview.
	 * @param displayRect the space that this tile occupies in the previewMaterial TileSet
	 * @param previewMaterial existing tiles
	 */
	protected Drawable generatePreview(TileKey key, Rect displayRect, TileSet previewMaterial) {
		CompositeDrawable preview=null;
		int size=key.getSize();
		for (TileSet.Record record: previewMaterial.records()) {
			if (record.displayRect==null || record.tile==null) continue;
			if (!Rect.intersects(displayRect, record.displayRect)) continue;
			
			Drawable source=record.tile.getDrawable();
			if (source==null) continue;
			if (preview==null) preview=new CompositeDrawable(size, size);
			
			RectF childBounds=new RectF(record.displayRect);
			childBounds.offset(-displayRect.left, -displayRect.top);
			preview.addChild(childBounds, source);
		}
		
		return preview;
	}	
}
