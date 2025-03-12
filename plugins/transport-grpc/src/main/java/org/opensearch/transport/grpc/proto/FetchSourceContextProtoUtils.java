/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.transport.grpc.proto;

import org.opensearch.core.common.Strings;
import org.opensearch.protobuf.SourceConfig;
import org.opensearch.protobuf.SourceFilter;
import org.opensearch.search.fetch.subphase.FetchSourceContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for converting SourceConfig Protocol Buffers to FetchSourceContext objects.
 * This class handles the conversion of Protocol Buffer representations to their
 * corresponding OpenSearch objects.
 */
public class FetchSourceContextProtoUtils {

    private FetchSourceContextProtoUtils() {
        // Utility class, no instances
    }

    /**
     * Converts a SourceConfig Protocol Buffer to a FetchSourceContext object.
     * This method was moved from org.opensearch.search.fetch.subphase.FetchSourceContext.
     *
     * The conversion includes:
     * - Fetch source flag
     * - Source includes
     * - Source excludes
     *
     * @param sourceConfig The SourceConfig Protocol Buffer to convert
     * @return A FetchSourceContext object
     */
    public static FetchSourceContext fromProto(SourceConfig sourceConfig) {
        boolean fetchSource = true;
        String[] includes = Strings.EMPTY_ARRAY;
        String[] excludes = Strings.EMPTY_ARRAY;
        if (sourceConfig.getSourceConfigCase() == SourceConfig.SourceConfigCase.FETCH) {
            fetchSource = sourceConfig.getFetch();
        } else if (sourceConfig.hasIncludes()) {
            ArrayList<String> list = new ArrayList<>();
            for (String string : sourceConfig.getIncludes().getStringArrayList()) {
                list.add(string);
            }
            includes = list.toArray(new String[0]);
        } else if (sourceConfig.hasFilter()) {
            SourceFilter sourceFilter = sourceConfig.getFilter();
            if (!sourceFilter.getIncludesList().isEmpty()) {
                List<String> includesList = new ArrayList<>();
                for (String s : sourceFilter.getIncludesList()) {
                    includesList.add(s);
                }
                includes = includesList.toArray(new String[0]);
            } else if (!sourceFilter.getExcludesList().isEmpty()) {
                List<String> excludesList = new ArrayList<>();
                for (String s : sourceFilter.getExcludesList()) {
                    excludesList.add(s);
                }
                excludes = excludesList.toArray(new String[0]);
            }
        }
        return new FetchSourceContext(fetchSource, includes, excludes);
    }
}
