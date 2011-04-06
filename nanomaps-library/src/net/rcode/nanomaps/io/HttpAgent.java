package net.rcode.nanomaps.io;

import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;

import net.rcode.nanomaps.Constants;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.io.ChunkedInputStream;
import org.apache.http.impl.io.ContentLengthInputStream;
import org.apache.http.impl.io.HttpRequestWriter;
import org.apache.http.impl.io.HttpResponseParser;
import org.apache.http.impl.io.SocketInputBuffer;
import org.apache.http.impl.io.SocketOutputBuffer;
import org.apache.http.message.BasicLineFormatter;
import org.apache.http.message.BasicLineParser;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import android.util.Log;


/**
 * An HttpAgent talks to a single http server and pipelines requests to it.
 * TODO: This class started with one design in mind and ended with another.
 * It needs some rework.
 * 
 * @author stella
 */
public class HttpAgent {
	private String host;
	private int port;
	private Socket socket;

	private HttpParams params;
	private SocketInputBuffer socketIn;
	private SocketOutputBuffer socketOut;
	
	private LinkedList<HttpInteraction> pending=new LinkedList<HttpInteraction>();
	
	public HttpAgent(String host, int port) {
		this.params=new BasicHttpParams();
		this.host=host;
		this.port=port;
	}
	
	public void failAll(Throwable t) {
		while (!pending.isEmpty()) {
			HttpInteraction hi=pending.removeFirst();
			hi.httpResponse=null;
			hi.exception=t;
			try {
				hi.callback.handleHttpResponse(hi);
			} catch (Throwable th) {
				Log.e(Constants.LOG_TAG, "Recursive error handling error.  Not much to do about it but ignore.", th);
			}
		}
	}
	
	public int getPendingCount() {
		return pending.size();
	}

	public void shutdown() {
		socketIn=null;
		socketOut=null;
		if (socket!=null) {
			try {
				socket.close();
			} catch (IOException e) {
				Log.e(Constants.LOG_TAG, "Error closing socket", e);
			}
		}
		socket=null;
	}
	
	public void submit(HttpInteraction interaction) {
		pending.add(interaction);
	}
	
	public void doIO() throws IOException, HttpException {
		if (socket==null) {
			Log.d(Constants.LOG_TAG, "Establishing http connection to " + host + ":" + port);
			socket=new Socket(host, port);
			socket.setSoLinger(false, 0);
			socket.setSoTimeout(30000);
			socketIn=new SocketInputBuffer(socket, 1500, params);
			socketOut=new SocketOutputBuffer(socket, 1500, params);
		}
		
		// Write all pending requests
		HttpRequestWriter requestWriter=new HttpRequestWriter(socketOut, new BasicLineFormatter(), params);
		for (HttpInteraction interaction: pending) {
			HttpRequest request=interaction.httpRequest;
			requestWriter.write(request);
		}
		socketOut.flush();
		
		// Now loop through reading responses and dispatching
		HttpResponseParser responseParser=new HttpResponseParser(socketIn, new BasicLineParser(), new DefaultHttpResponseFactory(), params);
		while (!pending.isEmpty()) {
			HttpInteraction next=pending.getFirst();
			HttpMessage message=responseParser.parse();
			next.httpResponse=(HttpResponse) message;
			
			BasicHttpEntity entity=new BasicHttpEntity();
			boolean hasBody=false;
			
			Header header=message.getFirstHeader("Content-Encoding");
			if (header!=null) {
				entity.setContentEncoding(header);
			}
			
			header=message.getFirstHeader("Content-Type");
			if (header!=null) {
				entity.setContentType(header);
			}
			
			header=message.getFirstHeader("Content-Length");
			if (header!=null) {
				long length=Long.parseLong(header.getValue());
				entity.setContentLength(length);
				entity.setContent(new ContentLengthInputStream(socketIn, length));
				hasBody=true;
			}
			
			header=message.getFirstHeader("Transfer-Encoding");
			if (header!=null) {
				if (header.getValue().indexOf("chunked")>=0) {
					entity.setChunked(true);
					entity.setContent(new ChunkedInputStream(socketIn));
					hasBody=true;
				}
			}
			
			next.httpResponse.setEntity(entity);
			
			if (!hasBody) {
				throw new ProtocolException("Response was neither fixed length or chunked");
			}
			
			next.callback.handleHttpResponse(next);

			// Success.  Shift it off.
			pending.removeFirst();

			header=message.getFirstHeader("Connection");
			if (header!=null && header.getValue().indexOf("close")>=0) {
				// Close the connection
				Log.d(Constants.LOG_TAG, "Server signalled to close the connection with " + pending.size() + " responses outstanding.");
				shutdown();
				return;
			} else {
				//Log.d(Constants.LOG_TAG, "Persisting connection to next request");
				
			}
		}
	}
	
}
