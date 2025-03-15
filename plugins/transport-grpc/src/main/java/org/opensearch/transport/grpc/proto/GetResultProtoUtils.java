/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.transport.grpc.proto;

import com.google.protobuf.ByteString;
import org.opensearch.index.get.GetResult;
import org.opensearch.index.seqno.SequenceNumbers;
import org.opensearch.protobuf.InlineGetDictUserDefined;

/**
 * Utility class for converting GetResult objects to Protocol Buffers.
 * This class handles the conversion of document get operation results to their
 * Protocol Buffer representation.
 */
public class GetResultProtoUtils {

    private GetResultProtoUtils() {
        // Utility class, no instances
    }

    /**
     * Converts a GetResult to its Protocol Buffer representation.
     * This method is equivalent to the toXContentEmbedded method in GetResult.java.
     *
     * @param getResult The GetResult to convert
     * @return A Protocol Buffer InlineGetDictUserDefined representation
     */
    public static InlineGetDictUserDefined toProto(GetResult getResult) {
        InlineGetDictUserDefined.Builder builder = InlineGetDictUserDefined.newBuilder();

        // Set index name
        // if (getResult.getIndex() != null) {
        // builder.setIndex(getResult.getIndex());
        // }

        // // Set document ID
        // if (getResult.getId() != null) {
        // builder.setId(getResult.getId());
        // }

        if (getResult.isExists()) {
            // Set document version if available
            if (getResult.getVersion() != -1) {
                // builder.setVersion(getResult.getVersion());
            }
            builder = toProtoEmbedded(getResult, builder);
        } else {
            builder.setFound(false);
        }

        return builder.build();
    }

    /**
     * Converts a GetResult to its Protocol Buffer representation for embedding in another message.
     * This method is equivalent to the toProtoEmbedded method in GetResult.
     *
     * @param getResult The GetResult to convert
     * @param builder The builder to add the GetResult data to
     * @return The updated builder with the GetResult data
     */
    public static InlineGetDictUserDefined.Builder toProtoEmbedded(GetResult getResult, InlineGetDictUserDefined.Builder builder) {
        // Set sequence number and primary term if available
        if (getResult.getSeqNo() != SequenceNumbers.UNASSIGNED_SEQ_NO) {
            builder.setSeqNo(getResult.getSeqNo());
            builder.setPrimaryTerm(getResult.getPrimaryTerm());
        }

        // TODO: Add support for meta fields
        // for (DocumentField field : metaFields.values()) {
        // if (field.getName().equals(IgnoredFieldMapper.NAME)) {
        // builder.field(field.getName(), field.getValues());
        // } else {
        // builder.field(field.getName(), field.<Object>getValue());
        // }
        // }

        // Set existence status
        builder.setFound(getResult.isExists());

        // Set source if available
        if (getResult.source() != null) {
            builder.setSource(ByteString.copyFrom(getResult.source()));
        }

        // TODO: Add support for document fields
        // if (!documentFields.isEmpty()) {
        // for (DocumentField field : documentFields.values()) {
        // field.toXContent(builder, params);
        // }
        // }

        return builder;
    }
}
