/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.plugin.transport.grpc.proto.request.search.query;

import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.index.query.AbstractQueryBuilder;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.plugin.transport.grpc.proto.request.common.ObjectMapProtoUtils;
import org.opensearch.protobufs.FieldValue;
import org.opensearch.protobufs.TermQuery;
import org.opensearch.protobufs.TermQueryFieldValue;

import java.util.Map;

/**
 * Utility class for converting MatchAllQuery Protocol Buffers to objects
 *
 */
public class TermQueryBuilderProtoUtils {

    private TermQueryBuilderProtoUtils() {
        // Utility class, no instances
    }

    /**
     * Similar to {@link TermQueryBuilder#fromXContent(XContentParser)}
     *
     * @param termQueryProto
     */

    public static TermQueryBuilder fromProto(Map<String, TermQueryFieldValue> termQueryProto) {
        String queryName = null;
        String fieldName = null;
        Object value = null;
        float boost = AbstractQueryBuilder.DEFAULT_BOOST;
        boolean caseInsensitive = TermQueryBuilder.DEFAULT_CASE_INSENSITIVITY;

        if (termQueryProto.size() > 1) {
            throw new IllegalArgumentException("Term query can only have 1 element in the map");
        }

        // TODO remove TermQueryFieldValue and use TermQuery instead.

        for (Map.Entry<String, TermQueryFieldValue> entry : termQueryProto.entrySet()) {

            fieldName = entry.getKey();

            TermQueryFieldValue termQueryFieldValue = entry.getValue();

            if (termQueryFieldValue.hasTermQuery()) {
                TermQuery termQuery = termQueryFieldValue.getTermQuery();
                // TODO fix protos
                // if (termQuery.hasName()) {
                queryName = termQuery.getName();
                // }
                // TODO fix protos
                // if (termQuery.hasBoost()) {
                boost = termQuery.getBoost();
                // }

                FieldValue fieldValue = termQueryFieldValue.getFieldValue();

                switch (fieldValue.getTypeCase()) {
                    case GENERAL_NUMBER:
                        // TODO: only assumes float right now
                        value = fieldValue.getGeneralNumber().getFloatValue();
                        break;
                    case STRING_VALUE:
                        value = fieldValue.getStringValue();
                        break;
                    case OBJECT_MAP:
                        value = ObjectMapProtoUtils.fromProto(fieldValue.getObjectMap());
                        break;
                    case BOOL_VALUE:
                        value = fieldValue.getBoolValue();
                        break;
                    default:
                        throw new IllegalArgumentException("TermQuery field value not recognized");
                }
            }
        }
        TermQueryBuilder termQuery = new TermQueryBuilder(fieldName, value);
        termQuery.boost(boost);
        if (queryName != null) {
            termQuery.queryName(queryName);
        }
        termQuery.caseInsensitive(caseInsensitive);

        return termQuery;
    }
}
