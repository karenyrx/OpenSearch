/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.transport.grpc.proto.response.search;

import org.opensearch.search.profile.ProfileShardResult;
import org.opensearch.search.profile.query.QueryProfileShardResult;

/**
 * Utility class for converting ProfileShardResult objects to Protocol Buffers.
 */
public class ProfileShardResultProtoUtils {

    private ProfileShardResultProtoUtils() {
        // Utility class, no instances
    }

    /**
     * Converts a ProfileShardResult to its Protocol Buffer representation.
     * Similar to the shard result conversion in {@link org.opensearch.search.profile.SearchProfileShardResults#toXContent}
     *
     * @param shardId The shard ID string
     * @param shardResult The ProfileShardResult to convert
     * @return A Protocol Buffer ShardProfile builder
     */
    public static org.opensearch.protobufs.ShardProfile.Builder toProto(String shardId, ProfileShardResult shardResult) {
        org.opensearch.protobufs.ShardProfile.Builder builder = org.opensearch.protobufs.ShardProfile.newBuilder();

        builder.setId(shardId);

        for (QueryProfileShardResult result : shardResult.getQueryProfileResults()) {
            builder.addSearches(QueryProfileShardResultProtoUtils.toProto(result));
        }

        // Check for aggregation profile results - throw exception if present as they're not supported yet
        if (shardResult.getAggregationProfileResults() != null
            && !shardResult.getAggregationProfileResults().getProfileResults().isEmpty()) {
            throw new UnsupportedOperationException("aggregation profile results are not supported in gRPC yet");
        }

        // Add fetch profile results if present
        if (shardResult.getFetchProfileResult() != null && !shardResult.getFetchProfileResult().getFetchProfileResults().isEmpty()) {
            builder.setFetch(FetchProfileShardResultProtoUtils.toProto(shardResult.getFetchProfileResult()));
        }

        return builder;
    }
}
