/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.transport.grpc.proto.request;

import org.opensearch.OpenSearchException;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.protobufs.BulkRequest;
import org.opensearch.protobufs.SearchRequest;
import org.opensearch.rest.action.search.RestSearchAction;

/**
 * gRPC Request wrapper that implements ToXContent.Params.
 * This class extracts response parameters from proto requests, similar to how
 * REST uses RestRequest as ToXContent.Params.
 *
 * @opensearch.internal
 */
public class GrpcRequest implements ToXContent.Params {
    private final Object protoRequest;
    private final boolean includeNamedQueriesScore;
    private final boolean totalHitsAsInt;
    private final boolean errorTrace;

    /**
     * Creates GrpcRequest from a proto request.
     *
     * @param protoRequest The proto request (SearchRequest, BulkRequest, etc.)
     */
    public GrpcRequest(Object protoRequest) {
        this.protoRequest = protoRequest;

        // Extract error_trace from GlobalParams (common to all request types)
        boolean errorTraceValue = false;
        if (protoRequest instanceof SearchRequest searchRequest) {
            errorTraceValue = searchRequest.hasGlobalParams()
                && searchRequest.getGlobalParams().hasErrorTrace()
                && searchRequest.getGlobalParams().getErrorTrace();

            // Extract search-specific parameters
            this.includeNamedQueriesScore = searchRequest.hasSearchRequestBody()
                && searchRequest.getSearchRequestBody().hasIncludeNamedQueriesScore()
                && searchRequest.getSearchRequestBody().getIncludeNamedQueriesScore();
            this.totalHitsAsInt = searchRequest.hasTotalHitsAsInt() && searchRequest.getTotalHitsAsInt();
        } else if (protoRequest instanceof BulkRequest bulkRequest) {
            errorTraceValue = bulkRequest.hasGlobalParams()
                && bulkRequest.getGlobalParams().hasErrorTrace()
                && bulkRequest.getGlobalParams().getErrorTrace();

            // Bulk requests don't have search-specific parameters
            this.includeNamedQueriesScore = false;
            this.totalHitsAsInt = false;
        } else {
            // Unknown request type - no parameters extracted
            this.includeNamedQueriesScore = false;
            this.totalHitsAsInt = false;
        }

        this.errorTrace = errorTraceValue;
    }

    @Override
    public String param(String key) {
        if (RestSearchAction.INCLUDE_NAMED_QUERIES_SCORE_PARAM.equals(key)) {
            return includeNamedQueriesScore ? "true" : null;
        }
        if (RestSearchAction.TOTAL_HITS_AS_INT_PARAM.equals(key)) {
            return totalHitsAsInt ? "true" : null;
        }
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
        if (RestSearchAction.INCLUDE_NAMED_QUERIES_SCORE_PARAM.equals(key)) {
            return includeNamedQueriesScore;
        }
        if (RestSearchAction.TOTAL_HITS_AS_INT_PARAM.equals(key)) {
            return totalHitsAsInt;
        }
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
        if (RestSearchAction.INCLUDE_NAMED_QUERIES_SCORE_PARAM.equals(key)) {
            return includeNamedQueriesScore;
        }
        if (RestSearchAction.TOTAL_HITS_AS_INT_PARAM.equals(key)) {
            return totalHitsAsInt;
        }
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
