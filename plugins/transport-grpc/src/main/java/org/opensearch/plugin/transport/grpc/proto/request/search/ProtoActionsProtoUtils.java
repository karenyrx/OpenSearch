/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.transport.grpc.proto.request.search;

import org.opensearch.core.action.ShardOperationFailedException;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.QueryStringQueryBuilder;
import org.opensearch.plugin.transport.grpc.proto.response.search.ShardStatisticsProtoUtils;
import org.opensearch.protobufs.ResponseBody;
import org.opensearch.protobufs.SearchRequest;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestActions;

import java.io.IOException;

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

    /**
     * Similar to {@link RestActions#buildBroadcastShardsHeader(XContentBuilder, ToXContent.Params, int, int, int, int, ShardOperationFailedException[])}

     * @param total
     * @param successful
     * @param skipped
     * @param failed
     */
    // TODO move to response side
    public static void buildBroadcastShardsHeader(
        ResponseBody.Builder searchResponseBodyProtoBuilder,
        int total,
        int successful,
        int skipped,
        int failed,
        ShardOperationFailedException[] shardFailures
    ) throws IOException {
        searchResponseBodyProtoBuilder.setShards(
            ShardStatisticsProtoUtils.getShardStats(total, successful, skipped, failed, shardFailures)
        );

    }

}
