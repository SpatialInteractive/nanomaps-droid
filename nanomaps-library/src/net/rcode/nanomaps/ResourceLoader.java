package net.rcode.nanomaps;

import java.io.InputStream;

import android.net.Uri;

/**
 * Abstraction for requesting to load resources.
 * @author stella
 *
 */
public abstract class ResourceLoader {
	public interface Callback {
		public void onComplete(Request request);
	}
	
	public interface DataHandler {
		public Object transformResult(InputStream input, int size);
	}
	
	public interface Request {
		/**
		 * Signals resource completion, either due to load or (non-retrying)
		 * error.
		 * @return true if the resource request is complete.
		 */
		public boolean isComplete();
		
		/**
		 * @return true if the resource is complete without error
		 */
		public boolean isLoaded();
		
		/**
		 * @return the data if isLoaded()
		 */
		public Object getResults();
		
		/**
		 * Cancel the request if possible.  This will remove any callback
		 * and may stop io operations in progress.
		 */
		public void cancel();
	}

	/**
	 * Initiate a resource load
	 * @param uriSpec
	 * @return Request
	 */
	public final Request loadResource(CharSequence uriSpec, DataHandler dataHandler, Callback callback) {
		Uri uri=Uri.parse(uriSpec.toString());
		return loadResource(uri, dataHandler, callback);
	}

	/**
	 * Initiate a resource load
	 * @param uri
	 * @return Request
	 */
	public abstract Request loadResource(Uri uri, DataHandler dataHandler, Callback callback);
}
