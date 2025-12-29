/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.transport.grpc.proto.response.search;

import org.opensearch.OpenSearchException;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.rest.action.search.RestSearchAction;

/**
 * Lightweight params implementation for gRPC search responses.
 * Extracts only the response parameters needed from the proto request,
 * similar to how REST uses RestRequest as ToXContent.Params.
 */
public class SearchResponseParams implements ToXContent.Params {
    private final boolean includeNamedQueriesScore;
    private final boolean totalHitsAsInt;
    private final boolean errorTrace;

    /**
     * Creates SearchResponseParams from a proto SearchRequest.
     *
     * @param protoRequest The proto request to extract response parameters from
     */
    public SearchResponseParams(org.opensearch.protobufs.SearchRequest protoRequest) {
        // Extract include_named_queries_score from SearchRequestBody
        this.includeNamedQueriesScore = protoRequest.hasSearchRequestBody()
            && protoRequest.getSearchRequestBody().hasIncludeNamedQueriesScore()
            && protoRequest.getSearchRequestBody().getIncludeNamedQueriesScore();

        // Extract total_hits_as_int from SearchRequest
        this.totalHitsAsInt = protoRequest.hasTotalHitsAsInt() && protoRequest.getTotalHitsAsInt();

        // Extract error_trace from GlobalParams (for stack trace control in errors)
        this.errorTrace = protoRequest.hasGlobalParams()
            && protoRequest.getGlobalParams().hasErrorTrace()
            && protoRequest.getGlobalParams().getErrorTrace();
    }

    @Override
    public String param(String key) {
        // Only response params are supported
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
