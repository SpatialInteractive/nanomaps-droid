package net.rcode.nanomaps;

/**
 * Extends TileKey to represent tiles in an (x,y) grid per level.
 * This is the standard arrangement for online map tiles and is
 * the basis for most of the tile loading machinery in this library.
 * 
 * @author stella
 *
 */
public final class CartesianTileKey implements TileKey {
	private int _hash;
	private CartesianTileSelector source;
	private final double resolution;
	private final double scaledX;
	private final double scaledY;
	private final int size;
	
	public final int level;
	public final int tileX;
	public final int tileY;
	
	public CartesianTileKey(CartesianTileSelector source, int level, int tileX, int tileY,
			double resolution, double scaledX, double scaledY,
			int size) {
		this.source=source;
		this.level=level;
		this.tileX=tileX;
		this.tileY=tileY;

		this.resolution=resolution;
		this.scaledX=scaledX;
		this.scaledY=scaledY;
		this.size=size;
		
		_hash=(level * 97) ^ tileX ^ tileY ^ source.hashCode();		
	}

	@Override
	public double getResolution() {
		return resolution;
	}

	@Override
	public double getScaledX() {
		return scaledX;
	}

	@Override
	public double getScaledY() {
		return scaledY;
	}

	@Override
	public int getSize() {
		return size;
	}

	@Override
	public int hashCode() {
		return _hash;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof CartesianTileKey)) return false;
		CartesianTileKey other=(CartesianTileKey)o;
		return other.source==this.source && other.level==this.level && other.tileX==this.tileX && other.tileY==this.tileY;
	}
	
	@Override
	public String toString() {
		return String.format("Tile(level=%s,x=%s,y=%s)", level, tileX, tileY);
	}

}