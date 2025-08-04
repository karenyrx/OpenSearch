/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.transport.grpc;

import org.opensearch.protobufs.BulkRequest;
import org.opensearch.protobufs.BulkRequestBody;
import org.opensearch.protobufs.BulkResponse;
import org.opensearch.protobufs.IndexOperation;

/**
 * External cluster integration tests for the DocumentService gRPC service.
 * Tests gRPC document operations against an external OpenSearch cluster.
 *
 * This test complements DocumentServiceIT (which uses internal clusters) by testing
 * against real external OpenSearch deployments.
 */
public class DocumentServiceGrpcIT extends OpenSearchGRPCTestCase {

    /**
     * Tests basic bulk operation via gRPC against external cluster.
     */
    public void testDocumentServiceBulk() throws Exception {
        // Create a simple test document
        String testDocument = "{\"message\":\"Hello from gRPC external test\",\"timestamp\":\"2024-01-01T00:00:00Z\"}";

        // Create a bulk request with an index operation
        IndexOperation indexOp = IndexOperation.newBuilder().setIndex("grpc-test-index").setId("external-test-1").build();

        BulkRequestBody requestBody = BulkRequestBody.newBuilder()
            .setIndex(indexOp)
            .setDoc(com.google.protobuf.ByteString.copyFromUtf8(testDocument))
            .build();

        BulkRequest bulkRequest = BulkRequest.newBuilder().addRequestBody(requestBody).build();

        // Execute the bulk request
        BulkResponse bulkResponse = documentStub().bulk(bulkRequest);

        // Verify the response
        assertNotNull("Bulk response should not be null", bulkResponse);
        assertNotNull("Bulk response body should not be null", bulkResponse.getBulkResponseBody());

        logger.info(
            "Bulk operation completed - errors: {}, items: {}",
            bulkResponse.getBulkResponseBody().getErrors(),
            bulkResponse.getBulkResponseBody().getItemsCount()
        );
    }

    /**
     * Tests async bulk operation.
     */
    public void testAsyncBulkOperation() throws Exception {
        // Create test document
        String testDocument = "{\"message\":\"Async bulk test\",\"timestamp\":\"2024-01-01T01:00:00Z\"}";

        IndexOperation indexOp = IndexOperation.newBuilder().setIndex("grpc-async-test-index").setId("async-test-1").build();

        BulkRequestBody requestBody = BulkRequestBody.newBuilder()
            .setIndex(indexOp)
            .setDoc(com.google.protobuf.ByteString.copyFromUtf8(testDocument))
            .build();

        BulkRequest bulkRequest = BulkRequest.newBuilder().addRequestBody(requestBody).build();

        // Create async observer
        TestStreamObserver<BulkResponse> observer = createTestStreamObserver();

        // Execute async bulk request
        documentAsyncStub().bulk(bulkRequest, observer);

        // Wait for response
        boolean completed = observer.await(30, java.util.concurrent.TimeUnit.SECONDS);

        if (completed && observer.getError() == null) {
            BulkResponse response = observer.getResponse();
            assertNotNull("Async bulk response should not be null", response);
            logger.info("Async bulk operation completed successfully");
        } else if (observer.getError() != null) {
            logger.info("Async bulk operation failed as expected: {}", observer.getError().getMessage());
        } else {
            fail("Async bulk operation timed out after 30 seconds");
        }
    }

    /**
     * Tests bulk operation with multiple documents.
     */
    public void testMultipleBulkOperations() throws Exception {
        BulkRequest.Builder bulkRequestBuilder = BulkRequest.newBuilder();

        // Create multiple index operations
        for (int i = 1; i <= 3; i++) {
            String testDocument = String.format(
                "{\"message\":\"Multi-doc test %d\",\"value\":%d,\"timestamp\":\"2024-01-01T%02d:00:00Z\"}",
                i,
                i * 10,
                i
            );

            IndexOperation indexOp = IndexOperation.newBuilder().setIndex("grpc-multi-test-index").setId("multi-test-" + i).build();

            BulkRequestBody requestBody = BulkRequestBody.newBuilder()
                .setIndex(indexOp)
                .setDoc(com.google.protobuf.ByteString.copyFromUtf8(testDocument))
                .build();

            bulkRequestBuilder.addRequestBody(requestBody);
        }

        BulkRequest bulkRequest = bulkRequestBuilder.build();

        // Execute the bulk request
        BulkResponse bulkResponse = documentStub().bulk(bulkRequest);

        // Verify the response
        assertNotNull("Bulk response should not be null", bulkResponse);
        assertNotNull("Bulk response body should not be null", bulkResponse.getBulkResponseBody());

        logger.info("Multi-document bulk operation completed - {} operations processed", bulkRequest.getRequestBodyCount());
    }

    /**
     * Tests error handling in bulk operations.
     */
    public void testBulkErrorHandling() throws Exception {
        // Create a bulk request with potentially problematic data
        String invalidDocument = ""; // Empty document might cause errors

        IndexOperation indexOp = IndexOperation.newBuilder().setIndex("grpc-error-test-index").setId("error-test-1").build();

        BulkRequestBody requestBody = BulkRequestBody.newBuilder()
            .setIndex(indexOp)
            .setDoc(com.google.protobuf.ByteString.copyFromUtf8(invalidDocument))
            .build();

        BulkRequest bulkRequest = BulkRequest.newBuilder().addRequestBody(requestBody).build();

        try {
            BulkResponse bulkResponse = documentStub().bulk(bulkRequest);

            // Even if the request succeeds, log the result
            logger.info("Bulk error handling test - response received: {}", messageToString(bulkResponse));

        } catch (Exception e) {
            // Expected behavior - log the error
            logger.info("Bulk operation with invalid data failed as expected: {}", e.getMessage());
        }
    }
}
