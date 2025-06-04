/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.transport.grpc.proto.response.document.index;

import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.support.replication.ReplicationResponse;
import org.opensearch.core.index.Index;
import org.opensearch.core.index.shard.ShardId;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.protobufs.IndexDocumentResponse;
import org.opensearch.protobufs.Result;
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;

public class IndexResponseProtoUtilsTests extends OpenSearchTestCase {

    public void testToProtoWithCreatedResponse() throws IOException {
        // Create an IndexResponse for a newly created document
        Index index = new Index("test-index", "_na_");
        ShardId shardId = new ShardId(index, 1);
        IndexResponse indexResponse = new IndexResponse(shardId, "test-id", 1, 1, 1, true);
        indexResponse.setForcedRefresh(true);

        // Set shard info with failures
        ReplicationResponse.ShardInfo shardInfo = new ReplicationResponse.ShardInfo(3, 2);
        Exception exception = new Exception("Test shard failure");
        ReplicationResponse.ShardInfo.Failure failure = new ReplicationResponse.ShardInfo.Failure(
            shardId, "node1", exception, RestStatus.INTERNAL_SERVER_ERROR, false
        );
        ReplicationResponse.ShardInfo shardInfoWithFailures = new ReplicationResponse.ShardInfo(
            3, 2, new ReplicationResponse.ShardInfo.Failure[] { failure }
        );
        indexResponse.setShardInfo(shardInfoWithFailures);

        // Convert to Protocol Buffer
        IndexDocumentResponse protoResponse = IndexResponseProtoUtils.toProto(indexResponse);

        // Verify the conversion
        assertEquals("Should have the correct index", "test-index", protoResponse.getIndexDocumentResponseBody().getIndex());
        assertEquals("Should have the correct id", "test-id", protoResponse.getIndexDocumentResponseBody().getId());
        assertEquals("Should have the correct version", 1, protoResponse.getIndexDocumentResponseBody().getVersion());
        assertEquals("Should have the correct primary term", 1, protoResponse.getIndexDocumentResponseBody().getPrimaryTerm());
        assertEquals("Should have the correct sequence number", 1, protoResponse.getIndexDocumentResponseBody().getSeqNo());
        assertTrue("Should have forced refresh", protoResponse.getIndexDocumentResponseBody().getForcedRefresh());
        assertEquals("Should have the correct result", Result.RESULT_CREATED, protoResponse.getIndexDocumentResponseBody().getResult());

        // Verify shard info
        assertEquals("Should have the correct total shards", 3, protoResponse.getIndexDocumentResponseBody().getShards().getTotal());
        assertEquals("Should have the correct successful shards", 2, protoResponse.getIndexDocumentResponseBody().getShards().getSuccessful());
        assertEquals("Should have the correct failed shards", 1, protoResponse.getIndexDocumentResponseBody().getShards().getFailed());
        assertEquals("Should have one failure", 1, protoResponse.getIndexDocumentResponseBody().getShards().getFailuresCount());
    }

    public void testToProtoWithUpdatedResponse() throws IOException {
        // Create an IndexResponse for an updated document
        Index index = new Index("test-index", "_na_");
        ShardId shardId = new ShardId(index, 1);
        IndexResponse indexResponse = new IndexResponse(shardId, "test-id", 2, 2, 2, false);

        // For an updated document, we don't need to set the result explicitly
        // The result will be determined by the IndexResponse constructor

        // Set shard info with no failures
        ReplicationResponse.ShardInfo shardInfo = new ReplicationResponse.ShardInfo(3, 3);
        indexResponse.setShardInfo(shardInfo);

        // Convert to Protocol Buffer
        IndexDocumentResponse protoResponse = IndexResponseProtoUtils.toProto(indexResponse);

        // Verify the conversion
        assertEquals("Should have the correct index", "test-index", protoResponse.getIndexDocumentResponseBody().getIndex());
        assertEquals("Should have the correct id", "test-id", protoResponse.getIndexDocumentResponseBody().getId());
        assertEquals("Should have the correct version", 2, protoResponse.getIndexDocumentResponseBody().getVersion());
        assertEquals("Should have the correct primary term", 2, protoResponse.getIndexDocumentResponseBody().getPrimaryTerm());
        assertEquals("Should have the correct sequence number", 2, protoResponse.getIndexDocumentResponseBody().getSeqNo());
        assertFalse("Should not have forced refresh", protoResponse.getIndexDocumentResponseBody().getForcedRefresh());
        assertEquals("Should have the correct result", Result.RESULT_UPDATED, protoResponse.getIndexDocumentResponseBody().getResult());

        // Verify shard info
        assertEquals("Should have the correct total shards", 3, protoResponse.getIndexDocumentResponseBody().getShards().getTotal());
        assertEquals("Should have the correct successful shards", 3, protoResponse.getIndexDocumentResponseBody().getShards().getSuccessful());
        assertEquals("Should have the correct failed shards", 0, protoResponse.getIndexDocumentResponseBody().getShards().getFailed());
        assertEquals("Should have no failures", 0, protoResponse.getIndexDocumentResponseBody().getShards().getFailuresCount());
    }

    public void testToProtoWithNoResult() throws IOException {
        // Create an IndexResponse with no explicit result (defaults to CREATED)
        Index index = new Index("test-index", "_na_");
        ShardId shardId = new ShardId(index, 1);
        IndexResponse indexResponse = new IndexResponse(shardId, "test-id", 1, 1, 1, true);
        indexResponse.setForcedRefresh(true);

        // Set shard info
        ReplicationResponse.ShardInfo shardInfo = new ReplicationResponse.ShardInfo(1, 1);
        indexResponse.setShardInfo(shardInfo);

        // Convert to Protocol Buffer
        IndexDocumentResponse protoResponse = IndexResponseProtoUtils.toProto(indexResponse);

        // Verify the conversion
        assertEquals("Should have the correct index", "test-index", protoResponse.getIndexDocumentResponseBody().getIndex());
        assertEquals("Should have the correct id", "test-id", protoResponse.getIndexDocumentResponseBody().getId());
        assertEquals("Should have the correct version", 1, protoResponse.getIndexDocumentResponseBody().getVersion());
        assertEquals("Should have the correct primary term", 1, protoResponse.getIndexDocumentResponseBody().getPrimaryTerm());
        assertEquals("Should have the correct sequence number", 1, protoResponse.getIndexDocumentResponseBody().getSeqNo());
        assertTrue("Should have forced refresh", protoResponse.getIndexDocumentResponseBody().getForcedRefresh());

        // Verify shard info
        assertEquals("Should have the correct total shards", 1, protoResponse.getIndexDocumentResponseBody().getShards().getTotal());
        assertEquals("Should have the correct successful shards", 1, protoResponse.getIndexDocumentResponseBody().getShards().getSuccessful());
        assertEquals("Should have the correct failed shards", 0, protoResponse.getIndexDocumentResponseBody().getShards().getFailed());
    }
}
