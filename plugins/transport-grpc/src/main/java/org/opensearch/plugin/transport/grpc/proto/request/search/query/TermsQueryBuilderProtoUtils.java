/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.plugin.transport.grpc.proto.request.search.query;

import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.index.query.TermsQueryBuilder;
import org.opensearch.protobufs.TermsLookupFieldStringArrayMap;

import java.util.Map;

/**
 * Utility class for converting MatchAllQuery Protocol Buffers to objects
 *
 */
public class TermsQueryBuilderProtoUtils {

    private TermsQueryBuilderProtoUtils() {
        // Utility class, no instances
    }

    /**
     * Similar to {@link TermsQueryBuilder#fromXContent(XContentParser)}
     *
     * @param termsQueryFieldProto
     */
    public static TermsQueryBuilder fromProto(org.opensearch.protobufs.TermsQueryField termsQueryFieldProto) {
        // String fieldName = null;
        // List<Object> values = null;
        // TermsLookup termsLookup = null;
        // String queryName = null;
        // float boost = AbstractQueryBuilder.DEFAULT_BOOST;
        // String valueTypeStr = TermsQueryBuilder.ValueType.DEFAULT.type;

        TermsQueryBuilder builder = null;
        // TODO does not support nested term query yet. only 1 layer.
        for (Map.Entry<String, TermsLookupFieldStringArrayMap> entry : termsQueryFieldProto.getTermsLookupFieldStringArrayMapMap()
            .entrySet()) {
            String field = entry.getKey();
            TermsLookupFieldStringArrayMap termsLookupFieldStringArrayMap = entry.getValue();

            if (termsLookupFieldStringArrayMap.hasTermsLookupField()) {
                termsLookupFieldStringArrayMap.getTermsLookupField();
            } else if (termsLookupFieldStringArrayMap.hasStringArray()) {
                builder = new TermsQueryBuilder(field, termsLookupFieldStringArrayMap.getStringArray().getStringArrayList());
            }

            if (termsQueryFieldProto.hasBoost()) {
                builder.boost(termsQueryFieldProto.getBoost());
            }
        }

        return builder;
    }

}
