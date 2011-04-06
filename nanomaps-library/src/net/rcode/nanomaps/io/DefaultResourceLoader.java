package net.rcode.nanomaps.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.http.HttpRequest;
import org.apache.http.message.BasicHttpRequest;

import net.rcode.nanomaps.util.Constants;

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
	
	static final int DEFAULT_PIPELINE_DEPTH=7;
	static final int DEFAULT_WORKERS_PER_QUEUE=3;
	static final int DEFAULT_IDLE_LINGER=30000;
	
	Map<String, IOQueue> queues=new HashMap<String, IOQueue>();
	
	IOQueue getQueue(String key, int maxWorkers, int idleLinger) {
		synchronized (queues) {
			IOQueue existing=queues.get(key);
			if (existing==null) {
				existing=new IOQueue(key, maxWorkers, idleLinger);
				queues.put(key, existing);
			}
			return existing;
		}
	}
	
	IOQueue getHttpQueue(Uri uri) {
		// Start an http worker queue
		String queueName=uri.getScheme() + ':' + uri.getAuthority();
		IOQueue queue=getQueue(queueName, DEFAULT_WORKERS_PER_QUEUE, DEFAULT_IDLE_LINGER);
		queue.startHttp(uri.getHost(), uri.getPort());
		return queue;
	}
	
	void removeQueue(String key) {
		synchronized (queues) {
			queues.remove(key);
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
		
		// For http worker queues
		boolean ishttp;
		String httpHost;
		int httpPort;
		int httpPipelineDepth=DEFAULT_PIPELINE_DEPTH;
		
		public IOQueue(String key, int maxWorkers, int idleLinger) {
			this.key=key;
			this.maxWorkers=maxWorkers;
			this.idleLinger=idleLinger;
		}
		
		public void startHttp(String host, int port) {
			synchronized (this) {
				if (!ishttp) {
					this.ishttp=true;
					this.httpHost=host;
					this.httpPort=port>0 ? port : 80;
				}
			}
		}
		
		public void startMaximum() {
			synchronized(this) {
				while (workers.size()<maxWorkers) {
					startOne();
				}
			}
		}
		
		/**
		 * Get the next IO request.  Return null if should exit.
		 * @return next or null
		 */
		public IORequest next(IOWorker worker, boolean noblock) {
			synchronized (this) {
				//Log.d(Constants.LOG_TAG, "Read next from queue of " + contents.size());
				if (noblock) {
					if (contents.isEmpty()) return null;
					return contents.removeFirst();
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
						if (workers.isEmpty()) {
							// This may race slightly.  At worst, a dangling reference
							// to this queue will cause us to fire up another thread
							// which will expire shortly thereafter
							removeQueue(key);
						}
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
				/*
				if (ishttp && contents.isEmpty()) {
					// We first want to see if there are available slots on a worker
					// that we can queue directly to
					IOWorker idlestWorker=null;
					int idlestSlots=0;
					for (int i=0; i<workers.size(); i++) {
						IOWorker w=workers.get(i);
						int s=w.getAvailableSlots();
						if (s>idlestSlots) {
							idlestSlots=s;
							idlestWorker=w;
						}
					}
					
					if (idlestWorker!=null && idlestWorker.queueDirect(item, false)) return;
				}
				*/
				
				// It wasn't http or there weren't any workers with free slots
				// Put into the regular queue
				contents.add(item);
				this.notify();
				if (ishttp) startMaximum();
				else {
					if (idleCount==0 && workers.size()<maxWorkers) {
						startOne();
					} else {
						Log.d(Constants.LOG_TAG, "Not starting worker for request: idle=" + idleCount + ", size=" + workers.size());
					}
				}
			}
		}
		
		public void remove(IORequest item) {
			synchronized (this) {
				contents.remove(item);
			}
		}
		
		private void startOne() {
			synchronized (this) {
				IOWorker worker=new IOWorker(this, key + '-' + (++workerNumber));
				workers.add(worker);
				worker.start();
			}
		}
		
		public void removeWorker(IOWorker worker) {
			synchronized (this) {
				workers.remove(worker);
				if (workers.size()==0 && !contents.isEmpty()) {
					startOne();
				}
			}
		}
	}
	
	class IOWorker implements Runnable, HttpCallback {
		boolean firstTime=true;
		String name;
		Thread runningThread;
		IOQueue queue;
		HttpAgent httpAgent;
		
		public IOWorker(IOQueue queue, String name) {
			this.queue=queue;
			this.name=name;
		}
		
		public HttpAgent getHttpAgent() {
			if (httpAgent==null) {
				httpAgent=new HttpAgent(queue.httpHost, queue.httpPort);
			}
			return httpAgent;
		}
		
		public int getAvailableSlots() {
			if (httpAgent==null) return 0;
			return queue.httpPipelineDepth - httpAgent.getPendingCount();
		}
		
		public synchronized boolean queueDirect(IORequest request, boolean nocheck) {
			HttpAgent agent=getHttpAgent();
			if (!nocheck && agent.getPendingCount()>=queue.httpPipelineDepth) return false;
					
			HttpRequest htr=httpRequestFromUri(request.uri);
			HttpInteraction interaction=new HttpInteraction(htr, this);
			interaction.correlation=request;
			interaction.callback=this;
			agent.submit(interaction);
			return false;
		}
		
		public synchronized boolean flushQueueToAgent() {
			HttpAgent agent=getHttpAgent();
			boolean firstRequest=true;
			boolean ret=true;
			while (agent.getPendingCount() < queue.httpPipelineDepth) {
				// Only block on the first time through
				IORequest request=queue.next(this, !firstRequest);
				if (request==null) {
					if (firstRequest) ret=false;
					break;
				}

				if (firstRequest) firstRequest=false;
				queueDirect(request, true);
			}
			
			//Log.d(Constants.LOG_TAG, "Flushed " + agent.getPendingCount() + " to agent.");
			return ret;
		}
		
		@Override
		public void run() {
			Log.d(Constants.LOG_TAG, "Starting worker thread " + name);
			
			if (queue.ishttp) {
				// Http queue processing is a little different
				// We get an agent and keep giving things to it
				// until its full and then go have it service itself
				runHttp();
			} else {
				// Traditional queue processing - one at a time
				for (;;) {
					IORequest request=queue.next(this, false);
					if (request==null) break;
					try {
						runRequest(request);
					} catch (Throwable t) {
						Log.e(Constants.LOG_TAG, "Error processing request " + request.uri, t);
						request.finish(true, false, null);
					}
				}
			}
			Log.d(Constants.LOG_TAG, "Ending worker thread " + name);
			queue.removeWorker(this);
			
			if (httpAgent!=null) {
				httpAgent.shutdown();
			}
		}
		
		private void runHttp() {
			for (;;) {
				HttpAgent agent=getHttpAgent();
				
				// Drain stuff into the queue
				if (!flushQueueToAgent()) {
					return;
				}
				while (agent.getPendingCount()>0) {
					try {
						Log.d(Constants.LOG_TAG, name + " HttpAgent transmitting pipeline of " + agent.getPendingCount() + " items.");
						agent.doIO();
					} catch (Exception e) {
						Log.e(Constants.LOG_TAG, "Error doing agent io", e);
						agent.failAll(e);
						agent.shutdown();	// Close connection
					}
				}
			}
		}
		
		
		
		private void runRequest(IORequest request) throws IOException {
			Uri uri=request.uri;
			/*
			if (request.pipelineable && "http".equals(uri.getScheme())) {
				// Punt it to a dedicated http queue
				IOQueue httpQueue=getHttpQueue(uri);
				httpQueue.add(request);
				return;
			}
			*/
			
			// Process it the old fashioned way
			long time=SystemClock.uptimeMillis();
			//Log.d(Constants.LOG_TAG, name + " Requesting " + request.uri);
			
			URL url=new URL(uri.toString());
			InputStream input=url.openStream();
			try {
				request.processStream(this, input, -1);
			} finally {
				input.close();
			}
			time=SystemClock.uptimeMillis()-time;
			Log.d(Constants.LOG_TAG, name + " Completed " + request.uri + " in " + time + "ms");
		}

		public void start() {
			runningThread=new Thread(this);
			runningThread.setName("nmio-" + name);
			runningThread.start();
		}
		
		@Override
		public void handleHttpResponse(HttpInteraction interaction) throws IOException {
			IORequest iorequest=(IORequest) interaction.correlation;
			if (interaction.exception!=null) {
				Log.e(Constants.LOG_TAG, "Error running pipelined request " + iorequest.uri, interaction.exception);
				iorequest.finish(true, false, null);
				return;
			}
			int statusCode=interaction.httpResponse.getStatusLine().getStatusCode();
			if (statusCode<200 || statusCode>300) {
				Log.e(Constants.LOG_TAG, "Bad http status code for pipelined request " + iorequest.uri + " (" + statusCode + ")", interaction.exception);
				iorequest.finish(true, false, null);
				return;
			}
			
			InputStream stream=interaction.httpResponse.getEntity().getContent();
			try {
				iorequest.processStream(this, stream, -1);
			} finally {
				stream.close();
			}
		}
	}
	
	class IORequest implements ResourceLoader.Request {
		boolean pipelineable=true;
		Looper originatingLooper;
		
		long startTime=SystemClock.uptimeMillis();
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
		
		public void processStream(IOWorker context, InputStream input, int expectedLength) throws IOException {
			DataHandler localDataHandler;
			Object result;
			synchronized (this) {
				localDataHandler=dataHandler;
			}
			
			if (localDataHandler==null) result=null;
			else result=localDataHandler.transformResult(input, expectedLength);
			
			if (result==null) {
				Log.e(Constants.LOG_TAG, "No decoded results for " + uri);
				finish(true, false, null);
			} else {
				finish(true, true, result);
			}
		}
		
		public void finish(boolean complete, boolean loaded, Object results) {
			final Callback localCallback;
			synchronized (this) {
				if (this.callback==null) return;	// Dispatch once
				this.complete=complete;
				this.loaded=loaded;
				this.results=results;
				localCallback=this.callback;
				this.callback=null;
			}			
			
			long runTime=SystemClock.uptimeMillis() - startTime;
			Log.d(Constants.LOG_TAG, "Finished request to " + uri + " (loaded=" + loaded + ") in " + runTime + "ms");
			
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
	
	public HttpRequest httpRequestFromUri(Uri uri) {
		String path=uri.getEncodedPath();
		String query=uri.getEncodedQuery();
		if (query!=null) path=path + '?' + query;
		BasicHttpRequest bhr=new BasicHttpRequest("GET", path);
		String hostHeader=uri.getHost();
		if (uri.getPort()>0) hostHeader=hostHeader + ':' + uri.getPort();
		bhr.addHeader("Host", uri.getHost());
		bhr.addHeader("User-Agent", "nanomaps-droid");
		
		return bhr;
	}

	@Override
	public Request loadResource(Uri uri, DataHandler dataHandler,
			Callback callback) {
		IORequest request=new IORequest();
		request.originatingLooper=Looper.myLooper();
		request.dataHandler=dataHandler;
		request.callback=callback;
		request.uri=uri;
		
		if (request.pipelineable && "http".equals(uri.getScheme())) {
			request.queue=getHttpQueue(uri);
		} else {
			request.queue=getQueue("default", DEFAULT_WORKERS_PER_QUEUE, DEFAULT_IDLE_LINGER);
		}
		
		request.queue.add(request);
		
		return request;
	}

}
