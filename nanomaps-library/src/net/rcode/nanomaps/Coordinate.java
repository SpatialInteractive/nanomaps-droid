package net.rcode.nanomaps;

/**
 * Abstracts out a single point in a global coordinate system.
 * This may be neurotic, but my brain is wired to think of (latitude,longitude)
 * and when its in that mode, I can't get it in my head that x=longitude and
 * y=latitude (in fact, just after writing this sentence I had to read it 3 times
 * because that didn't quite look right).  
 * As a further neurosis, I spent my productive map-engaged years abbreviating them (lat,lng).
 * <p>
 * So the presence of this class is a tip of the hat to the fact that no one
 * can cure the brain and quirky habits are better indulged in old age than
 * challenged.  Everyone else can just go on using (x,y) and remembering
 * the difference.  I name things explicitly here so as not to get confused.  Surely
 * a lifetime of abuse is worth a few bytes in a class file.
 * <p>
 * The entire rest of the library deals in (x,y).  I've tried to keep any concept
 * of latitude and longitude here in the form of helper methods.  The "public" api
 * should pass around instances of this class.  The bits under the covers, though
 * will often just passes x's and y's nakedly (ie. see MapState).
 * <p>
 * This class is also structured to allow some future refactoring to support double
 * or integral E6 coordinates.  This could save some space with lots instances (maybe,
 * depending on alignment) but my intuition tells me it is probably not a speed savings
 * since we've got to go back to double anyway for projection math.  In my experience
 * this is usually a premature optimization, but I leave the option open by way of a subclass.
 * <p>
 * Ok, that's enough of a treatise on a simple Coordinate class.
 * 
 * @author stella
 *
 */
public class Coordinate {
	private double x;
	private double y;
	
	private Coordinate(double x, double y) {
		this.x=x;
		this.y=y;
	}
	
	public static Coordinate xy(double x, double y) {
		return new Coordinate(x, y);
	}
	
	public static Coordinate latLng(double lat, double lng) {
		Coordinate ret=new Coordinate(lng, lat);
		return ret;
	}
	
	public final double getX() {
		return x;
	}
	
	public final double getY() {
		return y;
	}
	
	public final double getLat() {
		return y;
	}
	
	public final double getLng() {
		return x;
	}
	
	public final void setX(double x) {
		this.x = x;
	}
	
	public final void setY(double y) {
		this.y = y;
	}
	
	public final void setLat(double lat) {
		this.y = lat;
	}
	
	public final void setLng(double lng) {
		this.x = lng;
	}
	
	@Override
	public String toString() {
		return String.format("Coordinate(x=%s,y=%s)", x, y);
	}
}
