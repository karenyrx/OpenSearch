/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.transport.grpc.proto.request;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.DocWriteRequest;
import org.opensearch.action.bulk.BulkShardRequest;
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.support.ActiveShardCount;
import org.opensearch.action.support.WriteRequest;
import org.opensearch.action.update.UpdateRequest;
import org.opensearch.common.lucene.uid.Versions;
import org.opensearch.core.xcontent.MediaTypeRegistry;
import org.opensearch.index.VersionType;
import org.opensearch.index.seqno.SequenceNumbers;
import org.opensearch.protobuf.*;
import org.opensearch.script.Script;
import org.opensearch.search.fetch.subphase.FetchSourceContext;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.document.RestBulkAction;
import org.opensearch.transport.client.node.NodeClient;
import org.opensearch.transport.client.Requests;

import java.io.IOException;
import java.util.List;

import static org.opensearch.index.seqno.SequenceNumbers.UNASSIGNED_PRIMARY_TERM;

/**
 * Handler for bulk requests in gRPC.
 */
public class BulkRequestProtoUtils {  // todo extend some common BaseGrpcHandler
    protected static Logger logger = LogManager.getLogger(BulkRequestProtoUtils.class);

//    private final Settings settings;

    /**
     * Prepare the request for execution.
     * <p>
     * Similar to {@link RestBulkAction#prepareRequest(RestRequest, NodeClient)} ()}
     * Please ensure to keep both implementations consistent.
     *
     * @param request the request to execute
     * @return a future of the bulk action that was     executed
     * @throws IOException if an I/O exception occurred parsing the request and preparing for execution
     */
    public static org.opensearch.action.bulk.BulkRequest prepareRequest(BulkRequest request) throws IOException {
        org.opensearch.action.bulk.BulkRequest bulkRequest = Requests.bulkRequest();

        // String defaultIndex = request.hasIndex() ? request.getIndex() : null;
        String defaultIndex = null;
        String defaultRouting = request.hasRouting() ? request.getRouting() : null;
        FetchSourceContext defaultFetchSourceContext = FetchSourceContextProtoUtils.parseFromProtoRequest(request);
        String defaultPipeline = request.hasPipeline() ? request.getPipeline() : null;

        bulkRequest = getActiveShardCount(bulkRequest, request);

        Boolean defaultRequireAlias = request.hasRequireAlias() ? request.getRequireAlias() : null;

        if (request.hasTimeout()){
            bulkRequest.timeout(request.getTimeout());
        } else {
            bulkRequest.timeout(BulkShardRequest.DEFAULT_TIMEOUT);
        };

        bulkRequest.setRefreshPolicy(getRefreshPolicy(request));

        // Note: Add batch_size parameter when backporting to OS 2.x
        /*
        if (request.hasBatchSize()){
            logger.info("The batch size option in bulk API is deprecated and will be removed in 3.0.");
        }
        bulkRequest.batchSize(request.hasBatchSize() ? request.getBatchSize() : Integer.MAX_VALUE);
        */

        bulkRequest.add(getDocWriteRequests(request, defaultIndex, defaultRouting, defaultFetchSourceContext, defaultPipeline, defaultRequireAlias));

        return bulkRequest;
    }

    private static String getRefreshPolicy(org.opensearch.protobuf.BulkRequest request) {
        if (!request.hasRefresh()){
            return null;
        }
        switch (request.getRefresh()) {
            case REFRESH_TRUE:
                return WriteRequest.RefreshPolicy.IMMEDIATE.getValue();
            case REFRESH_WAIT_FOR:
                return WriteRequest.RefreshPolicy.WAIT_UNTIL.getValue();
            case REFRESH_FALSE:
            case REFRESH_UNSPECIFIED:
            default:
                return WriteRequest.RefreshPolicy.NONE.getValue();
        }
    }

    private static org.opensearch.action.bulk.BulkRequest getActiveShardCount(org.opensearch.action.bulk.BulkRequest bulkRequest, BulkRequest request){
        if(!request.hasWaitForActiveShards()){
            return bulkRequest;
        }
        WaitForActiveShards waitForActiveShards = request.getWaitForActiveShards();
        switch (waitForActiveShards.getWaitForActiveShardsCase()){
            case WaitForActiveShards.WaitForActiveShardsCase.WAIT_FOR_ACTIVE_SHARD_OPTIONS:
                switch (waitForActiveShards.getWaitForActiveShardOptions()) {
                    case WAIT_FOR_ACTIVE_SHARD_OPTIONS_UNSPECIFIED:
                        throw new UnsupportedOperationException("No mapping for WAIT_FOR_ACTIVE_SHARD_OPTIONS_UNSPECIFIED");
                    case WAIT_FOR_ACTIVE_SHARD_OPTIONS_ALL:
                       bulkRequest.waitForActiveShards(ActiveShardCount.ALL);
                       break;
                    default:
                        bulkRequest.waitForActiveShards(ActiveShardCount.DEFAULT);
                        break;
                }
                break;
            case WaitForActiveShards.WaitForActiveShardsCase.INT32_VALUE:
                bulkRequest.waitForActiveShards(waitForActiveShards.getInt32Value());
                break;
            default:
                throw new UnsupportedOperationException("No mapping for WAIT_FOR_ACTIVE_SHARD_OPTIONS_UNSPECIFIED");
        }
        return bulkRequest;
    }

