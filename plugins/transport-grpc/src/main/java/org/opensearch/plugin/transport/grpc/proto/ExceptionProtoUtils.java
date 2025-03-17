/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.plugin.transport.grpc.proto;

import org.opensearch.ExceptionsHelper;
import org.opensearch.OpenSearchException;
import org.opensearch.protobuf.ErrorCause;

import java.io.IOException;

/**
 * Utility class for converting Exception objects to Protocol Buffers.
 * This class handles the conversion of OpenSearchException and other Throwable instances
 * to their Protocol Buffer representation.
 */
public class ExceptionProtoUtils {

    private ExceptionProtoUtils() {
        // Utility class, no instances
    }

    /**
     * Converts an OpenSearchException to its Protocol Buffer representation.
     * This method is equivalent to the toXContent method in OpenSearchException.
     *
     * @param exception The OpenSearchException to convert
     * @return A Protocol Buffer ErrorCause representation
     * @throws IOException if there's an error during conversion
     */
    public static ErrorCause toProto(OpenSearchException exception) throws IOException {
        Throwable ex = ExceptionsHelper.unwrapCause(exception);
        if (ex != exception) {
            return generateThrowableProto(ex);
        } else {
            return innerToProto(exception, getExceptionName(exception), exception.getMessage(), exception.getCause());
        }
    }

    /**
     * Static helper method that renders {@link OpenSearchException} or {@link Throwable} instances
     * as Protocol Buffers.
     * <p>
     * This method is usually used when the {@link Throwable} is rendered as a part of another Protocol Buffer object.
     * It is equivalent to the generateThrowableXContent method in OpenSearchException.
     *
     * @param t The throwable to convert
     * @return A Protocol Buffer ErrorCause representation
     * @throws IOException if there's an error during conversion
     */
    public static ErrorCause generateThrowableProto(Throwable t) throws IOException {
        t = ExceptionsHelper.unwrapCause(t);

        if (t instanceof OpenSearchException) {
            return toProto((OpenSearchException) t);
        } else {
            return innerToProto(t, getExceptionName(t), t.getMessage(), t.getCause());
        }
    }

    /**
     * Inner helper method for converting a Throwable to its Protocol Buffer representation.
     * This method is equivalent to the innerToXContent method in OpenSearchException.
     *
     * @param throwable The throwable to convert
     * @param type The exception type
     * @param message The exception message
     * @param cause The exception cause
     * @return A Protocol Buffer ErrorCause representation
     * @throws IOException if there's an error during conversion
     */
    protected static ErrorCause innerToProto(Throwable throwable, String type, String message, Throwable cause) throws IOException {
        ErrorCause.Builder errorCauseBuilder = ErrorCause.newBuilder();

        // Set exception type
        errorCauseBuilder.setType(type);

        // Set exception message (reason)
        if (message != null) {
            errorCauseBuilder.setReason(message);
        }

        // Add metadata if the throwable is an OpenSearchException
        // if (throwable instanceof OpenSearchException) {
        // OpenSearchException exception = (OpenSearchException) throwable;

        // // Add index and shard information if available
        // if (exception.getIndex() != null) {
        // errorCauseBuilder.setIndex(exception.getIndex().getName());
        // if (exception.getShardId() != null) {
        // errorCauseBuilder.setShard(exception.getShardId().getId());
        // }
        // }

        // // Add resource type and ID if available
        // if (exception.getResourceType() != null) {
        // errorCauseBuilder.setResourceType(exception.getResourceType());
        // List<String> resourceIds = exception.getResourceId();
        // if (resourceIds != null && !resourceIds.isEmpty()) {
        // errorCauseBuilder.addAllResourceId(resourceIds);
        // }
        // }
        // }

        // TODO needed?
        // if (throwable instanceof OpenSearchException) {
        // OpenSearchException exception = (OpenSearchException) throwable;
        // exception.metadataToXContent(builder, params);
        // }

        // Add nested cause if available
        // todo how to pass params?
        // if (params.paramAsBoolean(REST_EXCEPTION_SKIP_CAUSE, REST_EXCEPTION_SKIP_CAUSE_DEFAULT) == false) {

        if (cause != null) {
            errorCauseBuilder.setCausedBy(generateThrowableProto(cause));
        }

        // Add stack trace
        // TODO how to pass params?
        // if (params.paramAsBoolean(REST_EXCEPTION_SKIP_STACK_TRACE, REST_EXCEPTION_SKIP_STACK_TRACE_DEFAULT) == false) {
        errorCauseBuilder.setStackTrace(ExceptionsHelper.stackTrace(throwable));

        // Add suppressed exceptions
        Throwable[] allSuppressed = throwable.getSuppressed();
        if (allSuppressed.length > 0) {
            for (Throwable suppressed : allSuppressed) {
                errorCauseBuilder.addSuppressed(generateThrowableProto(suppressed));
            }
        }

        return errorCauseBuilder.build();
    }

    /**
     * Returns an underscore case name for the given exception. This method strips {@code OpenSearch} prefixes from exception names.
     * This method is equivalent to the getExceptionName method in OpenSearchException.
     *
     * TODO: do we really need to make the exception name in underscore casing?
     */
    private static String getExceptionName(Throwable ex) {
        String simpleName = getExceptionSimpleClassName(ex);
        if (simpleName.startsWith("OpenSearch")) {
            simpleName = simpleName.substring("OpenSearch".length());
        }
        return toUnderscoreCase(simpleName);
    }

    /**
     * Returns the simple class name of the exception.
     * This method is equivalent to the getExceptionSimpleClassName method in OpenSearchException.
     */
    private static String getExceptionSimpleClassName(final Throwable ex) {
        String simpleName = ex.getClass().getSimpleName();
        if (simpleName.isEmpty()) {
            simpleName = "OpenSearchException";
        }
        return simpleName;
    }

    /**
     * Converts a camel case string to underscore case.
     * This method is equivalent to the toUnderscoreCase method in OpenSearchException.
     */
    private static String toUnderscoreCase(String value) {
        StringBuilder sb = new StringBuilder();
        boolean changed = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isUpperCase(c)) {
                if (!changed) {
                    // copy it over here
                    for (int j = 0; j < i; j++) {
                        sb.append(value.charAt(j));
                    }
                    changed = true;
                    if (i == 0) {
                        sb.append(Character.toLowerCase(c));
                    } else {
                        sb.append('_');
                        sb.append(Character.toLowerCase(c));
                    }
                } else {
                    sb.append('_');
                    sb.append(Character.toLowerCase(c));
                }
            } else {
                if (changed) {
                    sb.append(c);
                }
            }
        }
        if (!changed) {
            return value;
        }
        return sb.toString();
    }
}
