/*
 * @(#)HttpError.java	1.4 07/01/02
 *
 * Copyright 2007 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package cn.kivensoft.httpserver.inner;

/**
 * A Http error
 */
class HttpError extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public HttpError (String msg) {
	super (msg);
    }
}
