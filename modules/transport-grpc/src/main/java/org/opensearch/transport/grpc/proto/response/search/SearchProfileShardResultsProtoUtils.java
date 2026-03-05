/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.transport.grpc.proto.response.search;

import org.opensearch.search.profile.ProfileShardResult;
import org.opensearch.search.profile.SearchProfileShardResults;

import java.util.Map;
import java.util.TreeSet;

/**
 * Utility class for converting SearchProfileShardResults objects to Protocol Buffers.
 */
public class SearchProfileShardResultsProtoUtils {

    private SearchProfileShardResultsProtoUtils() {
        // Utility class, no instances
    }

    /**
     * Converts SearchProfileShardResults to its Protocol Buffer representation.
     * Similar to {@link SearchProfileShardResults#toXContent(XContentBuilder, Params)}
     *
     * @param profileResults The SearchProfileShardResults to convert
     * @return A Protocol Buffer Profile
     */
    public static org.opensearch.protobufs.Profile toProto(SearchProfileShardResults profileResults) {
        return toProto(profileResults.getShardResults());
    }

    /**
     * Converts a map of shard results to its Protocol Buffer representation.
     * Similar to {@link SearchProfileShardResults#toXContent(XContentBuilder, Params)}
     *
     * @param shardResults The map of shard ID to ProfileShardResult
     * @return A Protocol Buffer Profile
     */
    public static org.opensearch.protobufs.Profile toProto(Map<String, ProfileShardResult> shardResults) {
        org.opensearch.protobufs.Profile.Builder builder = org.opensearch.protobufs.Profile.newBuilder();

        // shardResults is a map, but we print entries in a json array, which is ordered.
        // we sort the keys of the map, so that toXContent always prints out the same array order
        TreeSet<String> sortedKeys = new TreeSet<>(shardResults.keySet());
        for (String key : sortedKeys) {
            ProfileShardResult profileShardResult = shardResults.get(key);
            builder.addShards(ProfileShardResultProtoUtils.toProto(key, profileShardResult));
        }

        return builder.build();
    }
}
