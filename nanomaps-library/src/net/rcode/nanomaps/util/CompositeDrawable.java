package net.rcode.nanomaps.util;

import java.util.ArrayList;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * A Drawable that consists of a set of children each with their own
 * bounds in this coordinate system.  The parent Drawable still maintains
 * its own width and height so that it can properly clip and scale
 * its children.
 * 
 * @author stella
 *
 */
public class CompositeDrawable extends Drawable {
	private static final Paint CLEAR_PAINT=new Paint();
	static {
		CLEAR_PAINT.setColor(0);
	}
	
	protected int alpha=255;
	protected ColorFilter cf;
	
	private int logicalWidth;
	private int logicalHeight;
	protected ArrayList<Entry> children=new ArrayList<Entry>(5);
	
	protected static class Entry {
		public RectF bounds;
		public Drawable drawable;
	}
	
	public CompositeDrawable(int logicalWidth, int logicalHeight) {
		this.logicalWidth=logicalWidth;
		this.logicalHeight=logicalHeight;
	}
	
	@Override
	public void draw(Canvas canvas) {
		Rect requestedRect=getBounds();
		float sx=(float)requestedRect.width() / logicalWidth;
		float sy=(float)requestedRect.height() / logicalHeight;
		
		// Clear the area
		canvas.drawRect(requestedRect, CLEAR_PAINT);
		
		// Setup clip
		canvas.save();
		if (!canvas.clipRect(requestedRect)) {
			// Nothing to draw
			canvas.restore();
			return;
		}
		
		// Setup transform to local coordinates
		canvas.translate(requestedRect.left, requestedRect.top);
		canvas.scale(sx, sy);
		
		// Draw children
		for (int i=0; i<children.size(); i++) {
			Entry entry=children.get(i);
			RectF childRect=entry.bounds;
			Drawable childDrawable=entry.drawable;
			childDrawable.setBounds((int)childRect.left, (int)childRect.top, (int)childRect.right, (int)childRect.bottom);
			childDrawable.setAlpha(alpha);
			childDrawable.setColorFilter(cf);
			childDrawable.draw(canvas);
		}
		
		canvas.restore();
	}
	
	public void addChild(RectF bounds, Drawable child) {
		if (child instanceof CompositeDrawable) {
			addNestedChildren(bounds, (CompositeDrawable)child);
		} else {
			// Just add the child directly
			Entry entry=new Entry();
			entry.bounds=bounds;
			entry.drawable=child;
			children.add(entry);
		}
	}
	
	protected void addNestedChildren(RectF parentBounds, CompositeDrawable child) {
		float sx=parentBounds.width() / logicalWidth;
		float sy=parentBounds.height() / logicalHeight;
		
		for (int i=0; i<child.children.size(); i++) {
			Entry entry=child.children.get(i);
			RectF translatedBounds=new RectF(entry.bounds);
			
			// Rescale child bounds
			translatedBounds.left*=sx;
			translatedBounds.right*=sx;
			translatedBounds.top*=sy;
			translatedBounds.bottom*=sy;
			
			// And offset by the requested parent position
			translatedBounds.offset(parentBounds.left, parentBounds.top);
			
			Entry newEntry=new Entry();
			newEntry.bounds=translatedBounds;
			newEntry.drawable=entry.drawable;
			children.add(newEntry);
		}
	}

	@Override
	public int getOpacity() {
		// We fill with transparent pixels, so must be considered translucent
		return PixelFormat.TRANSLUCENT;
	}

	@Override
	public void setAlpha(int alpha) {
		this.alpha=alpha;
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		this.cf=cf;
	}

}
