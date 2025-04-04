/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.plugin.transport.grpc.proto.request.search;

import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.protobufs.SortCombinations;
import org.opensearch.search.sort.SortBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for converting SearchSourceBuilder Protocol Buffers to objects
 *
 */
public class SortBuilderProtoUtils {

    private SortBuilderProtoUtils() {
        // Utility class, no instances
    }

    /**
     * Similar to {@link SortBuilder#fromXContent(XContentParser)}
     *
     * @param sortProto
     */

    public static List<SortBuilder<?>> fromProto(List<SortCombinations> sortProto) {
        List<SortBuilder<?>> sortFields = new ArrayList<>(2);

        // TODO populate list

        return sortFields;
    }

}
