/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.transport.grpc.proto.response.search;

import org.opensearch.search.profile.ProfileResult;
import org.opensearch.search.profile.query.QueryProfileShardResult;

/**
 * Utility class for converting QueryProfileShardResult objects to Protocol Buffers.
 */
public class QueryProfileShardResultProtoUtils {

    private QueryProfileShardResultProtoUtils() {
        // Utility class, no instances
    }

    /**
     * Converts a QueryProfileShardResult to its Protocol Buffer representation.
     * Similar to {@link QueryProfileShardResult#toXContent(XContentBuilder, Params)}
     *
     * @param queryProfile The QueryProfileShardResult to convert
     * @return A Protocol Buffer SearchProfile builder
     */
    public static org.opensearch.protobufs.SearchProfile.Builder toProto(QueryProfileShardResult queryProfile) {
        org.opensearch.protobufs.SearchProfile.Builder builder = org.opensearch.protobufs.SearchProfile.newBuilder();

        for (ProfileResult p : queryProfile.getQueryResults()) {
            builder.addQuery(ProfileResultProtoUtils.toQueryProfileProto(p));
        }
        builder.setRewriteTime(queryProfile.getRewriteTime());
        builder.addCollector(CollectorResultProtoUtils.toProto(queryProfile.getCollectorResult()));

        return builder;
    }
}
