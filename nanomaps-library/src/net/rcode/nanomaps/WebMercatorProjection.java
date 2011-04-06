package net.rcode.nanomaps;

import net.rcode.nanomaps.util.DoubleBounds;

/**
 * Web mercator projection as used by Google, Microsoft, et al.
 * <p>
 * This class is translated almost verbatim from the nanomaps JavaScript
 * source.
 * 
 * @author stella
 *
 */
public class WebMercatorProjection implements Projection {
	private static DoubleBounds GLOBAL_EXTENT=new DoubleBounds(
			-180.0, -85.05112878, 180.0, 85.05112878
			);
	private static DoubleBounds PROJECTED_EXTENT;
	public static WebMercatorProjection DEFAULT=new WebMercatorProjection();
	
	static {
		PROJECTED_EXTENT=new DoubleBounds(
				DEFAULT.forwardX(GLOBAL_EXTENT.getMinx()),
				DEFAULT.forwardY(GLOBAL_EXTENT.getMiny()),
				DEFAULT.forwardX(GLOBAL_EXTENT.getMaxx()),
				DEFAULT.forwardY(GLOBAL_EXTENT.getMaxy())
				);
	}
	
	private static final double DEG_TO_RAD=0.0174532925199432958;
	private static final double EARTH_RADIUS=6378137.0;
	private static final double FOURTHPI=0.78539816339744833;
	private static final double HALFPI=1.5707963267948966;
	private static final double RAD_TO_DEG=57.29577951308232;
	private static final double HIGHEST_RES=78271.5170;
	private static final double LOG2=Math.log(2);
	
	private int minLevel;
	private int maxLevel;
	
	public WebMercatorProjection() {
		minLevel=1;
		maxLevel=18;
	}
	
	public WebMercatorProjection(int minLevel, int maxLevel) {
		this.minLevel=minLevel;
		this.maxLevel=maxLevel;
	}
	
	@Override
	public double forwardX(double globalX) {
		return globalX * DEG_TO_RAD * EARTH_RADIUS;
	}
	
	@Override
	public double forwardY(double globalY) {
		return Math.log(Math.tan(FOURTHPI + 0.5 * DEG_TO_RAD * globalY)) * EARTH_RADIUS;
	}

	@Override
	public double inverseX(double projectedX) {
		return RAD_TO_DEG * projectedX / EARTH_RADIUS;
	}
	
	@Override
	public double inverseY(double projectedY) {
		return RAD_TO_DEG * (HALFPI - 2.0 * Math.atan(Math.exp(-projectedY/EARTH_RADIUS)));
	}


	@Override
	public double fromLevel(double level) {
		return HIGHEST_RES/Math.pow(2, level-1);
	}

	@Override
	public double toLevel(double resolution) {
		return Math.log(HIGHEST_RES/resolution) / LOG2 + 1;
	}

	@Override
	public int getMaxLevel() {
		return maxLevel;
	}

	@Override
	public int getMinLevel() {
		return minLevel;
	}
	
	@Override
	public boolean isYAxisInverted() {
		return true;
	}
	
	@Override
	public boolean isXAxisInverted() {
		return false;
	}
	
	@Override
	public DoubleBounds getGlobalExtent() {
		return GLOBAL_EXTENT;
	}
	
	@Override
	public DoubleBounds getProjectedExtent() {
		return PROJECTED_EXTENT;
	}
}
