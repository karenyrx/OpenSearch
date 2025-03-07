/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.grpc.common;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

import io.grpc.Status;

public class ExceptionHandler {

    // TODO consolidate this with ExceptionsHelper.grpcStatus()
    public static Throwable annotateException(Throwable e) {
        if (e instanceof IllegalArgumentException) {
            return Status.INVALID_ARGUMENT.withDescription(e.getMessage()).withCause(e).asRuntimeException();
        } else if (e instanceof NoSuchElementException) {
            return Status.NOT_FOUND.withDescription(e.getMessage()).withCause(e).asRuntimeException();
        } else if (e instanceof TimeoutException) {
            return Status.DEADLINE_EXCEEDED.withDescription(e.getMessage()).withCause(e).asRuntimeException();
        } else if (e instanceof UnsupportedOperationException) {
            return Status.UNIMPLEMENTED.withDescription(e.getMessage()).withCause(e).asRuntimeException();
        } else {
            return Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException();
        }
    }
}
