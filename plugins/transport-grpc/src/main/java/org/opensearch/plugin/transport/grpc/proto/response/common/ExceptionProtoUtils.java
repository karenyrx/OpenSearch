/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.plugin.transport.grpc.proto.response.common;

import com.google.protobuf.Struct;
import org.opensearch.ExceptionsHelper;
import org.opensearch.OpenSearchException;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.protobufs.ErrorCause;

import java.io.IOException;
import java.util.Map;

import static org.opensearch.OpenSearchException.getExceptionName;

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
     * This method is equivalent to the {@link OpenSearchException#toXContent(XContentBuilder, ToXContent.Params)}
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
     * It is equivalent to the {@link OpenSearchException#generateThrowableXContent(XContentBuilder, ToXContent.Params, Throwable)}
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
     * This method is equivalent to the {@link OpenSearchException#innerToXContent(XContentBuilder, ToXContent.Params, Throwable, String, String, Map, Map, Throwable)}.
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

        // TODO missing metadata for ErrorCause
        /*
        for (Map.Entry<String, List<String>> entry : metadata.entrySet()) {
            headerToXContent(builder, entry.getKey().substring(OPENSEARCH_PREFIX_KEY.length()), entry.getValue());
        }
        */

        // Add metadata if the throwable is an OpenSearchException
         if (throwable instanceof OpenSearchException) {
             OpenSearchException exception = (OpenSearchException) throwable;
            //  errorCauseBuilder.setMetadata(metadataToProto(exception));
         }

         if (cause != null) {
             errorCauseBuilder.setCausedBy(generateThrowableProto(cause));
         }

         // TODO how to set headers?
        /*
        if (headers.isEmpty() == false) {
            builder.startObject(HEADER);
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                headerToXContent(builder, entry.getKey(), entry.getValue());
            }
            builder.endObject();
        }

        */

        // Add stack trace
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
     * This method is similar to {@link OpenSearchException#metadataToXContent(XContentBuilder, ToXContent.Params)}
     * This method is override by various exception classes, such as {@link org.opensearch.core.common.breaker.CircuitBreakingException#metadataToXContent(XContentBuilder, ToXContent.Params)} or {@link org.opensearch.action.search.SearchPhaseExecutionException#metadataToXContent(XContentBuilder, ToXContent.Params)}
     * @param exception
     * @return
     */
    private static Struct metadataToProto(OpenSearchException exception) {
        Struct.Builder additionalDetailsBuilder = Struct.newBuilder();
        // TODO how do we populate additional details without adding an @Override method in exceptions under /server folder?
        return additionalDetailsBuilder.build();
    }
}
