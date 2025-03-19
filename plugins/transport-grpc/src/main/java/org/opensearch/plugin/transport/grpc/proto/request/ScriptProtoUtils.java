/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.transport.grpc.proto.request;

import org.opensearch.core.common.Strings;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.protobuf.InlineScript;
import org.opensearch.protobuf.StoredScriptId;
import org.opensearch.protobuf.SourceConfig;
import org.opensearch.protobuf.SourceConfigParam;
import org.opensearch.protobuf.SourceFilter;
import org.opensearch.script.Script;
import org.opensearch.script.ScriptType;
import org.opensearch.search.fetch.subphase.FetchSourceContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.opensearch.script.Script.DEFAULT_SCRIPT_LANG;

/**
 * Utility class for converting SourceConfig Protocol Buffers to FetchSourceContext objects.
 * This class handles the conversion of Protocol Buffer representations to their
 * corresponding OpenSearch objects.
 */
public class ScriptProtoUtils {

    private ScriptProtoUtils() {
        // Utility class, no instances
    }

    /**
     * Converts a Script Protocol Buffer to a Script object.
     * Similar to {@link Script#parse(XContentParser)}
     */
    public static Script parseFromProtoRequest(org.opensearch.protobuf.Script script) {
        return parseFromProtoRequest(script, DEFAULT_SCRIPT_LANG);
    }

    /**
     * Converts a Script Protocol Buffer to a Script object.
     * Similar to {@link Script#parse(XContentParser, String)}
     */
    private static Script parseFromProtoRequest(org.opensearch.protobuf.Script script, String defaultLang){
        // TODO: support script param
        throw new UnsupportedOperationException("Script param is not supported yet");
    }

    /** Parses a protobuf InlineScript to a POJO  */
    private static String parseInlineScript(InlineScript inlineScript){
        // TODO
        return null;
    }

     /** Parses a protobuf StoredScriptId to a POJO  */
    private static String parseStoredScriptId(StoredScriptId storedScriptId){
        // TODO

        return null;
    }
}
