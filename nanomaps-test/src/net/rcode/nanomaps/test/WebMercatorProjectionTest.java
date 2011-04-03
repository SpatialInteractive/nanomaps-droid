package net.rcode.nanomaps.test;

import junit.framework.TestCase;

import net.rcode.nanomaps.WebMercatorProjection;

public class WebMercatorProjectionTest extends TestCase {
	private static final double DELTA=0.000001;
	private WebMercatorProjection prj=WebMercatorProjection.DEFAULT;
	
	public void testForward() {
		double x=prj.forwardX(57.29577951308232286465);
		assertEquals(6378137.00000000000000000000, x, DELTA);
		double y=prj.forwardY(57.29577951308232286465);
		assertEquals(7820815.27608548197895288467, y, DELTA);
	}
	
	public void testInverse() {
		double x=prj.inverseX(6378137.00000000000000000000);
		assertEquals(57.29577951308231575922, x, DELTA);
		double y=prj.inverseY(7820815.27608548197895288467);
		assertEquals(57.29577951308231575922, y, DELTA);
	}
}
