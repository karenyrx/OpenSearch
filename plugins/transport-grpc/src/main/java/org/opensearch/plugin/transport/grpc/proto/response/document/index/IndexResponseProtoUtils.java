/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.plugin.transport.grpc.proto.response.document.index;

import org.opensearch.action.DocWriteResponse;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.plugin.transport.grpc.proto.response.document.common.ShardInfoProtoUtils;
import org.opensearch.protobufs.IndexDocumentResponseBody;
import org.opensearch.protobufs.Result;
import org.opensearch.protobufs.ShardInfo;
import org.opensearch.protobufs.ShardStatistics;

import java.io.IOException;

/**
 * Utility class for converting IndexResponse objects to Protocol Buffers.
 * This class handles the conversion of index operation responses to their
 * Protocol Buffer representation.
 */
public class IndexResponseProtoUtils {

    private IndexResponseProtoUtils() {
        // Utility class, no instances
    }

    /**
     * Converts an IndexResponse to its Protocol Buffer representation.
     * This method is equivalent to {@link IndexResponse#toXContent(XContentBuilder, ToXContent.Params)}
     *
     * @param response The IndexResponse to convert
     * @return A Protocol Buffer IndexDocumentResponse representation
     * @throws IOException if there's an error during conversion
     */
    public static org.opensearch.protobufs.IndexDocumentResponse toProto(IndexResponse response) throws IOException {
        org.opensearch.protobufs.IndexDocumentResponse.Builder indexResponse = org.opensearch.protobufs.IndexDocumentResponse.newBuilder();

        // Create the index document response body
        IndexDocumentResponseBody.Builder responseBody = IndexDocumentResponseBody.newBuilder();

        // Set fields from the response
        responseBody.setId(response.getId());
        responseBody.setIndex(response.getIndex());
        responseBody.setPrimaryTerm(response.getPrimaryTerm());
        responseBody.setSeqNo(response.getSeqNo());
        responseBody.setVersion(response.getVersion());
        responseBody.setForcedRefresh(response.forcedRefresh());

        // Set result (CREATED or UPDATED)
        if (response.getResult() == DocWriteResponse.Result.CREATED) {
            responseBody.setResult(Result.RESULT_CREATED);
        } else if (response.getResult() == DocWriteResponse.Result.UPDATED) {
            responseBody.setResult(Result.RESULT_UPDATED);
        }

        // Set shard info
        ShardInfo shardInfo = ShardInfoProtoUtils.toProto(response.getShardInfo());
        ShardStatistics shardStats = convertShardInfoToShardStatistics(shardInfo);
        responseBody.setShards(shardStats);

        // Build the final response
        indexResponse.setIndexDocumentResponseBody(responseBody.build());
        return indexResponse.build();
    }

    /**
     * Converts a ShardInfo to a ShardStatistics object.
     * This is needed because IndexDocumentResponseBody expects a ShardStatistics object,
     * but ShardInfoProtoUtils.toProto() returns a ShardInfo object.
     *
     * @param shardInfo The ShardInfo to convert
     * @return A ShardStatistics object with the same values as the ShardInfo
     */
    private static ShardStatistics convertShardInfoToShardStatistics(ShardInfo shardInfo) {
        ShardStatistics.Builder shardStats = ShardStatistics.newBuilder();
        shardStats.setTotal(shardInfo.getTotal());
        shardStats.setSuccessful(shardInfo.getSuccessful());
        shardStats.setFailed(shardInfo.getFailed());

        // Copy failures if any
        if (shardInfo.getFailuresCount() > 0) {
            shardStats.addAllFailures(shardInfo.getFailuresList());
        }

        return shardStats.build();
    }
}
