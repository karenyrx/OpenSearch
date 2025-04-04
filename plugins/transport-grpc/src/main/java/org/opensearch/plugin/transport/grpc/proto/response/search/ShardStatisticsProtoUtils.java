/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.plugin.transport.grpc.proto.response.search;

import org.opensearch.ExceptionsHelper;
import org.opensearch.action.admin.indices.stats.ShardStats;
import org.opensearch.core.action.ShardOperationFailedException;
import org.opensearch.core.common.util.CollectionUtils;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.plugin.transport.grpc.proto.response.exceptions.shardoperationfailedexception.ShardOperationFailedExceptionProtoUtils;
import org.opensearch.protobufs.ShardStatistics;

import java.io.IOException;

/**
 * Utility class for converting SearchResponse objects to Protocol Buffers.
 * This class handles the conversion of search operation responses to their
 * Protocol Buffer representation.
 */
public class ShardStatisticsProtoUtils {

    private ShardStatisticsProtoUtils() {
        // Utility class, no instances
    }

    /**
     * Converts a ShardStatistics to its Protocol Buffer representation.
     * This method is equivalent to {@link ShardStats#toXContent(XContentBuilder, ToXContent.Params)}
     *
     * @param total The SearchResponse to convert
     * @return A Protocol Buffer SearchResponse representation
     */
    public static ShardStatistics getShardStats(
        int total,
        int successful,
        int skipped,
        int failed,
        ShardOperationFailedException[] shardFailures
    ) throws IOException {
        ShardStatistics.Builder shardStats = ShardStatistics.newBuilder();
        shardStats.setTotal(total);
        shardStats.setSuccessful(successful);
        if (skipped >= 0) {
            shardStats.setSkipped(skipped);
        }
        shardStats.setFailed(failed);
        if (CollectionUtils.isEmpty(shardFailures) == false) {
            for (ShardOperationFailedException shardFailure : ExceptionsHelper.groupBy(shardFailures)) {
                shardStats.addFailures(ShardOperationFailedExceptionProtoUtils.toProto(shardFailure));
            }
        }
        return shardStats.build();
    }

}
