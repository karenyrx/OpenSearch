/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.transport.grpc.proto.request.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.support.IndicesOptions;
import org.opensearch.protobufs.SearchRequest;
import org.opensearch.rest.RestRequest;

import java.util.EnumSet;
import java.util.List;

import static org.opensearch.action.support.IndicesOptions.WildcardStates.CLOSED;
import static org.opensearch.action.support.IndicesOptions.WildcardStates.HIDDEN;
import static org.opensearch.action.support.IndicesOptions.WildcardStates.OPEN;
import static org.opensearch.action.support.IndicesOptions.fromOptions;

public class IndicesOptionsProtoUtils {
    protected static Logger logger = LogManager.getLogger(IndicesOptionsProtoUtils.class);

    private IndicesOptionsProtoUtils() {
        // Utility class, no instances
    }

    /**
     * Similar to{@link IndicesOptions#fromRequest(RestRequest, IndicesOptions)}
     */
    public static IndicesOptions fromRequest(org.opensearch.protobufs.SearchRequest request, IndicesOptions defaultSettings) {
        return fromProtoParameters(request, defaultSettings);
    }

    /**
     * Similar to {@link IndicesOptions#fromParameters(Object, Object, Object, Object, IndicesOptions)}
     */
    public static IndicesOptions fromProtoParameters(SearchRequest request, IndicesOptions defaultSettings) {
        if (!(request.getExpandWildcardsCount() > 0)
            && !request.hasIgnoreUnavailable()
            && !request.hasAllowNoIndices()
            && !request.hasIgnoreThrottled()) {
            return defaultSettings;
        }

        // TODO double check this works
        EnumSet<IndicesOptions.WildcardStates> wildcards = parseProtoParameter(
            request.getExpandWildcardsList(),
            defaultSettings.getExpandWildcards()
        );

        // note that allowAliasesToMultipleIndices is not exposed, always true (only for internal use)
        return fromOptions(
            request.hasIgnoreUnavailable() ? request.getIgnoreUnavailable() : defaultSettings.ignoreUnavailable(),
            request.hasAllowNoIndices() ? request.getAllowNoIndices() : defaultSettings.allowNoIndices(),
            wildcards.contains(OPEN),
            wildcards.contains(CLOSED),
            wildcards.contains(IndicesOptions.WildcardStates.HIDDEN),
            defaultSettings.allowAliasesToMultipleIndices(),
            defaultSettings.forbidClosedIndices(),
            defaultSettings.ignoreAliases(),
            request.hasIgnoreThrottled() ? request.getIgnoreThrottled() : defaultSettings.ignoreThrottled()
        );
    }

    /**
     * Similar to {@link IndicesOptions.WildcardStates#parseParameter(Object, EnumSet)}
     * @param wildcardList
     * @param defaultStates
     * @return
     */
    public static EnumSet<IndicesOptions.WildcardStates> parseProtoParameter(
        List<SearchRequest.ExpandWildcard> wildcardList,
        EnumSet<IndicesOptions.WildcardStates> defaultStates
    ) {
        if (wildcardList.isEmpty()) {
            return defaultStates;
        }

        EnumSet<IndicesOptions.WildcardStates> states = EnumSet.noneOf(IndicesOptions.WildcardStates.class);
        // TODO why do we let patterns like "none,all" or "open,none,closed" get used. The location of 'none' in the array changes the
        // meaning of the resulting value
        for (SearchRequest.ExpandWildcard wildcard : wildcardList) {
            updateSetForValue(states, wildcard);
        }

        return states;
    }

    /**
     * Keep implementation consistent with {@link IndicesOptions.WildcardStates#updateSetForValue(EnumSet, String)}
     */
    public static void updateSetForValue(EnumSet<IndicesOptions.WildcardStates> states, SearchRequest.ExpandWildcard wildcard) {
        switch (wildcard) {
            case EXPAND_WILDCARD_OPEN:
                states.add(OPEN);
                break;
            case EXPAND_WILDCARD_CLOSED:
                states.add(CLOSED);
                break;
            case EXPAND_WILDCARD_HIDDEN:
                states.add(HIDDEN);
                break;
            case EXPAND_WILDCARD_NONE:
                states.clear();
                break;
            case EXPAND_WILDCARD_ALL:
                states.addAll(EnumSet.allOf(IndicesOptions.WildcardStates.class));
                break;
            default:
                throw new IllegalArgumentException("No valid expand wildcard value [" + wildcard + "]");
        }
    }
}
