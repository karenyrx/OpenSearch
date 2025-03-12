/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.transport.grpc.services.document;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.transport.client.node.NodeClient;
import org.opensearch.transport.grpc.common.ExceptionHandler;
import org.opensearch.transport.grpc.services.BulkRequestHandler;

import io.grpc.stub.StreamObserver;
import org.opensearch.protobuf.BulkRequest;
import org.opensearch.protobuf.BulkResponse;
import org.opensearch.protobuf.services.DocumentServiceGrpc;

/**
 * Implementation of the gRPC Document Service.
 * This class was moved from server/src/main/java/org/opensearch/grpc/services/document/DocumentServiceImpl.java
 * to the transport-grpc module.
 */
public class DocumentServiceImpl extends DocumentServiceGrpc.DocumentServiceImplBase {
    private static final Logger logger = LogManager.getLogger(DocumentServiceImpl.class);

    BulkRequestHandler bulkRequestHandler;

    public DocumentServiceImpl(NodeClient client) {
        bulkRequestHandler = new BulkRequestHandler(client);
    }

    @Override
    public void bulk(BulkRequest request, StreamObserver<BulkResponse> responseObserver) {
        try {
            BulkResponse bulkResponse = bulkRequestHandler.executeRequest(request);
            responseObserver.onNext(bulkResponse);
            responseObserver.onCompleted();
        } catch (Throwable e) {
            Throwable t = ExceptionHandler.annotateException(e);
            logger.error("Error processing bulk request, request=" + request + ", error=" + t.getMessage());
            responseObserver.onError(t);
        }
    }
}
