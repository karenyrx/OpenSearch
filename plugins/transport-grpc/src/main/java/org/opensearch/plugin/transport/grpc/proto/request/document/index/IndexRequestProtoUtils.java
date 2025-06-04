/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.transport.grpc.proto.request.document.index;

import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.support.ActiveShardCount;
import org.opensearch.plugin.transport.grpc.proto.request.common.RefreshProtoUtils;
import org.opensearch.plugin.transport.grpc.proto.request.document.bulk.ActiveShardCountProtoUtils;
import org.opensearch.protobufs.IndexDocumentRequest;
import org.opensearch.protobufs.OpType;
import org.opensearch.rest.action.document.RestIndexAction;
import org.opensearch.transport.client.node.NodeClient;

/**
 * Utility class for converting IndexDocumentRequest Protocol Buffers to IndexRequest objects.
 * This class handles the conversion of Protocol Buffer representations to their
 * corresponding OpenSearch objects.
 */
public class IndexRequestProtoUtils {

    private IndexRequestProtoUtils() {
        // Utility class, no instances
    }

    /**
     * Prepare the request for execution.
     * Similar to {@link RestIndexAction#prepareRequest(RestRequest, NodeClient)}
     *
     * @param request the request to execute
     * @return an IndexRequest ready for execution
     */
    public static IndexRequest prepareRequest(IndexDocumentRequest request) {
        IndexRequest indexRequest = new IndexRequest();

        // Set index name
        if (request.hasIndex()) {
            indexRequest.index(request.getIndex());
        }

        // Set document ID if provided
        if (request.hasId()) {
            indexRequest.id(request.getId());
        }

        // Set routing if available
        if (request.hasRouting()) {
            indexRequest.routing(request.getRouting());
        }

        // Set refresh policy
        if (request.hasRefresh()) {
            indexRequest.setRefreshPolicy(RefreshProtoUtils.getRefreshPolicy(request.getRefresh()));
        }

        // Set timeout
        if (request.hasTimeout()) {
            indexRequest.timeout(request.getTimeout());
        }

        // Set operation type (create or index)
        if (request.hasOpType()) {
            if (request.getOpType() == OpType.OP_TYPE_CREATE) {
                indexRequest.opType(IndexRequest.OpType.CREATE);
            } else {
                indexRequest.opType(IndexRequest.OpType.INDEX);
            }
        }

        // Set pipeline
        if (request.hasPipeline()) {
            indexRequest.setPipeline(request.getPipeline());
        }

        // Set require alias flag
        if (request.hasRequireAlias()) {
            indexRequest.setRequireAlias(request.getRequireAlias());
        }

        // Set version and version type
        if (request.hasVersion()) {
            indexRequest.version(request.getVersion());

            if (request.hasVersionType()) {
                switch (request.getVersionType()) {
                    case VERSION_TYPE_EXTERNAL:
                        indexRequest.versionType(org.opensearch.index.VersionType.EXTERNAL);
                        break;
                    case VERSION_TYPE_EXTERNAL_GTE:
                        indexRequest.versionType(org.opensearch.index.VersionType.EXTERNAL_GTE);
                        break;
                    default:
                        // Use default version type
                        break;
                }
            }
        }

        // Set if_seq_no and if_primary_term for optimistic concurrency control
        if (request.hasIfSeqNo()) {
            indexRequest.setIfSeqNo(request.getIfSeqNo());
        }

        if (request.hasIfPrimaryTerm()) {
            indexRequest.setIfPrimaryTerm(request.getIfPrimaryTerm());
        }

        // Set wait_for_active_shards
        if (request.hasWaitForActiveShards()) {
            ActiveShardCount activeShardCount = ActiveShardCountProtoUtils.parseProto(request.getWaitForActiveShards());
            indexRequest.waitForActiveShards(activeShardCount);
        }

        // Set source document
        if (request.hasBytesRequestBody()) {
            // Use XContentType.JSON as the default content type for bytes_request_body
            indexRequest.source(request.getBytesRequestBody().toByteArray(), org.opensearch.common.xcontent.XContentType.JSON);
        } else if (request.hasRequestBody()) {
            // Convert from ObjectMap to Map<String, Object>
            indexRequest.source(request.getRequestBody().getFieldsMap());
        }

        return indexRequest;
    }
}
