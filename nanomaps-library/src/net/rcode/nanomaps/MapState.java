package net.rcode.nanomaps;

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
	private MapSurface surface;
	
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
	 * Counter to indicate we are doing a batch operation
	 */
	private int batch;
	
	public MapState(Projection projection) {
		this.projection=projection;
		this.resolution=projection.fromLevel(projection.getMinLevel());
		
		DoubleBoundingBox extent=projection.getProjectedExtent();
		setViewportProjected((extent.getMinx()+extent.getMaxx())/2, 
				(extent.getMiny()+extent.getMaxy())/2,
				0, 0);
	}
	
	private void _updated(boolean justOrigin) {
		if (batch>0) return;
	}
	
	void setSurface(MapSurface surface) {
		this.surface=surface;
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
		batch++;
		double projectedX=getViewportProjectedX(x, y);
		double projectedY=getViewportProjectedY(x, y);
		this.resolution=resolution;
		setViewportProjected(projectedX, projectedY, x, y);
		batch--;
		
		_updated(false);
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
	
	public double getViewportLatitude(int x, int y) {
		return getViewportGlobalY(x, y);
	}
	
	public double getViewportLongitude(int x, int y) {
		return getViewportGlobalX(x,y);
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
	public void moveOrigin(int deltaX, int deltaY) {
		viewportOriginX+=deltaX;
		viewportOriginY+=deltaY;
		_updated(true);
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
		projectedX=projectedToDisplayX(projectedX);
		projectedY=projectedToDisplayY(projectedY);
		viewportOriginX=projectedX - x;
		viewportOriginY=projectedY - y;
		_updated(true);
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
	
	/**
	 * Convenience to set the viewport in lat/lng coordinates.  Equivilent to
	 * setViewportGlobal(lng, lat, x, y);
	 * @param lat
	 * @param lng
	 * @param x
	 * @param y
	 */
	public void setViewportLatLng(double lat, double lng, int x, int y) {
		setViewportGlobal(lng, lat, x, y);
	}
}
