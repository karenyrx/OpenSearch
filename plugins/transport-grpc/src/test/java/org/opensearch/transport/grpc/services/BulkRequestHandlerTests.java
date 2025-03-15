/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.transport.grpc.services;

import org.opensearch.action.DocWriteRequest;
import org.opensearch.action.bulk.BulkItemResponse;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.support.WriteRequest;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.index.Index;
import org.opensearch.core.index.shard.ShardId;
import org.opensearch.protobuf.BulkRequest;
import org.opensearch.protobuf.BulkRequestBody;
import org.opensearch.protobuf.CreateOperation;
import org.opensearch.protobuf.DeleteOperation;
import org.opensearch.protobuf.IndexOperation;
import org.opensearch.protobuf.UpdateOperation;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.transport.client.node.NodeClient;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.protobuf.ByteString;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

public class BulkRequestHandlerTests extends OpenSearchTestCase {

    private BulkRequestHandler handler;

    @Mock
    private NodeClient client;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        handler = new BulkRequestHandler(client);
    }

    public void testPrepareRequestWithIndexOperation() throws IOException {
        // Create a Protocol Buffer BulkRequest with an index operation
        BulkRequest request = createBulkRequestWithIndexOperation();

        // Convert to OpenSearch BulkRequest
        org.opensearch.action.bulk.BulkRequest bulkRequest = handler.prepareRequest(request);

        // Verify the converted request
        assertEquals("Should have 1 request", 1, bulkRequest.numberOfActions());
        assertEquals("Should have the correct refresh policy", WriteRequest.RefreshPolicy.IMMEDIATE, bulkRequest.getRefreshPolicy());
        assertEquals("Should have the correct pipeline", "test-pipeline", bulkRequest.pipeline());

        // Verify the index request
        DocWriteRequest<?> docWriteRequest = bulkRequest.requests().get(0);
        assertEquals("Should be an INDEX operation", DocWriteRequest.OpType.INDEX, docWriteRequest.opType());
        assertEquals("Should have the correct index", "test-index", docWriteRequest.index());
        assertEquals("Should have the correct id", "test-id", docWriteRequest.id());
    }

    public void testPrepareRequestWithCreateOperation() throws IOException {
        // Create a Protocol Buffer BulkRequest with a create operation
        BulkRequest request = createBulkRequestWithCreateOperation();

        // Convert to OpenSearch BulkRequest
        org.opensearch.action.bulk.BulkRequest bulkRequest = handler.prepareRequest(request);

        // Verify the converted request
        assertEquals("Should have 1 request", 1, bulkRequest.numberOfActions());

        // Verify the create request
        DocWriteRequest<?> docWriteRequest = bulkRequest.requests().get(0);
        assertEquals("Should be a CREATE operation", DocWriteRequest.OpType.CREATE, docWriteRequest.opType());
        assertEquals("Should have the correct index", "test-index", docWriteRequest.index());
        assertEquals("Should have the correct id", "test-id", docWriteRequest.id());
    }

    public void testPrepareRequestWithDeleteOperation() throws IOException {
        // Create a Protocol Buffer BulkRequest with a delete operation
        BulkRequest request = createBulkRequestWithDeleteOperation();

        // Convert to OpenSearch BulkRequest
        org.opensearch.action.bulk.BulkRequest bulkRequest = handler.prepareRequest(request);

        // Verify the converted request
        assertEquals("Should have 1 request", 1, bulkRequest.numberOfActions());

        // Verify the delete request
        DocWriteRequest<?> docWriteRequest = bulkRequest.requests().get(0);
        assertEquals("Should have the correct index", "test-index", docWriteRequest.index());
        assertEquals("Should have the correct id", "test-id", docWriteRequest.id());
    }

    public void testPrepareRequestWithUpdateOperation() throws IOException {
        // Create a Protocol Buffer BulkRequest with an update operation
        BulkRequest request = createBulkRequestWithUpdateOperation();

        // Convert to OpenSearch BulkRequest
        org.opensearch.action.bulk.BulkRequest bulkRequest = handler.prepareRequest(request);

        // Verify the converted request
        assertEquals("Should have 1 request", 1, bulkRequest.numberOfActions());

        // Verify the update request
        DocWriteRequest<?> docWriteRequest = bulkRequest.requests().get(0);
        assertEquals("Should have the correct index", "test-index", docWriteRequest.index());
        assertEquals("Should have the correct id", "test-id", docWriteRequest.id());
    }

    public void testExecuteRequest() throws IOException {
        // Create a Protocol Buffer BulkRequest
        BulkRequest request = createBulkRequestWithIndexOperation();

        // Mock the client response
        BulkItemResponse[] responses = new BulkItemResponse[1];
        Index index = new Index("test-index", "_na_");
        ShardId shardId = new ShardId(index, 1);
        IndexResponse indexResponse = new IndexResponse(shardId, "test-id", 1, 1, 1, true);
        responses[0] = new BulkItemResponse(0, DocWriteRequest.OpType.INDEX, indexResponse);
        BulkResponse bulkResponse = new BulkResponse(responses, 100);

        // Setup the mock client to return the response
        ArgumentCaptor<org.opensearch.action.bulk.BulkRequest> requestCaptor = ArgumentCaptor.forClass(
            org.opensearch.action.bulk.BulkRequest.class
        );

        doAnswer(invocation -> {
            ActionListener<BulkResponse> listener = invocation.getArgument(1);
            listener.onResponse(bulkResponse);
            return null;
        }).when(client).bulk(requestCaptor.capture(), any());

        // Execute the request
        org.opensearch.protobuf.BulkResponse response = handler.executeRequest(request);

        // Verify the response
        assertFalse("Response should indicate no errors", response.getBulkResponseBody().getErrors());
        assertEquals("Response should have the correct took time", 100, response.getBulkResponseBody().getTook());
        assertEquals("Response should have 1 item", 1, response.getBulkResponseBody().getItemsCount());
    }

    // Helper methods to create test requests

    private BulkRequest createBulkRequestWithIndexOperation() {
        IndexOperation indexOp = IndexOperation.newBuilder()
            .setIndex("test-index")
            .setId("test-id")
            .build();

        BulkRequestBody requestBody = BulkRequestBody.newBuilder()
            .setIndex(indexOp)
            .setDoc(ByteString.copyFromUtf8("{\"field\":\"value\"}"))
            .build();

        return BulkRequest.newBuilder()
            .setPipeline("test-pipeline")
            .setRefreshValue(1) // REFRESH_TRUE = 1
            .addRequestBody(requestBody)
            .build();
    }

    private BulkRequest createBulkRequestWithCreateOperation() {
        CreateOperation createOp = CreateOperation.newBuilder()
            .setIndex("test-index")
            .setId("test-id")
            .build();

        BulkRequestBody requestBody = BulkRequestBody.newBuilder()
            .setCreate(createOp)
            .setDoc(ByteString.copyFromUtf8("{\"field\":\"value\"}"))
            .build();

        return BulkRequest.newBuilder()
            .addRequestBody(requestBody)
            .build();
    }

    private BulkRequest createBulkRequestWithDeleteOperation() {
        DeleteOperation deleteOp = DeleteOperation.newBuilder()
            .setIndex("test-index")
            .setId("test-id")
            .build();

        BulkRequestBody requestBody = BulkRequestBody.newBuilder()
            .setDelete(deleteOp)
            .build();

        return BulkRequest.newBuilder()
            .addRequestBody(requestBody)
            .build();
    }

    private BulkRequest createBulkRequestWithUpdateOperation() {
        UpdateOperation updateOp = UpdateOperation.newBuilder()
            .setIndex("test-index")
            .setId("test-id")
            .build();

        BulkRequestBody requestBody = BulkRequestBody.newBuilder()
            .setUpdate(updateOp)
            .setDoc(ByteString.copyFromUtf8("{\"field\":\"updated-value\"}"))
            .build();

        return BulkRequest.newBuilder()
            .addRequestBody(requestBody)
            .build();
    }
}
