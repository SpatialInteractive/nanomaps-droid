package net.rcode.nanomaps;

import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/**
 * Tile implementation for dealing with bitmaps.
 * 
 * @author stella
 *
 */
public class BitmapTile extends Tile {
	/**
	 * The current request if being loaded
	 */
	ResourceLoader.Request request;
	
	private static final ResourceLoader.DataHandler BITMAP_DECODER=new ResourceLoader.DataHandler() {
		@Override
		public Object transformResult(InputStream in, int size) {
			return BitmapFactory.decodeStream(in);
		}
	};
	
	public BitmapTile(TileKey key) {
		super(key);
	}
	
	@Override
	public void destroy() {
		super.destroy();
		if (request!=null) {
			request.cancel();
			request=null;
		}
	}
	
	/**
	 * Attempt to assemble the tile from current material if possible.
	 * This may take current tiles and build a preview.
	 * @param current
	 */
	public void initializeFrom(TileSet current) {
		return;
	}
	
	/**
	 * Schedules this tile for loading from the given loader
	 * @param loader
	 * @param uri
	 */
	public void load(ResourceLoader loader, CharSequence uri) {
		if (request!=null) throw new IllegalStateException();
		request=loader.loadResource(uri,
			BITMAP_DECODER,
			new ResourceLoader.Callback() {
				@Override
				public void onComplete(ResourceLoader.Request request) {
					// Clear out our request
					BitmapTile.this.request=null;
					
					if (!request.isLoaded()) {
						// Error
						updateContent(getDrawable(), STATE_ERROR);
					} else {
						// Success
						Drawable drawable=new BitmapDrawable((Bitmap) request.getResults());
						updateContent(drawable, STATE_LOADED);
					}
				}
			});
	}
}
