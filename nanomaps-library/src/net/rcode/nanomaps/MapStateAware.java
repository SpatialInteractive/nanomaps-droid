package net.rcode.nanomaps;

/**
 * Objects that are aware of the MapState implement this interface
 * to be notified of changes.
 * @author stella
 *
 */
public interface MapStateAware {
	/**
	 * Called when the mapstate has changed.  If full is false,
	 * then it can be assumed that this update is purely related
	 * to the origin is changing.  Otherwise, assume that the
	 * full geometry has changed.
	 * 
	 * @param full
	 */
	public void mapStateUpdated(MapState mapState, boolean full);
}
