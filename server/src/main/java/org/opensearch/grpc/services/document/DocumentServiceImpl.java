/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.grpc.services.document;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.grpc.common.ExceptionHandler;
import org.opensearch.grpc.handlers.document.BulkRequestHandler;
import org.opensearch.transport.client.node.NodeClient;

import io.grpc.stub.StreamObserver;
import opensearch.proto.BulkRequest;
import opensearch.proto.BulkResponse;
import opensearch.proto.services.DocumentServiceGrpc;

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
