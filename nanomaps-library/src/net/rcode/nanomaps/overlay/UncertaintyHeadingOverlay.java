package net.rcode.nanomaps.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * Extends CircleOverlay to draw an uncertainty circle plus a heading cone.
 * Either the uncertainty circle or the heading cone can be made invisible
 * independently.
 * 
 * @author stella
 *
 */
public class UncertaintyHeadingOverlay extends CircleOverlay {
	protected Paint headingConePaint;
	protected int headingConeMinRadius;
	protected int headingConeOvershoot;
	protected float heading;
	private float headingUncertainty;
	
	public UncertaintyHeadingOverlay(Context context) {
		super(context);
		headingConePaint=new Paint();
		headingConePaint.setColor(0x70ffffff);
		headingConePaint.setAntiAlias(true);
		heading=Float.NaN;
	}

	/**
	 * @return the Paint used to draw the heading cone
	 */
	public Paint getHeadingConePaint() {
		return headingConePaint;
	}
	
	/**
	 * Minimum radius of the heading cone.  If the enclosing circle overlay
	 * is >= this size, then the heading cone will be drawn to match the circle.
	 * Otherwise, the cone will extrude past the circle to this radius.
	 * @return min radius
	 */
	public int getHeadingConeMinRadius() {
		return headingConeMinRadius;
	}
	
	/**
	 * Change the minimum heading cone radius
	 * @param headingConeMinRadius
	 */
	public void setHeadingConeMinRadius(int headingConeMinRadius) {
		this.headingConeMinRadius = headingConeMinRadius;
		updateMetrics();
		invalidate();
	}

	/**
	 * @return the number of pixels to extend past the radius (or min radius)
	 */
	public int getHeadingConeOvershoot() {
		return headingConeOvershoot;
	}
	
	/**
	 * Update overshoot
	 * @param headingConeOvershoot
	 */
	public void setHeadingConeOvershoot(int headingConeOvershoot) {
		this.headingConeOvershoot = headingConeOvershoot;
		updateMetrics();
		invalidate();
	}
	
	/**
	 * @return The current heading in degrees.  If NaN, then no cone is drawn.
	 */
	public float getHeading() {
		return heading;
	}
	
	/**
	 * @return The current heading arc in degrees.
	 */
	public float getHeadingUncertainty() {
		return headingUncertainty;
	}
	
	/**
	 * Change the heading and headingArc
	 * @param heading
	 */
	public void setHeading(float heading, float headingUncertainty) {
		this.heading = heading;
		this.headingUncertainty = headingUncertainty;
		updateMetrics();
		invalidate();
	}
	
	@Override
	protected int calculateSize() {
		int size=super.calculateSize();
		
		if (!Float.isNaN(heading) && radiusPixels < headingConeMinRadius) {
			// The circle is to small.  Override the radius.
			size=headingConeMinRadius*2 + FUDGE_PADDING;
		}
		
		size+=headingConeOvershoot*2;
		
		return size;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (mapState==null || Float.isNaN(heading)) return;
		
		float startAngle=heading - (float)mapState.getHeading() - headingUncertainty - 90;
		float sweepAngle=2 * headingUncertainty;
		
		int radius=radiusPixels;
		if (radius<headingConeMinRadius) radius=headingConeMinRadius;
		int cx=getWidth()/2, cy=getHeight()/2;
		
		int effectiveRadius=radius + headingConeOvershoot;
		
		RectF oval=new RectF();
		oval.left=cx-effectiveRadius;
		oval.top=cy-effectiveRadius;
		oval.right=cx+effectiveRadius;
		oval.bottom=cy+effectiveRadius;
		
		canvas.drawArc(oval, startAngle, 
				sweepAngle, 
				true, headingConePaint);
	}
}
