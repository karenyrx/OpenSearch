/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.transport.grpc.listeners;

import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.core.action.ActionListener;
import org.opensearch.plugin.transport.grpc.proto.response.document.index.IndexResponseProtoUtils;

import java.io.IOException;

/**
 * Listener for index request execution completion, handling successful and failure scenarios.
 */
public class IndexRequestActionListener implements ActionListener<IndexResponse> {
    private static final Logger logger = LogManager.getLogger(IndexRequestActionListener.class);
    private final StreamObserver<org.opensearch.protobufs.IndexDocumentResponse> responseObserver;

    /**
     * Creates a new IndexRequestActionListener.
     *
     * @param responseObserver The gRPC stream observer to send the response back to the client
     */
    public IndexRequestActionListener(StreamObserver<org.opensearch.protobufs.IndexDocumentResponse> responseObserver) {
        super();
        this.responseObserver = responseObserver;
    }

    /**
     * Handles successful index request execution.
     * Converts the OpenSearch internal response to protobuf format and sends it to the client.
     *
     * @param response The index response from OpenSearch
     */
    @Override
    public void onResponse(IndexResponse response) {
        // Index execution succeeded. Convert the opensearch internal response to protobuf
        try {
            org.opensearch.protobufs.IndexDocumentResponse protoResponse = IndexResponseProtoUtils.toProto(response);
            responseObserver.onNext(protoResponse);
            responseObserver.onCompleted();
        } catch (RuntimeException | IOException e) {
            responseObserver.onError(e);
        }
    }

    /**
     * Handles index request execution failures.
     * Converts the exception to an appropriate gRPC error and sends it to the client.
     *
     * @param e The exception that occurred during execution
     */
    @Override
    public void onFailure(Exception e) {
        logger.error("IndexRequestActionListener failed to process index request:" + e.getMessage());
        responseObserver.onError(e);
    }
}
