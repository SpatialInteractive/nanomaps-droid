package net.rcode.nanomaps;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

/**
 * Platform default resource loader singleton.
 * @author stella
 */
public class DefaultResourceLoader extends ResourceLoader {
	private static DefaultResourceLoader INSTANCE;
	
	public static DefaultResourceLoader getInstance() {
		if (INSTANCE==null) INSTANCE=new DefaultResourceLoader();
		return INSTANCE;
	}
	
	static final int DEFAULT_WORKERS_PER_QUEUE=2;
	static final int DEFAULT_IDLE_LINGER=10000;
	
	Map<String, IOQueue> queues=new HashMap<String, IOQueue>();
	
	IOQueue getQueue(String key) {
		synchronized (queues) {
			IOQueue existing=queues.get(key);
			if (existing==null) {
				existing=new IOQueue(key, DEFAULT_WORKERS_PER_QUEUE, DEFAULT_IDLE_LINGER);
				queues.put(key, existing);
			}
			return existing;
		}
	}
	
	class IOQueue {
		int workerNumber;
		String key;
		ArrayList<IOWorker> workers=new ArrayList<IOWorker>();
		int maxWorkers;
		int idleLinger;
		int idleCount;
		LinkedList<IORequest> contents=new LinkedList<IORequest>();
		
		public IOQueue(String key, int maxWorkers, int idleLinger) {
			this.key=key;
			this.maxWorkers=maxWorkers;
		}
		
		/**
		 * Get the next IO request.  Return null if should exit.
		 * @return next or null
		 */
		public IORequest next(IOWorker worker) {
			synchronized (this) {
				if (worker.firstTime) {
					worker.firstTime=false;
					// We artificially increase idleCount in startOne
					// to avoid a race condition.  Decrease it here.
					idleCount--;
				}
				if (contents.isEmpty()) {
					try {
						idleCount++;
						this.wait(idleLinger);
						idleCount--;
					} catch (InterruptedException e) {
						idleCount--;
						workers.remove(worker);
						return null;
					}
					if (contents.isEmpty()) {
						workers.remove(worker);
						return null;
					}
				}
				
				IORequest ret=contents.removeFirst();
				if (!contents.isEmpty()) this.notify();
				return ret;
			}
		}
		
		public void add(IORequest item) {
			synchronized (this) {
				contents.add(item);
				if (idleCount==0 && workers.size()<maxWorkers) {
					startOne();
				}
				this.notify();
			}
		}
		
		public void remove(IORequest item) {
			synchronized (this) {
				contents.remove(item);
			}
		}
		
		private void startOne() {
			synchronized (this) {
				idleCount++;
				IOWorker worker=new IOWorker(this, key + '-' + (++workerNumber));
				worker.start();
			}
		}
	}
	
	class IOWorker implements Runnable {
		boolean firstTime;
		String name;
		Thread runningThread;
		IOQueue queue;
		
		public IOWorker(IOQueue queue, String name) {
			this.queue=queue;
			this.name=name;
		}
		
		@Override
		public void run() {
			Log.d(Constants.LOG_TAG, "Starting worker thread " + name);
			for (;;) {
				IORequest request=queue.next(this);
				if (request==null) break;
				
				long time=SystemClock.uptimeMillis();
				Log.d(Constants.LOG_TAG, name + " Requesting " + request.uri);
				request.process(this);
				time=SystemClock.uptimeMillis()-time;
				Log.d(Constants.LOG_TAG, name + " Completed " + request.uri + " in " + time + "ms");
			}
			Log.d(Constants.LOG_TAG, "Ending worker thread " + name);
		}
		
		public void start() {
			runningThread=new Thread(this);
			runningThread.setName("nmio-" + name);
			runningThread.start();
		}
	}
	
	class IORequest implements ResourceLoader.Request {
		Looper originatingLooper;
		
		IOQueue queue;
		Uri uri;
		DataHandler dataHandler;
		Callback callback;
		boolean complete;
		boolean loaded;
		Object results;
		
		@Override
		public Object getResults() {
			return results;
		}

		@Override
		public boolean isComplete() {
			return complete;
		}

		@Override
		public boolean isLoaded() {
			return loaded;
		}
		
		@Override
		public void cancel() {
			synchronized (this) {
				this.callback=null;
				this.dataHandler=null;
			}
			queue.remove(this);
		}
		
		public void process(IOWorker context) {
			try {
				URL url=new URL(uri.toString());
				InputStream in;
				in=url.openStream();
				try {
					DataHandler localDataHandler;
					Object result;
					synchronized (this) {
						localDataHandler=dataHandler;
					}
					
					if (localDataHandler==null) result=null;
					else result=localDataHandler.transformResult(in, -1);
					
					if (result==null) {
						Log.e(Constants.LOG_TAG, "No decoded results for " + uri);
						finish(true, false, null);
					} else {
						finish(true, true, result);
					}
				} finally {
					in.close();
				}
			} catch (Throwable t) {
				Log.e(Constants.LOG_TAG, "Error requesting " + uri, t);
				finish(true, false, null);
			}
		}
		
		public void finish(boolean complete, boolean loaded, Object results) {
			final Callback localCallback;
			synchronized (this) {
				this.complete=complete;
				this.loaded=loaded;
				this.results=results;
				localCallback=this.callback;
			}			
			
			if (localCallback!=null) {
				new Handler(originatingLooper).post(new Runnable() {
					public void run() {
						localCallback.onComplete(IORequest.this);
					}
				});
			}
		}
	}
	
	private DefaultResourceLoader() {
	}
	
	@Override
	public Request loadResource(Uri uri, DataHandler dataHandler,
			Callback callback) {
		IORequest request=new IORequest();
		request.originatingLooper=Looper.myLooper();
		request.dataHandler=dataHandler;
		request.callback=callback;
		request.uri=uri;
		request.queue=getQueue("default");
		request.queue.add(request);
		
		return request;
	}

}