    /**
     * Similar to  BulkRequestParser#parse(), except that it takes into account global values.
     * Please ensure implementation is consistent.
     *
     * @param request
     * @return
     */
    private static DocWriteRequest<?>[] getDocWriteRequests (org.opensearch.protobuf.BulkRequest request, String defaultIndex, String defaultRouting, FetchSourceContext defaultFetchSourceContext, String defaultPipeline, Boolean defaultRequireAlias) {
        List<BulkRequestBody> bulkRequestBodyList = request.getRequestBodyList();
        DocWriteRequest<?>[] docWriteRequests = new DocWriteRequest<?>[bulkRequestBodyList.size()];

        // Process each operation in the request body
        for (int i = 0; i < bulkRequestBodyList.size(); i++) {
            BulkRequestBody bulkRequestBodyEntry = bulkRequestBodyList.get(i);
            DocWriteRequest<?> docWriteRequest;

            // Set default values, taking into account global values, similar to BulkRequest#add(BytesReference, ...., )
            String index = defaultIndex;
            String id = null;
            String routing = defaultRouting;
//            String routing = getRouting(defaultRouting);
            FetchSourceContext fetchSourceContext = defaultFetchSourceContext;
            IndexOperation.OpType opType = null;
            long version = Versions.MATCH_ANY;
            VersionType versionType = VersionType.INTERNAL;
            long ifSeqNo = SequenceNumbers.UNASSIGNED_SEQ_NO;
            long ifPrimaryTerm = UNASSIGNED_PRIMARY_TERM;
            int retryOnConflict = 0;
            String pipeline = defaultPipeline;
//            String pipeline = getPipeline(defaultPipeline);
            boolean requireAlias = defaultRequireAlias != null && defaultRequireAlias;
//            Boolean requireAlias = getRequireAlias(defaultRequireAlias);

            // Parse the operation type: create, index, update, delete, or none provided (which is invalid).
           switch (bulkRequestBodyEntry.getOperationContainerCase()) {
               case CREATE:
                   docWriteRequest = buildCreateRequest(
                       bulkRequestBodyEntry.getCreate(), bulkRequestBodyEntry.getDoc().toByteArray(),
                       index, id, routing, version, versionType, pipeline, ifSeqNo, ifPrimaryTerm, requireAlias);
                   break;
               case INDEX:
                   docWriteRequest = buildIndexRequest(
                       bulkRequestBodyEntry.getIndex(), bulkRequestBodyEntry.getDoc().toByteArray(),
                       opType, index, id, routing, version, versionType, pipeline, ifSeqNo, ifPrimaryTerm, requireAlias);
                   break;
               case UPDATE:
                   docWriteRequest = buildUpdateRequest(
                       bulkRequestBodyEntry.getUpdate(),
                       bulkRequestBodyEntry.getDoc().toByteArray(),
                       bulkRequestBodyEntry,
                       index, id, routing, fetchSourceContext, retryOnConflict, pipeline, ifSeqNo, ifPrimaryTerm, requireAlias);
                   break;
               case DELETE:
                   docWriteRequest = buildDeleteRequest(bulkRequestBodyEntry.getDelete(),
                       index, id, routing, version, versionType,ifSeqNo, ifPrimaryTerm);
                   break;
               case OPERATIONCONTAINER_NOT_SET:
               default:
                   throw new IllegalArgumentException(
                   "Invalid BulkRequestBody. An OperationContainer (create, index, update, or delete) must be provided.");
           }
            // Add the request to the bulk request
            docWriteRequests[i] = docWriteRequest;
        }
        return docWriteRequests;
    }

