/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.transport.grpc.proto.request.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.search.SearchType;
import org.opensearch.protobufs.SearchRequest;

public class SearchTypeProtoUtils {
    protected static Logger logger = LogManager.getLogger(SearchTypeProtoUtils.class);

    private SearchTypeProtoUtils() {
        // Utility class, no instances
    }

    /**
     * Prepare the request for execution.
     *
     * Similar to {@link SearchType#fromString(String)} ()}
     * Please ensure to keep both implementations consistent.
     *
     * @param request the request to execute
     * @return the action to execute

     */
    public static SearchType fromProto(SearchRequest request) {
        if (!request.hasSearchType()) {
            return SearchType.DEFAULT;
        }
        SearchRequest.SearchType searchType = request.getSearchType();
        switch (searchType) {
            case SEARCH_TYPE_DFS_QUERY_THEN_FETCH:
                return SearchType.DFS_QUERY_THEN_FETCH;
            case SEARCH_TYPE_QUERY_THEN_FETCH:
                return SearchType.QUERY_THEN_FETCH;
            default:
                throw new IllegalArgumentException("No search type for [" + searchType + "]");
        }
    }
}
