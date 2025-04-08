/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.transport.grpc.proto.response.search;

import org.opensearch.core.action.ShardOperationFailedException;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.protobufs.ResponseBody;
import org.opensearch.rest.action.RestActions;

import java.io.IOException;

public class ProtoActionsProtoUtils {

    private ProtoActionsProtoUtils() {
        // Utility class, no instances
    }

    /**
     * Similar to {@link RestActions#buildBroadcastShardsHeader(XContentBuilder, ToXContent.Params, int, int, int, int, ShardOperationFailedException[])}

     * @param total
     * @param successful
     * @param skipped
     * @param failed
     */
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
