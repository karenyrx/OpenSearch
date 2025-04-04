/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.plugin.transport.grpc.proto.response.search;

import com.google.protobuf.ByteString;
import org.opensearch.common.document.DocumentField;
import org.opensearch.core.common.bytes.BytesReference;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.index.mapper.IgnoredFieldMapper;
import org.opensearch.index.seqno.SequenceNumbers;
import org.opensearch.protobufs.NullValue;
import org.opensearch.search.SearchHit;
import org.opensearch.transport.RemoteClusterAware;

import java.io.IOException;

/**
 * Utility class for converting SearchResponse objects to Protocol Buffers.
 * This class handles the conversion of search operation responses to their
 * Protocol Buffer representation.
 */
public class SearchHitProtoUtils {

    private SearchHitProtoUtils() {
        // Utility class, no instances
    }

    /**
     * Converts a SearchHit to its Protocol Buffer representation.
     * This method is equivalent to {@link SearchHit#toXContent(XContentBuilder, ToXContent.Params)}
     *
     * @param hit The SearchHit to convert
     * @return A Protocol Buffer Hit representation
     * @throws IOException if there's an error during conversion
     */

    /**
     * Similar to {@link SearchHit#toXContent(XContentBuilder, ToXContent.Params)}
     * @param hit
     * @return
     * @throws IOException
     */
    public static org.opensearch.protobufs.Hit toProto(SearchHit hit) throws IOException {
        return toInnerProto(hit);
    }

    /**
     * Similar to {@link SearchHit#toInnerXContent(XContentBuilder, ToXContent.Params)}
     * @param hit The SearchHit to convert
     * @return A Protocol Buffer Hit representation
     * @throws IOException if there's an error during conversion
     */
    // public because we render hit as part of completion suggestion option
    public static org.opensearch.protobufs.Hit toInnerProto(SearchHit hit) throws IOException {
        long startTime = System.currentTimeMillis();
        // System.out.println("=== Hit toInnerProto. index=" + index);
        org.opensearch.protobufs.Hit.Builder hitBuilder = org.opensearch.protobufs.Hit.newBuilder();

        // For inner_hit hits shard is null and that is ok, because the parent search hit has all this information.
        // Even if this was included in the inner_hit hits this would be the same, so better leave it out.
        if (hit.getExplanation() != null && hit.getShard() != null) {
            // builder.field(Fields._SHARD, shard.getShardId());
            // builder.field(Fields._NODE, shard.getNodeIdText());
        }
        if (hit.getIndex() != null) {
            hitBuilder.setIndex(RemoteClusterAware.buildRemoteIndexName(hit.getClusterAlias(), hit.getIndex()));
        }
        if (hit.getId() != null) {
            hitBuilder.setId(hit.getId().toString());
        }
        if (hit.getNestedIdentity() != null) {
            // nestedIdentity.toXContent(builder, params);
        }
        if (hit.getVersion() != -1) {
            hitBuilder.setVersion(hit.getVersion());
        }

        if (hit.getSeqNo() != SequenceNumbers.UNASSIGNED_SEQ_NO) {
            hitBuilder.setSeqNo(hit.getSeqNo());
            hitBuilder.setPrimaryTerm(hit.getPrimaryTerm());
        }

        if (Float.isNaN(hit.getScore())) {
            hitBuilder.setScore(org.opensearch.protobufs.Hit.Score.newBuilder().setNullValue(NullValue.NULL_VALUE_NULL).build());
        } else {
            hitBuilder.setScore(org.opensearch.protobufs.Hit.Score.newBuilder().setFloatValue(hit.getScore()).build());
        }

        // TODO
        for (DocumentField field : hit.getMetaFields().values()) {
            // ignore empty metadata fields
            if (field.getValues().isEmpty()) {
                continue;
            }
            // _ignored is the only multi-valued meta field
            // TODO: can we avoid having an exception here?
            if (field.getName().equals(IgnoredFieldMapper.NAME)) {
                // builder.field(field.getName(), field.getValues());

            } else {
                // builder.field(field.getName(), field.<Object>getValue());
            }
        }
        if (hit.getSourceRef() != null) {
            // XContentHelper.writeRawField(SourceFieldMapper.NAME, source, builder, params);
            hitBuilder.setSource(ByteString.copyFrom(BytesReference.toBytes(hit.getSourceRef())));
        }
        if (hit.getDocumentFields() != null && !hit.getDocumentFields().isEmpty() &&
        // ignore fields all together if they are all empty
            hit.getDocumentFields().values().stream().anyMatch(df -> !df.getValues().isEmpty())) {
            // builder.startObject(Fields.FIELDS);
            // for (DocumentField field : documentFields.values()) {
            // if (!field.getValues().isEmpty()) {
            // field.toXContent(builder, params);
            // }
            // }
            // builder.endObject();
        }
        if (hit.getHighlightFields() != null && !hit.getHighlightFields().isEmpty()) {
            // builder.startObject(Fields.HIGHLIGHT);
            // for (HighlightField field : highlightFields.values()) {
            // field.toXContent(builder, params);
            // }
            // builder.endObject();
        }
        // sortValues.toProto();
        if (hit.getMatchedQueries() != null && hit.getMatchedQueries().length > 0) {
            // boolean includeMatchedQueriesScore = params.paramAsBoolean(RestSearchAction.INCLUDE_NAMED_QUERIES_SCORE_PARAM, false);
            // if (includeMatchedQueriesScore) {
            // builder.startObject(Fields.MATCHED_QUERIES);
            // for (Map.Entry<String, Float> entry : matchedQueries.entrySet()) {
            // builder.field(entry.getKey(), entry.getValue());
            // }
            // builder.endObject();
            // } else {
            // builder.startArray(Fields.MATCHED_QUERIES);
            // for (String matchedFilter : matchedQueries.keySet()) {
            // builder.value(matchedFilter);
            // }
            // builder.endArray();
            // }
        }
        if (hit.getExplanation() != null) {
            // builder.field(Fields._EXPLANATION);
            // buildExplanation(builder, getExplanation());
        }
        if (hit.getInnerHits() != null) {
            // builder.startObject(Fields.INNER_HITS);
            // for (Map.Entry<String, SearchHits> entry : innerHits.entrySet()) {
            // builder.startObject(entry.getKey());
            // entry.getValue().toXContent(builder, params);
            // builder.endObject();
            // }
            // builder.endObject();
        }
        return hitBuilder.build();
    }
}