    private static IndexRequest buildCreateRequest(CreateOperation createOperation, byte[] document, String index, String id, String routing, long version, VersionType versionType, String pipeline, long ifSeqNo, long ifPrimaryTerm, boolean requireAlias) {
        index = createOperation.hasIndex() ? createOperation.getIndex() : index;
        id = createOperation.hasId() ? createOperation.getId() : id;
        routing = createOperation.hasRouting() ? createOperation.getRouting() : routing;
        version = createOperation.hasVersion() ? createOperation.getVersion() : version;
        if(createOperation.hasVersionType()) {
            switch (createOperation.getVersionType()) {
                case VERSION_TYPE_EXTERNAL:
                    versionType = VersionType.EXTERNAL;
                    break;
                case VERSION_TYPE_EXTERNAL_GTE:
                    versionType = VersionType.EXTERNAL_GTE;
                    break;
                default:
                    versionType = VersionType.INTERNAL;
                    break;
            }
        }
        pipeline = createOperation.hasPipeline() ? createOperation.getPipeline() : pipeline;
        ifSeqNo = createOperation.hasIfSeqNo() ? createOperation.getIfSeqNo() : ifSeqNo;
        ifPrimaryTerm = createOperation.hasIfPrimaryTerm() ? createOperation.getIfPrimaryTerm() : ifPrimaryTerm;
        requireAlias = createOperation.hasRequireAlias() ? createOperation.getRequireAlias() : requireAlias;

        IndexRequest indexRequest = new IndexRequest(index).id(id)
            .routing(routing)
            .version(version)
            .versionType(versionType)
            .create(true)
            .setPipeline(pipeline)
            .setIfSeqNo(ifSeqNo)
            .setIfPrimaryTerm(ifPrimaryTerm)
            .source(document, MediaTypeRegistry.JSON)
            .setRequireAlias(requireAlias);
        return indexRequest;
    }

    private static IndexRequest buildIndexRequest(IndexOperation indexOperation, byte[] document, IndexOperation.OpType opType, String index, String id, String routing, long version, VersionType versionType, String pipeline, long ifSeqNo, long ifPrimaryTerm, boolean requireAlias) {
        opType = indexOperation.hasOpType()? indexOperation.getOpType() : opType;
        index = indexOperation.hasIndex() ? indexOperation.getIndex() : index;
        id = indexOperation.hasId() ? indexOperation.getId() : id;
        routing = indexOperation.hasRouting() ? indexOperation.getRouting() : routing;
        version = indexOperation.hasVersion() ? indexOperation.getVersion() : version;
        if(indexOperation.hasVersionType()) {
            switch (indexOperation.getVersionType()) {
                case VERSION_TYPE_EXTERNAL:
                    versionType = VersionType.EXTERNAL;
                    break;
                case VERSION_TYPE_EXTERNAL_GTE:
                    versionType = VersionType.EXTERNAL_GTE;
                    break;
                default:
                    versionType = VersionType.INTERNAL;
                    break;
            }
        }
        pipeline = indexOperation.hasPipeline() ? indexOperation.getPipeline() : pipeline;
        ifSeqNo = indexOperation.hasIfSeqNo() ? indexOperation.getIfSeqNo() : ifSeqNo;
        ifPrimaryTerm = indexOperation.hasIfPrimaryTerm() ? indexOperation.getIfPrimaryTerm() : ifPrimaryTerm;
        requireAlias = indexOperation.hasRequireAlias() ? indexOperation.getRequireAlias() : requireAlias;

        IndexRequest indexRequest;
        if (opType == null) {
            indexRequest = new IndexRequest(index).id(id)
                .routing(routing)
                .version(version)
                .versionType(versionType)
                .setPipeline(pipeline)
                .setIfSeqNo(ifSeqNo)
                .setIfPrimaryTerm(ifPrimaryTerm)
                .source(document, MediaTypeRegistry.JSON)
                .setRequireAlias(requireAlias);
        } else {
            indexRequest = new IndexRequest(index).id(id)
                .routing(routing)
                .version(version)
                .versionType(versionType)
                .create(opType.equals(IndexOperation.OpType.OP_TYPE_CREATE))
                .setPipeline(pipeline)
                .setIfSeqNo(ifSeqNo)
                .setIfPrimaryTerm(ifPrimaryTerm)
                .source(document, MediaTypeRegistry.JSON)
                .setRequireAlias(requireAlias);
        }
        return indexRequest;
    }

