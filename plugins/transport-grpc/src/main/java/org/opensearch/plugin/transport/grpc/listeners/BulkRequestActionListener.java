/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.transport.grpc.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.grpc.stub.StreamObserver;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.core.action.ActionListener;
import org.opensearch.plugin.transport.grpc.common.ExceptionHandler;
import org.opensearch.plugin.transport.grpc.proto.response.document.bulk.BulkResponseProtoUtils;


/**
 * Listener for bulk request execution completion, handling successful and failure scenarios.
 */
public class BulkRequestActionListener implements ActionListener<BulkResponse> {
    private static final Logger logger = LogManager.getLogger(BulkRequestActionListener.class);
    private StreamObserver<org.opensearch.protobufs.BulkResponse> responseObserver;

    public BulkRequestActionListener(StreamObserver<org.opensearch.protobufs.BulkResponse> responseObserver){
        super();
        this.responseObserver = responseObserver;
    }

    @Override
    public void onResponse(org.opensearch.action.bulk.BulkResponse response) {
        // Bulk execution succeeded. Convert the opensearch internal response to protobuf
        try {
            org.opensearch.protobufs.BulkResponse protoResponse = BulkResponseProtoUtils.toProto(response);
            responseObserver.onNext(protoResponse);
            responseObserver.onCompleted();
        } catch (Exception e) {
            Throwable t = ExceptionHandler.annotateException(e);
            responseObserver.onError(t);
        }
    }

    @Override
    public void onFailure(Exception e) {
        Throwable t = ExceptionHandler.annotateException(e);
        logger.error("BulkRequestActionListener failed to process bulk request:" + t.getMessage());
        responseObserver.onError(t);
    }
}
