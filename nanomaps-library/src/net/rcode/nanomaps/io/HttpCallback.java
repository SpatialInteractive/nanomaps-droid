package net.rcode.nanomaps.io;

import java.io.IOException;


public interface HttpCallback {
	public void handleHttpResponse(HttpInteraction interaction) throws IOException;
}
