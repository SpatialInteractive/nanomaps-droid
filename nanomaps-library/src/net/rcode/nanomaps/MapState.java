package net.rcode.nanomaps;

import net.rcode.nanomaps.util.Constants;
import net.rcode.nanomaps.util.DoubleBounds;

/**
 * Represents physical state of the map.
 * <p>
 * There are several different coordinate systems accessible from this
 * object:
 * <ul>
 * <li>viewport: display coordinates offset by the origin (top left).  Range is
 * (0..width-1, 0..height-1) 
 * <li>display: pixel sized units at the current resolution on a virtual
 * canvas the size of the scaled and rotated global map.  If caching
 * coordinates for quick display updates, views should cache their
 * display coordinates as these will only need to be updated on resolution
 * or rotation changes, not on move.  If the projection has axis inversion,
 * then you should note that the display coordinates will have this fixed
 * relative to normal screen orientation
 * <li>projected: projected coordinates from the controlling map projection.
 * These are unscaled, so they do not change with resolution or rotation.  Note
 * that the y-axis is most likely inverted (based on projection.isYAxisInverted()).
 * <li>global: typically lat/lng but really is whatever is taken by the 
 * projection's forward functions.
 * </ul>
 * 
 * When accessing projected and global coordinates, the object will let you bias
 * your request relative to viewport coordinates.  This is most often what you want
 * because it let's you quickly resolve a point on the screen to its actual physical
 * coordinates and vica-versa.  Unless otherwise stated, any unadorned (x,y) coordinates
 * are viewport relative.
 * 
 * @author stella
 *
 */
public class MapState {
	private MapStateAware listener;
	
	private Projection projection;
	
	/**
	 * Current resolution
	 */
	private double resolution;
	
	/**
	 * The origin x coordinate in scaled projected units
	 */
	private double viewportOriginX;
	
	/**
	 * The origin y coordinate in scaled projected units
	 */
	private double viewportOriginY;
	
	/**
	 * Width of the viewport.  Automatically set by the MapSurface.
	 */
	private int viewportWidth;

	/**
	 * Height of the viewport.  Automatically set by the MapSurface.
	 */
	private int viewportHeight;
	
	/**
	 * Counter to indicate we are doing a batch operation
	 */
	private int lockCount;
	private boolean lockUpdated;
	private boolean lockUpdatedFull;
	
	public MapState(Projection projection) {
		this.projection=projection;
		this.resolution=projection.fromLevel(projection.getMinLevel());
		
		DoubleBounds extent=projection.getProjectedExtent();
		setViewportProjected((extent.getMinx()+extent.getMaxx())/2, 
				(extent.getMiny()+extent.getMaxy())/2,
				0, 0);
	}
	
	/**
	 * Copy constructor.  Does nothing.
	 */
	protected MapState() {
	}
	
	/**
	 * Copies the current mapstate into a new detached instance
	 * @return
	 */
	public MapState dup() {
		MapState ret=new MapState();
		ret.projection=projection;
		ret.resolution=resolution;
		ret.viewportHeight=viewportHeight;
		ret.viewportWidth=viewportWidth;
		ret.viewportOriginX=viewportOriginX;
		ret.viewportOriginY=viewportOriginY;
		return ret;
	}
	
	/**
	 * Copy all mutable parameters from this mapstate to the target.
	 * If anything changed, calls update.
	 * @param target
	 */
	public void copy(MapState target) {
		boolean changed=(
				target.viewportOriginX!=viewportOriginX ||
				target.viewportOriginY!=viewportOriginY ||
				target.resolution!=resolution ||
				target.viewportHeight!=viewportHeight ||
				target.viewportWidth!=viewportWidth
				);
		target.resolution=resolution;
		target.viewportHeight=viewportHeight;
		target.viewportWidth=viewportWidth;
		target.viewportOriginX=viewportOriginX;
		target.viewportOriginY=viewportOriginY;
		
		if (changed) {
			target._updated(true);
		}
	}
	
	private void _updated(boolean fullUpdate) {
		if (lockCount>0) {
			lockUpdated=true;
			if (fullUpdate) lockUpdatedFull=true;
			return;
		} else {
			if (listener!=null) listener.mapStateUpdated(this, fullUpdate);
		}
	}
	
	void setListener(MapStateAware listener) {
		this.listener=listener;
	}
	
	public void lock() {
		if (++lockCount == 1) {
			lockUpdated=false;
			lockUpdatedFull=false;
		}
	}
	public void unlock() {
		if (--lockCount==0) {
			if (lockUpdated) _updated(lockUpdatedFull);
		}
	}
	
	public Projection getProjection() {
		return projection;
	}
	public double getResolution() {
		return resolution;
	}
	
	/**
	 * Set the resolution, preserving the physical position under the given
	 * viewport (x,y) coordinates.
	 * @param resolution
	 * @param x
	 * @param y
	 */
	public void setResolution(double resolution, int x, int y) {
		if (resolution!=this.resolution) {
			lock();
			double projectedX=getViewportProjectedX(x, y);
			double projectedY=getViewportProjectedY(x, y);
			this.resolution=resolution;
			setViewportProjected(projectedX, projectedY, x, y);
			lockUpdated=true;
			lockUpdatedFull=true;
			unlock();
		}
	}
	
