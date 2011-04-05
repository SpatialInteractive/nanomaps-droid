package net.rcode.nanomaps.io;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

/**
 * Holder for the objects making up an HTTP interaction.
 * @author stella
 *
 */
public class HttpInteraction {
	/**
	 * Caller supplied object for correlating the request
	 */
	public Object correlation;
	
	public HttpRequest httpRequest;
	public HttpResponse httpResponse;
	public Throwable exception;
	
	public HttpCallback callback;
	
	public HttpInteraction(HttpRequest httpRequest, HttpCallback callback) {
		this.httpRequest=httpRequest;
	}
}
