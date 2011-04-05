package net.rcode.nanomaps;

import java.util.Collection;

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
	 * Resolve a TileKey to a Tile.  Callers should pass the TileSet
	 * that was most recently rendered.  In this case, the method can
	 * choose to recycle or import from any of the tiles in the TileSet.
	 * destroy() should be called on the returned Tile when it is no
	 * longer needed.
	 * @param key
	 * @param current
	 * @return Tile
	 */
	public abstract Tile resolve(TileKey key, TileSet current);
}
