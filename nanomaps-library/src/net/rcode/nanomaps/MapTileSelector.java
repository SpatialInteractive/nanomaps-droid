package net.rcode.nanomaps;

/**
 * Selects meta-data about tiles to display for a displayable area
 * of a map.  The MapTileSelector.Key class is used to represent a tile
 * and can later be passed to resolveSource in order to get something
 * that can be loaded.
 * 
 * @author stella
 *
 */
public abstract class MapTileSelector {
	
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
			Handler callback);
	
	/**
	 * Callback handler used when enumerating tiles
	 * @author stella
	 *
	 */
	public interface Handler {
		public void handleTile(TileKey key);
	}
	
	/**
	 * Represents a tile.  This class correctly implements hashCode()
	 * and equals() so it can be used as a hash key.
	 * @author stella
	 *
	 */
	public abstract static class TileKey {
		/**
		 * Almost all math regarding tiles needs access to its native
		 * resolution.  To avoid recalculating, the resolution corresponding
		 * to level is stored here.  It should not be used in key comparison
		 * because it is a floating point value.
		 */
		public final double resolution;
		
		/**
		 * X coordinate of the upper left corner in scaled projected
		 * coordinates
		 */
		public final double scaledX;
		
		/**
		 * Y coordinates of the upper left corner in scaled projected
		 * coordinates
		 */
		public final double scaledY;
		
		/**
		 * Size in pixels of the tile (assumes square tiles)
		 */
		public final int size;
		
		public TileKey(double resolution, double scaledX, double scaledY, int size) {
			this.resolution=resolution;
			this.scaledX=scaledX;
			this.scaledY=scaledY;
			this.size=size;
		}
	}
	
	
}
