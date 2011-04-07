package net.rcode.nanomaps.overlay;

import net.rcode.nanomaps.MapConstants;
import net.rcode.nanomaps.MapLayer;
import net.rcode.nanomaps.MapState;
import net.rcode.nanomaps.MapStateAware;
import net.rcode.nanomaps.util.Constants;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

/**
 * Display a circle whose radius is represented in projected
 * map units (typically meters).  This is not strictly cartographically
 * correct but for a conformal projection, it will be fairly accurate
 * over small areas.  It is often visually better to take the (small)
 * error rather than distort the shape into an ellipse.
 * 
 * @author stella
 *
 */
public class CircleOverlay extends View implements MapStateAware {
	/**
	 * Add fudged padding to the width and height.  Should be an odd number.
	 * Makes sure to capture an anti-alias effects, etc.
	 */
	protected static final int FUDGE_PADDING=5;
	
	protected MapState mapState;
	protected float physicalRadius;
	protected Paint backgroundPaint;
	protected Paint ringPaint;
	
	// -- metrics
	protected int size;
	protected int radiusPixels=-1;
	
	public CircleOverlay(Context context) {
		super(context);
		backgroundPaint=new Paint();
		backgroundPaint.setColor(0x700000ff);
		backgroundPaint.setAntiAlias(true);
		
		ringPaint=new Paint();
		ringPaint.setColor(0xbb0000ff);
		ringPaint.setStrokeWidth(2);
		ringPaint.setStyle(Style.STROKE);
		ringPaint.setAntiAlias(true);
	}

	@Override
	public void setLayoutParams(LayoutParams params) {
		// We rely on fixed values for offsetX and offsetY
		MapLayer.LayoutParams mlp=(net.rcode.nanomaps.MapLayer.LayoutParams) params;
		mlp.offsetX|=MapConstants.BIAS_CENTER;
		mlp.offsetY|=MapConstants.BIAS_CENTER;
		
		super.setLayoutParams(params);
	}
	
	@Override
	public void mapStateUpdated(MapState mapState, boolean full) {
		this.mapState=mapState;
		if (!full && radiusPixels!=-1) return;
		updateMetrics();
	}
	
	/**
	 * Get the paint used for the background.  If changing it, you should
	 * invalidate the view
	 * @return paint
	 */
	public Paint getBackgroundPaint() {
		return backgroundPaint;
	}
	
	/**
	 * @return the radius in projected units
	 */
	public float getRadius() {
		return physicalRadius;
	}
	
	/**
	 * Update the radius
	 * @param radius
	 */
	public void setRadius(float radius) {
		this.physicalRadius = radius;
		updateMetrics();
		invalidate();
	}
	
	/**
	 * Called when a vital parameter has changed
	 */
	protected void updateMetrics() {
		if (mapState==null) return;
		int left=getLeft(), top=getTop();
		size=calculateSize();
		
		if (Constants.DEBUG) Log.d(Constants.LOG_TAG, "CircleOverlayView.updateMetrics: size=" + size);
		
		// Don't worry about left and top.  The MapLayer is going to move us
		// anyway as a result of a mapState invalidation
		setMeasuredDimension(size, size);
		layout(left, top, left+size, top+size);
		
		// There is always a full invalidate after a significant
		// mapState change so don't retrigger
	}
	
	/**
	 * Calculate the needed size of the display area.  The box defined by size must have
	 * the anchor point at the center.
	 * <p>
	 * This method should also stash any other state it needs to draw.  It will only ever be
	 * called if mapState!=null
	 * 
	 * @return size of the drawable area
	 */
	protected int calculateSize() {
		radiusPixels=(int) Math.round(physicalRadius / mapState.getResolution());
		return radiusPixels*2 + FUDGE_PADDING;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		if (Constants.DEBUG) Log.d(Constants.LOG_TAG, "CircleOverlayView.onDraw: radius=" + radiusPixels);
		if (radiusPixels==0) return;
		
		float cx=getWidth()/2, cy=getHeight()/2;
		canvas.drawCircle(cx, cy, radiusPixels, backgroundPaint);
		if (ringPaint.getStrokeWidth()>=0) 
			canvas.drawCircle(cx, cy, radiusPixels-ringPaint.getStrokeWidth(), ringPaint);
	}
}
