package net.rcode.nanomaps;

/**
 * Extends CartesianTileSelector to resolve tiles from external
 * bitmap sources.  It returns BitmapTile instances.
 * 
 * @author stella
 *
 */
public class UriTileSelector extends CartesianTileSelector {
	private TileUriPattern pattern;
	private ResourceLoader loader;
	
	public UriTileSelector(String pattern, ResourceLoader loader) throws IllegalArgumentException {
		this(new TileUriPattern(pattern), loader);
	}
	
	public UriTileSelector(TileUriPattern pattern, ResourceLoader loader) {
		this.pattern=pattern;
		if (loader==null) loader=DefaultResourceLoader.getInstance();
		this.loader=loader;
	}
	
	public UriTileSelector(TileUriPattern pattern) {
		this(pattern, null);
	}
	
	public UriTileSelector(String pattern) {
		this(pattern, null);
	}
	
	@Override
	public Tile resolve(TileKey key, TileSet current) {
		CharSequence uri=pattern.uriFor(key);
		BitmapTile tile=new BitmapTile(key);
		tile.initializeFrom(current);
		if (tile.getState()!=Tile.STATE_LOADED) {
			// Load it
			tile.load(loader, uri);
		}
		return tile;
	}
	
}
