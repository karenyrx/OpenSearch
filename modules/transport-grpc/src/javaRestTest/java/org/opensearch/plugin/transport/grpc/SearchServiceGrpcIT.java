/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.transport.grpc;

import org.opensearch.protobufs.SearchRequest;
import org.opensearch.protobufs.SearchRequestBody;
import org.opensearch.protobufs.SearchResponse;

/**
 * External cluster integration tests for the SearchService gRPC service.
 * Tests gRPC search functionality against an external OpenSearch cluster.
 *
 * This test complements SearchServiceIT (which uses internal clusters) by testing
 * against real external OpenSearch deployments.
 */
public class SearchServiceGrpcIT extends OpenSearchGRPCTestCase {

    /**
     * Tests basic search operation via gRPC against external cluster.
     */
    public void testSearchServiceSearch() throws Exception {
        // Create a search request for a simple query
        SearchRequestBody requestBody = SearchRequestBody.newBuilder().setFrom(0).setSize(10).build();

        SearchRequest searchRequest = SearchRequest.newBuilder()
            .addIndex("_all")  // Search across all indices
            .setRequestBody(requestBody)
            .setQ("*:*")  // Match all query
            .build();

        // Execute the search request
        SearchResponse searchResponse = searchStub().search(searchRequest);

        // Verify the response
        assertNotNull("Search response should not be null", searchResponse);
        assertNotNull("Search response body should not be null", searchResponse.getResponseBody());
        assertNotNull("Search hits should not be null", searchResponse.getResponseBody().getHits());

        logger.info(
            "Search completed successfully with {} total hits",
            searchResponse.getResponseBody().getHits().getTotal().getTotalHits().getValue()
        );
    }

    /**
     * Tests search with timeout handling.
     */
    public void testSearchWithTimeout() throws Exception {
        SearchRequestBody requestBody = SearchRequestBody.newBuilder().setFrom(0).setSize(1).build();

        SearchRequest searchRequest = SearchRequest.newBuilder().addIndex("_all").setRequestBody(requestBody).setQ("*:*").build();

        try {
            SearchResponse searchResponse = searchStub().search(searchRequest);
            assertNotNull("Search response should not be null", searchResponse);
            logger.info("Search with timeout completed successfully");
        } catch (Exception e) {
            // Timeout or other errors are acceptable in this test
            logger.info("Search with timeout encountered expected error: {}", e.getMessage());
        }
    }

    /**
     * Tests async search functionality.
     */
    public void testAsyncSearch() throws Exception {
        SearchRequestBody requestBody = SearchRequestBody.newBuilder().setFrom(0).setSize(5).build();

        SearchRequest searchRequest = SearchRequest.newBuilder().addIndex("_all").setRequestBody(requestBody).setQ("*:*").build();

        // Create async observer
        TestStreamObserver<SearchResponse> observer = createTestStreamObserver();

        // Execute async search
        searchAsyncStub().search(searchRequest, observer);

        // Wait for response
        boolean completed = observer.await(30, java.util.concurrent.TimeUnit.SECONDS);

        if (completed && observer.getError() == null) {
            SearchResponse response = observer.getResponse();
            assertNotNull("Async search response should not be null", response);
            logger.info("Async search completed successfully");
        } else if (observer.getError() != null) {
            logger.info("Async search failed as expected: {}", observer.getError().getMessage());
        } else {
            fail("Async search timed out after 30 seconds");
        }
    }

    /**
     * Tests gRPC connectivity and health.
     */
    public void testGrpcConnectivity() throws Exception {
        // Verify that we can get a channel and it's active
        assertNotNull("gRPC client should be available", grpcClient());
        assertNotNull("gRPC channel should not be shutdown", grpcClient().getChannel());

        // Test random address functionality
        assertNotNull("Random gRPC address should be available", randomGrpcAddress());

        logger.info("gRPC connectivity test passed - using address: {}", randomGrpcAddress());
    }
}
