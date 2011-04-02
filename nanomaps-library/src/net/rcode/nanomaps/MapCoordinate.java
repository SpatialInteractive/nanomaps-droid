package net.rcode.nanomaps;

/**
 * Represents a map coordinate as (x,y).  When used in a
 * context for lat/lngs, lat=y and lng=x.  Since this is
 * such a common thing, convenience methods of getLatitude()
 * and getLongitude() are provided but obviously, they only
 * have significance if the coordinates being represented
 * are in fact latitude/longitudes.
 * <p>
 * Since map math is inherently floating point based, coordinates
 * are stored as two doubles.  Performance critical code operating
 * purely in pixel coordinates may wish to manage its coordinates
 * with integers internally.  All access is done via getters/setters
 * however, so an additional class could be introduced later to abstract
 * out a fixed integer storage model.  My instincts tell me, though, that
 * the cost of conversions and the extra cost of indirect calls will
 * negate any performance savings
 * 
 * @author stella
 *
 */
public class MapCoordinate {
	private double x;
	private double y;
	
	public MapCoordinate() {
	}
	public MapCoordinate(double x, double y) {
		this.x=x;
		this.y=y;
	}
	
	public final MapCoordinate copy() {
		MapCoordinate ret=new MapCoordinate();
		ret.x=x;
		ret.y=y;
		return ret;
	}
	
	public final double getX() {
		return x;
	}
	public final double getY() {
		return y;
	}
	public final void setX(double x) {
		this.x = x;
	}
	public final void setY(double y) {
		this.y = y;
	}
	
	public final double getLatitude() {
		return y;
	}
	public final double getLongitude() {
		return x;
	}
	public final void setLatitude(double lat) {
		y=lat;
	}
	public final void setLongitude(double lng) {
		x=lng;
	}
	
	public static MapCoordinate fromLatLng(double lat, double lng) {
		MapCoordinate ret=new MapCoordinate();
		ret.x=lng;
		ret.y=lat;
		return ret;
	}
}
