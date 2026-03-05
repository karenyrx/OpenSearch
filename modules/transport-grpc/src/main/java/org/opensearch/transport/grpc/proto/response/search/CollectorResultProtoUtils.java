/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.transport.grpc.proto.response.search;

import org.opensearch.search.profile.query.CollectorResult;

/**
 * Utility class for converting CollectorResult objects to Protocol Buffers.
 */
public class CollectorResultProtoUtils {

    private CollectorResultProtoUtils() {
        // Utility class, no instances
    }

    /**
     * Converts a CollectorResult to its Protocol Buffer representation.
     * Similar to {@link CollectorResult#toXContent(XContentBuilder, ToXContent.Params)}
     *
     * @param collector The CollectorResult to convert
     * @return A Protocol Buffer Collector builder
     */
    public static org.opensearch.protobufs.Collector.Builder toProto(CollectorResult collector) {
        org.opensearch.protobufs.Collector.Builder builder = org.opensearch.protobufs.Collector.newBuilder();
        builder.setName(collector.getName());
        builder.setReason(collector.getReason());
        builder.setTimeInNanos(collector.getTime());

        if (!collector.getProfiledChildren().isEmpty()) {
            for (CollectorResult child : collector.getProfiledChildren()) {
                builder.addChildren(toProto(child));
            }
        }

        return builder;
    }
}
