/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.transport.grpc.proto.response.search;

import org.opensearch.search.profile.ProfileResult;
import org.opensearch.search.profile.fetch.FetchProfileShardResult;

import java.util.List;

/**
 * Utility class for converting FetchProfileShardResult objects to Protocol Buffers.
 */
public class FetchProfileShardResultProtoUtils {

    private FetchProfileShardResultProtoUtils() {
        // Utility class, no instances
    }

    /**
     * Converts a FetchProfileShardResult to its Protocol Buffer representation.
     * Similar to {@link FetchProfileShardResult#toXContent(XContentBuilder, Params)}
     *
     * @param fetchResult The FetchProfileShardResult to convert
     * @return A Protocol Buffer FetchProfile builder
     */
    public static org.opensearch.protobufs.FetchProfile.Builder toProto(FetchProfileShardResult fetchResult) {
        List<ProfileResult> fetchProfileResults = fetchResult.getFetchProfileResults();
        if (fetchProfileResults.isEmpty()) {
            return org.opensearch.protobufs.FetchProfile.newBuilder();
        }

        // Take the first profile result as the main fetch profile
        // If there are children, they will be added recursively
        return toFetchProfileProto(fetchProfileResults.get(0));
    }

    /**
     * Converts a ProfileResult to a FetchProfile.
     *
     * @param result The ProfileResult to convert
     * @return A Protocol Buffer FetchProfile builder
     */
    private static org.opensearch.protobufs.FetchProfile.Builder toFetchProfileProto(ProfileResult result) {
        org.opensearch.protobufs.FetchProfile.Builder builder = org.opensearch.protobufs.FetchProfile.newBuilder();

        builder.setType(result.getQueryName());
        builder.setDescription(result.getLuceneDescription());
        builder.setTimeInNanos(result.getTime());

        // Set breakdown (currently empty as FetchProfileBreakdown only has optional load_source fields)
        org.opensearch.protobufs.FetchProfileBreakdown.Builder breakdownBuilder = org.opensearch.protobufs.FetchProfileBreakdown
            .newBuilder();
        Object loadSource = result.getTimeBreakdown().get("load_source");
        if (loadSource instanceof Long) {
            breakdownBuilder.setLoadSource(((Long) loadSource).intValue());
        }
        Object loadSourceCount = result.getTimeBreakdown().get("load_source_count");
        if (loadSourceCount instanceof Long) {
            breakdownBuilder.setLoadSourceCount(((Long) loadSourceCount).intValue());
        }
        builder.setBreakdown(breakdownBuilder);

        // Set debug info if present
        if (!result.getDebugInfo().isEmpty()) {
            org.opensearch.protobufs.FetchProfileDebug.Builder debugBuilder = org.opensearch.protobufs.FetchProfileDebug.newBuilder();
            Object storedFields = result.getDebugInfo().get("stored_fields");
            if (storedFields instanceof List) {
                for (Object field : (List<?>) storedFields) {
                    if (field instanceof String) {
                        debugBuilder.addStoredFields((String) field);
                    }
                }
            }
            Object fastPath = result.getDebugInfo().get("fast_path");
            if (fastPath instanceof Integer) {
                debugBuilder.setFastPath((Integer) fastPath);
            }
            builder.setDebug(debugBuilder);
        }

        // Add children
        if (!result.getProfiledChildren().isEmpty()) {
            for (ProfileResult child : result.getProfiledChildren()) {
                builder.addChildren(toFetchProfileProto(child));
            }
        }

        return builder;
    }
}
