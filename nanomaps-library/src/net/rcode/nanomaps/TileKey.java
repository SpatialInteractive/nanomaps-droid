package net.rcode.nanomaps;

/**
 * Represents a tile.  Derived classes will correctly implement
 * hashCode() and equals() but for performance do not implement the
 * full general contract.  equals can only be used to compare to
 * non-null values.
 * <p>
 * The methods on this interface represent the minimum information
 * necessary for a MapTileView to determine positioning on the display.
 * 
 * @author stella
 *
 */
public interface TileKey {
	/**
	 * Almost all math regarding tiles needs access to its native
	 * resolution.  To avoid recalculating, the resolution corresponding
	 * to level is stored here.  It should not be used in key comparison
	 * because it is a floating point value.
	 */
	public double getResolution();

	/**
	 * X coordinate of the upper left corner in scaled projected
	 * coordinates
	 */
	public double getScaledX();

	/**
	 * Y coordinates of the upper left corner in scaled projected
	 * coordinates
	 */
	public double getScaledY();

	/**
	 * Size in pixels of the tile (assumes square tiles)
	 */
	public int getSize();
}