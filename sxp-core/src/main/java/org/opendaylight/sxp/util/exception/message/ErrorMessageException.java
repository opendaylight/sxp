/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.exception.message;

import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorCodeNonExtended;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorSubCode;

public class ErrorMessageException extends Exception {

    /** */
    private static final long serialVersionUID = 801190427695923174L;

    private Exception carriedException;

    private byte[] data;

    private ErrorCode errorCode;

    private ErrorCodeNonExtended errorCodeNonExtended;

    private ErrorSubCode errorSubCode;

    private boolean legacy = true;

    public ErrorMessageException(ErrorCode errorCode, ErrorSubCode errorSubCode, byte[] data, Exception carriedException) {
        this.errorCode = errorCode;
        this.errorSubCode = errorSubCode;
        this.data = data;
        this.legacy = false;
        this.carriedException = carriedException;
    }

    public ErrorMessageException(ErrorCode errorCode, ErrorSubCode errorSubCode, Exception carriedException) {
        this(errorCode, errorSubCode, null, carriedException);
    }

    public ErrorMessageException(ErrorCode errorCode, Exception carriedException) {
        this(errorCode, null, null, carriedException);
    }

    public ErrorMessageException(ErrorCodeNonExtended errorCodeNonExtended, Exception carriedException) {
        this.errorCodeNonExtended = errorCodeNonExtended;
        this.carriedException = carriedException;
    }

    public Exception getCarriedException() {
        return carriedException;
    }

    public byte[] getData() {
        return data;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public ErrorCodeNonExtended getErrorCodeNonExtended() {
        return errorCodeNonExtended;
    }

    public ErrorSubCode getErrorSubCode() {
        return errorSubCode;
    }

    public boolean isLegacy() {
        return legacy;
    }
}
