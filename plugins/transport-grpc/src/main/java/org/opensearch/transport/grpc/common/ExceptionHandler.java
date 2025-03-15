/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.transport.grpc.common;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

import org.opensearch.core.concurrency.OpenSearchRejectedExecutionException;

import io.grpc.Status;

/**
 * Handler for converting Java exceptions to gRPC Status exceptions.
 */
public class ExceptionHandler {

    /**
     * Maps specific Java exceptions to gRPC status codes.
     * This method is similar to the status() method in ExceptionsHelper.java.
     *
     * @param t The exception to convert
     * @return A gRPC Status exception
     */
    public static Throwable annotateException(Throwable t) {
        // TODO add more exception to GRPC status code mappings
        if (t instanceof IllegalArgumentException) {
            return Status.INVALID_ARGUMENT.withDescription(t.getMessage()).withCause(t).asRuntimeException();
        } else if (t instanceof NoSuchElementException) {
            return Status.NOT_FOUND.withDescription(t.getMessage()).withCause(t).asRuntimeException();
        } else if (t instanceof TimeoutException) {
            return Status.DEADLINE_EXCEEDED.withDescription(t.getMessage()).withCause(t).asRuntimeException();
        } else if (t instanceof OpenSearchRejectedExecutionException) {
            return Status.RESOURCE_EXHAUSTED.withDescription(t.getMessage()).withCause(t).asRuntimeException();
        } else if (t instanceof UnsupportedOperationException) {
            return Status.UNIMPLEMENTED.withDescription(t.getMessage()).withCause(t).asRuntimeException();
        } else {
            return Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
        }
    }
}