	/**
	 * Get the heading in radians that the map is oriented towards.  This is
	 * measured relative to the positive Y display axis, just like a compass.
	 * @return heading
	 */
	public double getHeading() {
		return 0;
	}
	
	/**
	 * Get the heading translated to degrees (convenience)
	 * @return heading in degrees
	 */
	public double getHeadingDegrees() {
		return getHeading() * Constants.RAD_TO_DEG;
	}
	
	public double getLevel() {
		return projection.toLevel(resolution);
	}
	

	public void setLevel(double level, int x, int y) {
		setResolution(projection.fromLevel(level), x, y);
	}
	
	/**
	 * @return The display X coordinate of the left edge
	 */
	public double getViewportOriginX() {
		return viewportOriginX;
	}
	/**
	 * @return The display Y coordinate of the top edge
	 */
	public double getViewportOriginY() {
		return viewportOriginY;
	}

	public void setViewportOrigin(double x, double y) {
		this.viewportOriginX=x;
		this.viewportOriginY=y;
		_updated(false);
	}
	
	public int getViewportWidth() {
		return viewportWidth;
	}
	public int getViewportHeight() {
		return viewportHeight;
	}
	
	public void setViewportSize(int w, int h) {
		this.viewportWidth=w;
		this.viewportHeight=h;
	}
	
	/**
	 * @param relativeX viewport X coordinate to reference against
	 * @return display X coordinate corresponding to viewport X coordinate
	 */
	public double getViewportDisplayX(int x, int y) {
		return viewportOriginX+x;
	}
	/**
	 * @param relativeY viewport Y coordinate to reference against
	 * @return display Y coordinate corresponding to viewport Y coordinate
	 */
	public double getViewportDisplayY(int x, int y) {
		return viewportOriginY+y;
	}
	
	public double getViewportProjectedX(int x, int y) {
		// TODO: Apply rotation here
		double displayX=getViewportDisplayX(x,y) * resolution;
		
		// Apply axis inversion
		if (projection.isXAxisInverted()) {
			displayX=projection.getProjectedExtent().getMaxx() - displayX;
		}
		
		return displayX;
	}
	
	public double getViewportProjectedY(int x, int y) {
		double displayY=getViewportDisplayY(x,y) * resolution;
		
		// Apply axis inversion
		if (projection.isYAxisInverted()) {
			// DisplayY will be the magnitude of projection units below maxy
			displayY=projection.getProjectedExtent().getMaxy() - displayY;
		}
		
		return displayY;
	}
	
	public double getViewportGlobalX(int x, int y) {
		return projection.inverseX(getViewportProjectedX(x,y));
	}
	
	public double getViewportGlobalY(int x, int y) {
		return projection.inverseY(getViewportProjectedY(x,y));
	}
	
	
	public double projectedToDisplayX(double projectedX) {
		if (projection.isXAxisInverted()) {
			projectedX=projection.getProjectedExtent().getMaxx() - projectedX;
		}

		return projectedX / resolution;
	}
	
	public double projectedToDisplayY(double projectedY) {
		if (projection.isYAxisInverted()) {
			projectedY=projection.getProjectedExtent().getMaxy() - projectedY;
		}

		// TODO: Apply rotation
		return projectedY / resolution;
	}
	
	/**
	 * Set the viewport origin relative to itself
	 * @param deltaX
	 * @param deltaY
	 */
	public void moveViewport(double deltaX, double deltaY) {
		viewportOriginX+=deltaX;
		viewportOriginY+=deltaY;
		_updated(false);
	}
	
	/**
	 * Set the origin of the viewport in display units.
	 * @param displayX
	 * @param displayY
	 * @param x
	 * @param y
	 */
	public void setViewportDisplay(double displayX, double displayY, int x, int y) {
		displayX=displayX - x;
		displayY=displayY - y;
		
		if (displayX!=viewportOriginX || displayY!=viewportOriginY) {
			viewportOriginX=displayX;
			viewportOriginY=displayY;
			_updated(false);
		}
	}
	
	/**
	 * Set the origin of the viewport in projected units.
	 * If (x,y) is non zero, then this indicates that the given projected coordinates
	 * should be under these viewport coordinates.
	 * @param projectedX
	 * @param projectedY
	 * @param x
	 * @param y
	 */
	public void setViewportProjected(double projectedX, double projectedY, int x, int y) {
		setViewportDisplay(projectedToDisplayX(projectedX), projectedToDisplayY(projectedY), x, y);
	}
	
	
	/**
	 * Set the origin of the viewport in projected units.
	 * If (x,y) is non zero, then this indicates that the given projected coordinates
	 * should be under these viewport coordinates.
	 * @param globalX
	 * @param globalY
	 * @param x
	 * @param y
	 */
	public void setViewportGlobal(double globalX, double globalY, int x, int y) {
		setViewportProjected(projection.forwardX(globalX), projection.forwardY(globalY), x, y);
	}
	
	
	@Override
	public String toString() {
		return String.format("MapState(res=%s,x=%s,y=%s)", resolution, viewportOriginX, viewportOriginY);
	}
}
