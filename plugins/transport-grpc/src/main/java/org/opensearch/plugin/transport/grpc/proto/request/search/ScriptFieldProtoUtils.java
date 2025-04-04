/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.plugin.transport.grpc.proto.request.search;

import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.plugin.transport.grpc.proto.request.common.ScriptProtoUtils;
import org.opensearch.protobufs.ScriptField;
import org.opensearch.script.Script;
import org.opensearch.search.builder.SearchSourceBuilder;

import java.io.IOException;

/**
 * Utility class for converting SearchSourceBuilder Protocol Buffers to objects
 *
 */
public class ScriptFieldProtoUtils {

    private ScriptFieldProtoUtils() {
        // Utility class, no instances
    }

    /**
     * Similar to {@link SearchSourceBuilder.ScriptField#ScriptField(XContentParser)}
     *
     * @param scriptFieldName
     * @param scriptFieldProto
     * @throws IOException if there's an error during parsing
     */

    public static SearchSourceBuilder.ScriptField fromProto(String scriptFieldName, ScriptField scriptFieldProto) throws IOException {
        Script script = ScriptProtoUtils.parseFromProtoRequest(scriptFieldProto.getScript());
        boolean ignoreFailure = scriptFieldProto.hasIgnoreFailure() ? scriptFieldProto.getIgnoreFailure() : false;

        return new SearchSourceBuilder.ScriptField(scriptFieldName, script, ignoreFailure);
    }

}