    private static UpdateRequest buildUpdateRequest(UpdateOperation updateOperation, byte[] document, BulkRequestBody bulkRequestBody, String index, String id, String routing, FetchSourceContext fetchSourceContext, int retryOnConflict, String pipeline, long ifSeqNo, long ifPrimaryTerm, boolean requireAlias) {
        index = updateOperation.hasIndex() ? updateOperation.getIndex() : index;
        id = updateOperation.hasId() ? updateOperation.getId() : id;
        routing = updateOperation.hasRouting() ? updateOperation.getRouting() : routing;
        fetchSourceContext = bulkRequestBody.hasSource() ? FetchSourceContextProtoUtils.fromProto(
                bulkRequestBody.getSource()) : fetchSourceContext;
        retryOnConflict = updateOperation.hasRetryOnConflict() ? updateOperation.getRetryOnConflict() : retryOnConflict;
        ifSeqNo = updateOperation.hasIfSeqNo() ? updateOperation.getIfSeqNo() : ifSeqNo;
        ifPrimaryTerm = updateOperation.hasIfPrimaryTerm() ? updateOperation.getIfPrimaryTerm() : ifPrimaryTerm;
        requireAlias = updateOperation.hasRequireAlias() ? updateOperation.getRequireAlias() : requireAlias;

        UpdateRequest updateRequest = new UpdateRequest().index(index)
            .id(id)
            .routing(routing)
            .retryOnConflict(retryOnConflict)
            .setIfSeqNo(ifSeqNo)
            .setIfPrimaryTerm(ifPrimaryTerm)
            .setRequireAlias(requireAlias)
            .routing(routing);

        updateRequest = fromProto(updateRequest, document, bulkRequestBody, updateOperation);

        if (fetchSourceContext != null) {
            updateRequest.fetchSource(fetchSourceContext);
        }
        // TODO: test how is upsertRequest used?
//        IndexRequest upsertRequest = updateRequest.upsertRequest();
//        if (upsertRequest != null) {
//            upsertRequest.setPipeline(pipeline);
//        }

        return updateRequest;
    }

    /** Similar to UpdateRequest#fromXContent(); **/
    private static UpdateRequest fromProto(UpdateRequest updateRequest, byte[] document, BulkRequestBody bulkRequestBody, UpdateOperation updateOperation) {
        // TODO compare with REST

        if (bulkRequestBody.hasScript()) {
            Script script = ScriptProtoUtils.parseFromProtoRequest(bulkRequestBody.getScript());
            updateRequest.script(script);
        }

        if (bulkRequestBody.hasScriptedUpsert()) {
            updateRequest.scriptedUpsert(bulkRequestBody.getScriptedUpsert());
        }

        if (bulkRequestBody.hasUpsert()) {
            updateRequest.upsert(bulkRequestBody.getUpsert(), MediaTypeRegistry.JSON);
        }

        updateRequest.doc(document, MediaTypeRegistry.JSON);

        if (bulkRequestBody.hasDocAsUpsert()) {
            updateRequest.docAsUpsert(bulkRequestBody.getDocAsUpsert());
        }

        if (bulkRequestBody.hasDetectNoop()) {
            updateRequest.detectNoop(bulkRequestBody.getDetectNoop());
        }

        if (bulkRequestBody.hasDocAsUpsert()) {
            updateRequest.docAsUpsert(bulkRequestBody.getDocAsUpsert());
        }

        if (bulkRequestBody.hasSource()) {
            updateRequest.fetchSource(FetchSourceContextProtoUtils.fromProto(bulkRequestBody.getSource()));
        }

        if (updateOperation.hasIfSeqNo()) {
            updateRequest.setIfSeqNo(updateOperation.getIfSeqNo());
        }

        if (updateOperation.hasIfPrimaryTerm()) {
            updateRequest.setIfPrimaryTerm(updateOperation.getIfPrimaryTerm());
        }

        return updateRequest;
    }


    private static DeleteRequest buildDeleteRequest(DeleteOperation deleteOperation, String index, String id, String routing, long version, VersionType versionType, long ifSeqNo, long ifPrimaryTerm) {
        index = deleteOperation.hasIndex() ? deleteOperation.getIndex() : index;
        id = deleteOperation.hasId() ? deleteOperation.getId() : id;
        routing = deleteOperation.hasRouting() ? deleteOperation.getRouting() : routing;
        version = deleteOperation.hasVersion() ? deleteOperation.getVersion() : version;
        if(deleteOperation.hasVersionType()) {
            switch (deleteOperation.getVersionType()) {
                case VERSION_TYPE_EXTERNAL:
                    versionType = VersionType.EXTERNAL;
                    break;
                case VERSION_TYPE_EXTERNAL_GTE:
                    versionType = VersionType.EXTERNAL_GTE;
                    break;
                default:
                    versionType = VersionType.INTERNAL;
                    break;
            }
        }
        ifSeqNo = deleteOperation.hasIfSeqNo() ? deleteOperation.getIfSeqNo() : ifSeqNo;
        ifPrimaryTerm = deleteOperation.hasIfPrimaryTerm() ? deleteOperation.getIfPrimaryTerm() : ifPrimaryTerm;

        DeleteRequest deleteRequest = new DeleteRequest(index).id(id)
            .routing(routing)
            .version(version)
            .versionType(versionType)
            .setIfSeqNo(ifSeqNo)
            .setIfPrimaryTerm(ifPrimaryTerm);

        return deleteRequest;
    }
}
