/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.plugin.transport.grpc.proto.response.document.common;

import org.opensearch.action.support.replication.ReplicationResponse;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.plugin.transport.grpc.proto.response.common.ExceptionProtoUtils;
import org.opensearch.protobufs.*;

import java.io.IOException;

/**
 * Utility class for converting ReplicationResponse.ShardInfo objects to Protocol Buffers.
 */
public class ShardInfoProtoUtils {

    private ShardInfoProtoUtils() {
        // Utility class, no instances
    }

    /**
     * Converts a ReplicationResponse.ShardInfo Java object to a protobuf ShardStatistics.
     * Similar to {@link ReplicationResponse.ShardInfo#toXContent(XContentBuilder, ToXContent.Params)}
     */
    public static ShardInfo toProto(ReplicationResponse.ShardInfo shardInfo) throws IOException {
        // TODO double check
        ShardInfo.Builder shardInfoBuilder = ShardInfo.newBuilder();
        shardInfoBuilder.setTotal(shardInfo.getTotal());
        shardInfoBuilder.setSuccessful(shardInfo.getSuccessful());
        shardInfoBuilder.setFailed(shardInfo.getFailed());

        // Add any shard failures
        for (ReplicationResponse.ShardInfo.Failure failure : shardInfo.getFailures()) {
            shardInfoBuilder.addFailures(toProto(failure));
        }

        return shardInfoBuilder.build();
    }

    /**
     * Similar to {@link ReplicationResponse.ShardInfo.Failure#toXContent(XContentBuilder, ToXContent.Params)}
     * @return
     */
    private static ShardFailure toProto(ReplicationResponse.ShardInfo.Failure failure) throws IOException {
        // TODO double check
        ShardFailure.Builder shardFailure = ShardFailure.newBuilder();
        shardFailure.setIndex(failure.index());
        shardFailure.setShard(failure.shardId());
        shardFailure.setNode(failure.nodeId());
        shardFailure.setReason(ExceptionProtoUtils.generateThrowableProto(failure.getCause()));
        shardFailure.setStatus(failure.status().name());
//        shardFailure.setPrimary(failure.primary()); // TODO add to spec
        return shardFailure.build();
    }
}
