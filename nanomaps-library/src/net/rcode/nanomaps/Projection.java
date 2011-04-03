package net.rcode.nanomaps;

/**
 * Define a map projection.
 * <p>
 * The projection for a map goes a little bit beyond the traditional
 * definition.  It provides a forward and inverse method for transforming
 * between global (lat/lng) and unscaled map units.  Unscaled map units
 * are related to pixel coordinates by applying a resolution factor.
 * <p>
 * The projection also maintains the transformation logic between indexed
 * levels and corresponding resolution factors.
 * 
 * @author stella
 *
 */
public interface Projection {
	/**
	 * @return Minimum supported level (to deliver highest scale maps)
	 */
	public int getMinLevel();
	
	/**
	 * @return Maximum supported level (to deliver lowest scale maps)
	 */
	public int getMaxLevel();
	
	/**
	 * Convert from a level to a resolution
	 * @param level
	 * @return resolution
	 */
	public double fromLevel(double level);
	
	/**
	 * Convert from a resolution to the corresponding level
	 * @param resolution
	 * @return level
	 */
	public double toLevel(double resolution);
	
	/**
	 * Convert from global coordinates to projected units
	 * @param global
	 */
	public double forwardX(double globalX);
	
	/**
	 * Convert from global coordinates to projected units
	 * @param global
	 */
	public double forwardY(double globalY);
	
	/**
	 * Convert from projected units to global coordinates
	 * @param projected
	 */
	public double inverseX(double projectedX);

	/**
	 * Convert from projected units to global coordinates
	 * @param projected
	 */
	public double inverseY(double projectedY);
	
	/**
	 * @return true if the x axis is inverted
	 */
	public boolean isXAxisInverted();
	
	/**
	 * @return true if the y axis is inverted
	 */
	public boolean isYAxisInverted();
	
	/**
	 * @return the extent of the projected units
	 */
	public Bounds getProjectedExtent();
	
	/**
	 * @return the extent in global units
	 */
	public Bounds getGlobalExtent();
}
