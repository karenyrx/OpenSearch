/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.transport.grpc.proto.response.document.bulk;

import org.opensearch.OpenSearchException;
import org.opensearch.core.xcontent.ToXContent;

/**
 * Lightweight params implementation for gRPC bulk responses.
 * Extracts only the response parameters needed from the proto request,
 * similar to how REST uses RestRequest as ToXContent.Params.
 */
public class BulkResponseParams implements ToXContent.Params {
    private final boolean errorTrace;

    /**
     * Creates BulkResponseParams from a proto BulkRequest.
     *
     * @param protoRequest The proto request to extract response parameters from
     */
    public BulkResponseParams(org.opensearch.protobufs.BulkRequest protoRequest) {
        // Extract error_trace from GlobalParams (for stack trace control in errors)
        this.errorTrace = protoRequest.hasGlobalParams()
            && protoRequest.getGlobalParams().hasErrorTrace()
            && protoRequest.getGlobalParams().getErrorTrace();
    }

    @Override
    public String param(String key) {
        if ("error_trace".equals(key)) {
            return errorTrace ? "true" : null;
        }
        if (OpenSearchException.REST_EXCEPTION_SKIP_STACK_TRACE.equals(key)) {
            // error_trace=true means include stack traces (skip=false)
            return errorTrace ? "false" : null;
        }
        return null;
    }

    @Override
    public String param(String key, String defaultValue) {
        String value = param(key);
        return value != null ? value : defaultValue;
    }

    @Override
    public boolean paramAsBoolean(String key, boolean defaultValue) {
        if ("error_trace".equals(key)) {
            return errorTrace;
        }
        if (OpenSearchException.REST_EXCEPTION_SKIP_STACK_TRACE.equals(key)) {
            // error_trace=true means include stack traces (skip=false)
            return !errorTrace;
        }
        return defaultValue;
    }

    @Override
    public Boolean paramAsBoolean(String key, Boolean defaultValue) {
        if ("error_trace".equals(key)) {
            return errorTrace;
        }
        if (OpenSearchException.REST_EXCEPTION_SKIP_STACK_TRACE.equals(key)) {
            // error_trace=true means include stack traces (skip=false)
            return !errorTrace;
        }
        return defaultValue;
    }
}
