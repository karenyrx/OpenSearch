/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.transport.grpc.proto.request.search.suggest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.protobufs.SearchRequest;
import org.opensearch.search.suggest.term.TermSuggestionBuilder;

public class TermSuggestionBuilderProtoUtils {
    protected static Logger logger = LogManager.getLogger(TermSuggestionBuilderProtoUtils.class);

    private TermSuggestionBuilderProtoUtils() {
        // Utility class, no instances
    }

    /**
     *
     * Similar to {@link TermSuggestionBuilder.SuggestMode#resolve(String)}
     *
     * @param suggest_mode
     * @return
     */
    public static TermSuggestionBuilder.SuggestMode resolve(final SearchRequest.SuggestMode suggest_mode) {
        switch (suggest_mode) {
            case SUGGEST_MODE_ALWAYS:
                return TermSuggestionBuilder.SuggestMode.ALWAYS;
            case SUGGEST_MODE_MISSING:
                return TermSuggestionBuilder.SuggestMode.MISSING;
            case SUGGEST_MODE_POPULAR:
                return TermSuggestionBuilder.SuggestMode.POPULAR;
            default:
                throw new IllegalArgumentException("Invalid suggest_mode " + suggest_mode.toString());
        }
    }
}
