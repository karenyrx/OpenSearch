/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.transport.grpc.services.document;

import org.opensearch.plugin.transport.grpc.services.DocumentServiceImpl;
import org.opensearch.protobuf.BulkRequest;
import org.opensearch.protobuf.BulkRequestBody;
import org.opensearch.protobuf.IndexOperation;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.transport.client.node.NodeClient;
import org.opensearch.plugin.transport.grpc.proto.request.BulkRequestProtoUtils;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.protobuf.ByteString;

import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DocumentServiceImplTests extends OpenSearchTestCase {

    private DocumentServiceImpl service;

    @Mock
    private NodeClient client;

    @Mock
    private BulkRequestProtoUtils bulkRequestProtoUtils;

    @Mock
    private StreamObserver<org.opensearch.protobuf.BulkResponse> responseObserver;

    @Before
    public void setup() throws IOException {
        MockitoAnnotations.openMocks(this);
        service = new DocumentServiceImpl(client);
        service.bulkRequestProtoUtils = bulkRequestProtoUtils;
    }

    public void testBulkSuccess() throws IOException {
        // Create a test request
        BulkRequest request = createTestBulkRequest();

        // Create a test response
        org.opensearch.protobuf.BulkResponse response = org.opensearch.protobuf.BulkResponse.newBuilder()
            .setBulkResponseBody(
                org.opensearch.protobuf.BulkResponseBody.newBuilder()
                    .setTook(100)
                    .setErrors(false)
                    .build()
            )
            .build();

        // Mock the handler to return the test response
        when(bulkRequestProtoUtils.executeRequest(request)).thenReturn(response);

        // Call the bulk method
        service.bulk(request, responseObserver);

        // Verify that the response was sent
        verify(responseObserver).onNext(response);
        verify(responseObserver).onCompleted();
    }

    public void testBulkError() throws IOException {
        // Create a test request
        BulkRequest request = createTestBulkRequest();

        // Mock the handler to throw an exception
        IOException exception = new IOException("Test exception");
        when(bulkRequestProtoUtils.executeRequest(request)).thenThrow(exception);

        // Call the bulk method
        service.bulk(request, responseObserver);

        // Verify that the error was sent
        verify(responseObserver).onError(any(StatusRuntimeException.class));
    }

    private BulkRequest createTestBulkRequest() {
        IndexOperation indexOp = IndexOperation.newBuilder()
            .setIndex("test-index")
            .setId("test-id")
            .build();

        BulkRequestBody requestBody = BulkRequestBody.newBuilder()
            .setIndex(indexOp)
            .setDoc(ByteString.copyFromUtf8("{\"field\":\"value\"}"))
            .build();

        return BulkRequest.newBuilder()
            .addRequestBody(requestBody)
            .build();
    }
}
