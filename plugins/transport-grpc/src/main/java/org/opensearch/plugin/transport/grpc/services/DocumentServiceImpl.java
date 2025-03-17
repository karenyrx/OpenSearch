/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.transport.grpc.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.bulk.BulkAction;
import org.opensearch.plugin.transport.grpc.listeners.BulkRequestActionListener;
import org.opensearch.protobuf.services.DocumentServiceGrpc;
import org.opensearch.transport.client.node.NodeClient;
import org.opensearch.plugin.transport.grpc.common.ExceptionHandler;
import org.opensearch.plugin.transport.grpc.proto.request.BulkRequestProtoUtils;

import io.grpc.stub.StreamObserver;

/**
 * Implementation of the gRPC Document Service.
 */
public class DocumentServiceImpl extends DocumentServiceGrpc.DocumentServiceImplBase {
    private static final Logger logger = LogManager.getLogger(DocumentServiceImpl.class);
    private final NodeClient client;

    /**
     *
     * @param client: Client for executing actions on the local node
     */
    public DocumentServiceImpl(NodeClient client) {
        this.client = client;
    }

    @Override
    public void bulk(org.opensearch.protobuf.BulkRequest request, StreamObserver<org.opensearch.protobuf.BulkResponse> responseObserver) {
        try {
            org.opensearch.action.bulk.BulkRequest bulkRequest = BulkRequestProtoUtils.prepareRequest(request);
            BulkRequestActionListener listener = new BulkRequestActionListener(responseObserver);
            client.execute(BulkAction.INSTANCE, bulkRequest, listener);
        } catch (Throwable e) {
            Throwable t = ExceptionHandler.annotateException(e);
            logger.error("DocumentServiceImpl failed to process bulk request, request=" + request + ", error=" + t.getMessage());
            responseObserver.onError(t);
        }
    }
}
