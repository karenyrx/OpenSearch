/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.plugin.transport.grpc.proto.response.search;

import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.search.SearchResponseSections;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.XContentBuilder;

import java.io.IOException;

/**
 * Utility class for converting SearchResponse objects to Protocol Buffers.
 * This class handles the conversion of search operation responses to their
 * Protocol Buffer representation.
 */
public class SearchResponseSectionsProtoUtils {

    private SearchResponseSectionsProtoUtils() {
        // Utility class, no instances
    }

    /**
     * Converts a SearchResponse to its Protocol Buffer representation.
     * Similar to {@link SearchResponseSections#toXContent(XContentBuilder, ToXContent.Params)}
     *
     * @param builder The Protocol Buffer SearchResponse builder to populate
     * @param response The SearchResponse to convert
     * @return The populated Protocol Buffer SearchResponse builder
     * @throws IOException if there's an error during conversion
     */
    public static org.opensearch.protobufs.ResponseBody.Builder toProto(
        org.opensearch.protobufs.ResponseBody.Builder builder,
        SearchResponse response
    ) throws IOException {
        builder.setHits(SearchHitsProtoUtils.toProto(response.getHits()));

        // TODO: Implement aggregations conversion
        if (response.getAggregations() != null) {
            // aggregations.toXContent(builder, params);
        }

        if (response.getSuggest() != null) {
            // suggest.toXContent(builder, params);
        }

        if (response.getProfileResults() != null) {
            // profileResults.toXContent(builder, params);
        }

        // TODO: Implement search ext builders conversion
        if (response.getInternalResponse().getSearchExtBuilders() != null
            && !response.getInternalResponse().getSearchExtBuilders().isEmpty()) {
            // builder.startObject(EXT_FIELD.getPreferredName());
            // for (SearchExtBuilder searchExtBuilder : searchExtBuilders) {
            // searchExtBuilder.toXContent(builder, params);
            // }
            // builder.endObject();
        }

        return builder;
    }
}
