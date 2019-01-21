/*
 * @(#)HttpExchangeImpl.java	1.8 07/01/02
 *
 * Copyright 2007 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package cn.kivensoft.httpserver.inner;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;

import cn.kivensoft.httpserver.Headers;
import cn.kivensoft.httpserver.HttpExchange;
import cn.kivensoft.httpserver.HttpPrincipal;

class HttpExchangeImpl extends HttpExchange {

	ExchangeImpl impl;

	HttpExchangeImpl(ExchangeImpl impl) {
		this.impl = impl;
	}

	public Headers getRequestHeaders() {
		return impl.getRequestHeaders();
	}

	public Headers getResponseHeaders() {
		return impl.getResponseHeaders();
	}

	public URI getRequestURI() {
		return impl.getRequestURI();
	}

	public String getRequestMethod() {
		return impl.getRequestMethod();
	}

	public HttpContextImpl getHttpContext() {
		return impl.getHttpContext();
	}

	public void close() {
		impl.close();
	}

	public InputStream getRequestBody() {
		return impl.getRequestBody();
	}

	public int getResponseCode() {
		return impl.getResponseCode();
	}

	public OutputStream getResponseBody() {
		return impl.getResponseBody();
	}

	public void sendResponseHeaders(int rCode, long contentLen)
			throws IOException {
		impl.sendResponseHeaders(rCode, contentLen);
	}

	public InetSocketAddress getRemoteAddress() {
		return impl.getRemoteAddress();
	}

	public InetSocketAddress getLocalAddress() {
		return impl.getLocalAddress();
	}

	public String getProtocol() {
		return impl.getProtocol();
	}

	public Object getAttribute(String name) {
		return impl.getAttribute(name);
	}

	public void setAttribute(String name, Object value) {
		impl.setAttribute(name, value);
	}

	@Override
	public void setStreams(InputStream i, OutputStream o) {
		impl.setStreams(i, o);
	}

	public HttpPrincipal getPrincipal() {
		return impl.getPrincipal();
	}

	ExchangeImpl getExchangeImpl() {
		return impl;
	}

}
