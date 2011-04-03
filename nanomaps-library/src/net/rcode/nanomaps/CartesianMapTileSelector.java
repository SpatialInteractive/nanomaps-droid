package net.rcode.nanomaps;

/**
 * A tile selector for tiles laid out in "normal" fashion for
 * the web.
 * @author stella
 *
 */
public class CartesianMapTileSelector extends MapTileSelector {
	private int tileSize=256;
	
	@Override
	public void select(Projection projection, double resolution, 
			double x1, double y1, double x2, double y2, 
			Handler callback) {
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
				
				
				TileKey tk=new TileKey(nativeLevel,
						i,
						j,
						nativeResolution,
						projectedX,
						projectedY,
						tileSize);
				
				callback.handleTile(tk);
			}
		}
	}
	
	
	public class TileKey extends MapTileSelector.TileKey {
		public final int level;
		public final int tileX;
		public final int tileY;
		
		public TileKey(int level, int tileX, int tileY,
				double resolution, double scaledX, double scaledY,
				int size) {
			super(resolution, scaledX, scaledY, size);
			this.level=level;
			this.tileX=tileX;
			this.tileY=tileY;
		}
		
		@Override
		public int hashCode() {
			return (level * 97) ^ tileX ^ tileY;
		}
		
		@Override
		public boolean equals(Object o) {
			TileKey other=(TileKey)o;
			if (other==null) return false;
			return other.level==this.level && other.tileX==this.tileX && other.tileY==this.tileY;
		}
		
		@Override
		public String toString() {
			return String.format("Tile(level=%s,x=%s,y=%s)", level, tileX, tileY);
		}
	}
}
