package net.rcode.nanomaps;

/**
 * Bounding box based on the double data type
 * @author stella
 *
 */
public class Bounds {
	private double minx,miny,maxx,maxy;

	public Bounds(double minx, double miny, double maxx, double maxy) {
		this.minx = minx;
		this.miny = miny;
		this.maxx = maxx;
		this.maxy = maxy;
	}

	public double getMinx() {
		return minx;
	}

	public double getMiny() {
		return miny;
	}

	public double getMaxx() {
		return maxx;
	}

	public double getMaxy() {
		return maxy;
	}
	
	
}
