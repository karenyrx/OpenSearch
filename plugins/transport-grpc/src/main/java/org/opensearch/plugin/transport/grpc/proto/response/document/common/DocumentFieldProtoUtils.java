/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.plugin.transport.grpc.proto.response.document.common;

import com.google.protobuf.Struct;
import org.opensearch.common.document.DocumentField;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.XContentBuilder;

import java.util.List;

/**
 * Utility class for converting DocumentField objects to Protocol Buffers.
 * This class handles the conversion of document get operation results to their
 * Protocol Buffer representation.
 */
public class DocumentFieldProtoUtils {

    private DocumentFieldProtoUtils() {
        // Utility class, no instances
    }

    /**
     * Converts a DocumentField to its Protocol Buffer representation.
     * This method is equivalent to the  {@link DocumentField#toXContent(XContentBuilder, ToXContent.Params)}
     *
     * @param fieldValue The DocumentField to convert
     * @return A Protobuf builder InlineGetDictUserDefined representation
     */
    public static Struct toProto(List<Object> fieldValue) {

        Struct.Builder structBuilder = Struct.newBuilder();
        // TODO how to convert DocumentField to ObjectMap?
        // Map<String, Object> fieldMap;
        // if ((field.getValues() instanceof Map)) {
        //     fieldMap = (Map<String, Object>) field;
        // } else {
        //     throw new UnsupportedOperationException("document field is not a map of key/value pairs. Cannot convert to InlineGetDictUserDefined fields protobuf");
        // }

        // for (Map.Entry<String, Object> entry : fieldMap.entrySet()){
        //     structBuilder.putFields(entry.getKey(), StructProtoUtils.toProto(entry.getValue()));
        // }
        return structBuilder.build();
    }


}
