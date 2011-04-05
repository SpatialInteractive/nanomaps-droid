package net.rcode.nanomaps;

import java.util.Collection;

/**
 * Base class for tile selectors that represent tiles in a cartesian
 * grid.  This produces CartesianTileKey instances.
 * <p>
 * The base class implements a select() method that presumes that tiles
 * are numbered from the upper left corner of the projected bounds and
 * exist at fixed native levels corresponding to the integers between
 * the projection's minimum and maximum levels (inclusive).
 * <p>
 * For exotic cases, sub-classes should just wholesale override the select()
 * method.  For variations that should be configurable, we will want to add
 * virtual methods that the base class select() calls to configure itself.
 * As I don't have example of variations right now, I have not added any
 * abstraction to keep things simple.
 * <p>
 * This class should support configuration with respect to the levels
 * actually supported but this has not yet been implemented.
 * <p>
 * Subclasses need to define the resolve() method.
 * 
 * @author stella
 *
 */
public abstract class CartesianTileSelector extends TileSelector {
	private int tileSize=256;
	
	@Override
	public void select(Projection projection, double resolution, 
			double x1, double y1, double x2, double y2, 
			Collection<TileKey> destination) {
		Bounds projectedBounds=projection.getProjectedExtent();
		boolean xinversion=projection.isXAxisInverted();
		boolean yinversion=projection.isYAxisInverted();
		int nativeLevel=(int) Math.round(projection.toLevel(resolution));
			// Round to the closest integral resolution
			// TODO: Constrain this based on supported levels configured on the selector
		double nativeResolution=projection.fromLevel(nativeLevel);
		
		// Now get all coordinates into pixel units at nativeResolution
		// starting at the tile origin (upper left)
		double nativeOriginX;
		double nativeOriginY;
		
		x1/=nativeResolution;
		y1/=nativeResolution;
		x2/=nativeResolution;
		y2/=nativeResolution;
		
		// Axis inversion madness - gotta love it
		if (yinversion) {
			nativeOriginY=projectedBounds.getMaxy() / nativeResolution;
			y1=nativeOriginY-y1;
			y2=nativeOriginY-y2;
		} else {
			nativeOriginY=projectedBounds.getMiny() / nativeResolution;
			y1=y1-nativeOriginY;
			y2=y2-nativeOriginY;
		}
		
		if (xinversion) {
			nativeOriginX=projectedBounds.getMaxx() / nativeResolution;
			x1=nativeOriginX-x1;
			x2=nativeOriginX-x2;
		} else {
			nativeOriginX=projectedBounds.getMinx() / nativeResolution;
			x1=x1-nativeOriginX;
			x2=x2-nativeOriginX;
		}
		
		int startX=(int) Math.floor(Math.min(x1,x2)/tileSize);
		int startY=(int) Math.floor(Math.min(y1,y2)/tileSize);
		int endX=(int) Math.floor(Math.max(x1, x2)/tileSize);
		int endY=(int) Math.floor(Math.max(y1, y2)/tileSize);
		
		for (int j=startY; j<=endY; j++) {
			for (int i=startX; i<=endX; i++) {
				double projectedX;
				double projectedY;
				
				if (yinversion) projectedY=nativeOriginY - j*tileSize;
				else projectedY=nativeOriginY + j*tileSize;
				
				if (xinversion) projectedX=nativeOriginX - i*tileSize;
				else projectedX=nativeOriginX + i*tileSize;
				
				
				CartesianTileKey tk=new CartesianTileKey(this,
						nativeLevel,
						i,
						j,
						nativeResolution,
						projectedX,
						projectedY,
						tileSize);
				
				destination.add(tk);
			}
		}
	}
}
