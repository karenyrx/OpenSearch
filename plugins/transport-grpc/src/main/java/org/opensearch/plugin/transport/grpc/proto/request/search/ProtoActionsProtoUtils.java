/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.transport.grpc.proto.request.search;

import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.QueryStringQueryBuilder;
import org.opensearch.protobufs.SearchRequest;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestActions;

public class ProtoActionsProtoUtils {

    private ProtoActionsProtoUtils() {
        // Utility class, no instances
    }

    /**
     * Similar to {@link RestActions#urlParamsToQueryBuilder(RestRequest)}
     *
     * @param request
     * @return
     */
    public static QueryBuilder urlParamsToQueryBuilder(SearchRequest request) {
        if (!request.hasQ()) {
            return null;
        }

        QueryStringQueryBuilder queryBuilder = QueryBuilders.queryStringQuery(request.getQ());
        queryBuilder.defaultField(request.hasDf() ? request.getDf() : null); // TODO what should default value be?
        queryBuilder.analyzer(request.hasAnalyzer() ? request.getAnalyzer() : null);  // TODO what should default value be?
        queryBuilder.analyzeWildcard(request.hasAnalyzeWildcard() ? request.getAnalyzeWildcard() : false);
        queryBuilder.lenient(request.hasLenient() ? request.getLenient() : null);
        if (request.hasDefaultOperator()) {
            queryBuilder.defaultOperator(OperatorProtoUtils.fromEnum(request.getDefaultOperator()));
        }
        return queryBuilder;
    }
}
