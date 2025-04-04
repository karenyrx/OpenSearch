/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.plugin.transport.grpc.proto.request.search;

import org.opensearch.common.unit.TimeValue;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.plugin.transport.grpc.proto.request.common.FetchSourceContextProtoUtils;
import org.opensearch.plugin.transport.grpc.proto.request.common.ScriptProtoUtils;
import org.opensearch.plugin.transport.grpc.proto.request.search.query.AbstractQueryBuilderProtoUtils;
import org.opensearch.plugin.transport.grpc.proto.request.search.suggest.SuggestBuilderProtoUtils;
import org.opensearch.protobufs.DerivedField;
import org.opensearch.protobufs.FieldAndFormat;
import org.opensearch.protobufs.NumberMap;
import org.opensearch.protobufs.Rescore;
import org.opensearch.protobufs.ScoreSort;
import org.opensearch.protobufs.ScriptField;
import org.opensearch.protobufs.SearchRequestBody;
import org.opensearch.protobufs.SortCombinations;
import org.opensearch.protobufs.TrackHits;
import org.opensearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.Map;

import static org.opensearch.search.builder.SearchSourceBuilder.TIMEOUT_FIELD;
import static org.opensearch.search.builder.SearchSourceBuilder.searchSource;
import static org.opensearch.search.internal.SearchContext.TRACK_TOTAL_HITS_ACCURATE;
import static org.opensearch.search.internal.SearchContext.TRACK_TOTAL_HITS_DISABLED;

/**
 * Utility class for converting SearchSourceBuilder Protocol Buffers to objects
 *
 */
public class SearchSourceBuilderProtoUtils {

    private SearchSourceBuilderProtoUtils() {
        // Utility class, no instances
    }

    /**
     * Parses a protobuf SearchRequestBody into a SearchSourceBuilder.
     * This method is equivalent to {@link SearchSourceBuilder#parseXContent(XContentParser, boolean)}
     *
     * @param searchSourceBuilder The SearchSourceBuilder to populate
     * @param protoRequest The Protocol Buffer SearchRequest to parse
     * @throws IOException if there's an error during parsing
     */

    public static void parseProto(SearchSourceBuilder searchSourceBuilder, SearchRequestBody protoRequest) throws IOException {
        // TODO what to do about parser.getDeprecationHandler() for protos?

        if (protoRequest.hasFrom()) {
            searchSourceBuilder.from(protoRequest.getFrom());
        } else if (protoRequest.hasSize()) {
            searchSourceBuilder.size(protoRequest.getSize());
        } else if (protoRequest.hasTimeout()) {
            searchSourceBuilder.timeout(TimeValue.parseTimeValue(protoRequest.getTimeout(), null, TIMEOUT_FIELD.getPreferredName()));
        } else if (protoRequest.hasTerminateAfter()) {
            searchSourceBuilder.terminateAfter(protoRequest.getTerminateAfter());
        } else if (protoRequest.hasMinScore()) {
            searchSourceBuilder.minScore(protoRequest.getMinScore());
        } else if (protoRequest.hasVersion()) {
            searchSourceBuilder.version(protoRequest.getVersion());
        } else if (protoRequest.hasSeqNoPrimaryTerm()) {
            searchSourceBuilder.seqNoAndPrimaryTerm(protoRequest.getSeqNoPrimaryTerm());
        } else if (protoRequest.hasExplain()) {
            searchSourceBuilder.explain(protoRequest.getExplain());
        } else if (protoRequest.hasTrackScores()) {
            searchSourceBuilder.trackScores(protoRequest.getTrackScores());
        } else if (protoRequest.hasIncludeNamedQueriesScore()) {
            searchSourceBuilder.includeNamedQueriesScores(protoRequest.getIncludeNamedQueriesScore());
        } else if (protoRequest.hasTrackTotalHits()) {
            if (protoRequest.getTrackTotalHits().getTrackHitsCase() == TrackHits.TrackHitsCase.BOOL_VALUE) {
                searchSourceBuilder.trackTotalHitsUpTo(
                    protoRequest.getTrackTotalHits().getBoolValue() ? TRACK_TOTAL_HITS_ACCURATE : TRACK_TOTAL_HITS_DISABLED
                );
            } else {
                searchSourceBuilder.trackTotalHitsUpTo(protoRequest.getTrackTotalHits().getInt32Value());
            }
        } else if (protoRequest.hasSource()) {
            searchSourceBuilder.fetchSource(FetchSourceContextProtoUtils.fromProto(protoRequest.getSource()));
        } else if (protoRequest.getStoredFieldsCount() > 0) {
            searchSourceBuilder.storedFields(StoredFieldsContextProtoUtils.fromProto(protoRequest.getStoredFieldsList()));
        } else if (protoRequest.getSortCount() > 0) {
            for (SortCombinations sortCombinations : protoRequest.getSortList()) {
                if (sortCombinations.hasStringValue()) {
                    String name = sortCombinations.getStringValue();
                    searchSourceBuilder.sort(name);
                } else if (sortCombinations.hasFieldWithOrderMap()) {
                    for (Map.Entry<String, ScoreSort> entry : sortCombinations.getFieldWithOrderMap()
                        .getFieldWithOrderMapMap()
                        .entrySet()) {
                        String name = entry.getKey();
                        ScoreSort scoreSort = entry.getValue();
                        // TODO
                        // searchSourceBuilder.sort(name, SortBuilderProtoUtils.fromProto(scoreSort));
                    }
                } else if (sortCombinations.hasSortOptions()) {
                    // TODO
                } else {
                    throw new IllegalArgumentException("Must provide oneof sort combinations");
                }
            }
        } else if (protoRequest.hasProfile()) {
            searchSourceBuilder.profile(protoRequest.getProfile());
        } else if (protoRequest.hasSearchPipeline()) {
            searchSourceBuilder.pipeline(protoRequest.getSearchPipeline());
        } else if (protoRequest.hasVerbosePipeline()) {
            searchSourceBuilder.verbosePipeline(protoRequest.getVerbosePipeline());
        } else if (protoRequest.hasQuery()) {
            System.out.println("=== before parseInnerQueryBuilderProto = " + protoRequest.getQuery());
            searchSourceBuilder.query(AbstractQueryBuilderProtoUtils.parseInnerQueryBuilderProto(protoRequest.getQuery()));
            System.out.println("=== after parseInnerQueryBuilderProto = " + protoRequest.getQuery());
        } else if (protoRequest.hasPostFilter()) {
            searchSourceBuilder.postFilter(AbstractQueryBuilderProtoUtils.parseInnerQueryBuilderProto(protoRequest.getQuery()));
        } else if (protoRequest.hasSource()) {
            searchSourceBuilder.fetchSource(FetchSourceContextProtoUtils.fromProto(protoRequest.getSource()));
        } else if (protoRequest.getScriptFieldsCount() > 0) {
            for (Map.Entry<String, ScriptField> entry : protoRequest.getScriptFieldsMap().entrySet()) {
                String name = entry.getKey();
                ScriptField scriptFieldProto = entry.getValue();
                SearchSourceBuilder.ScriptField scriptField = ScriptFieldProtoUtils.fromProto(name, scriptFieldProto);
                searchSourceBuilder.scriptField(name, scriptField.script(), scriptField.ignoreFailure());
            }
        } else if (protoRequest.getIndicesBoostCount() > 0) {
            /**
             * Similar to {@link SearchSourceBuilder.IndexBoost#IndexBoost(XContentParser)}
             */
            for (NumberMap numberMap : protoRequest.getIndicesBoostList()) {
                for (Map.Entry<String, Float> entry : numberMap.getNumberMapMap().entrySet()) {
                    searchSourceBuilder.indexBoost(entry.getKey(), entry.getValue());
                }
            }
            // TODO support aggregations, highlight, suggest, sort, rescore, slice, collapse
            // } else if (protoRequest.getAggsCount() > 0)
            // || AGGS_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
            // aggregations = AggregatorFactories.parseAggregators(parser);
        } else if (protoRequest.hasHighlight()) {
            searchSourceBuilder.highlighter(HighlightBuilderProtoUtils.fromProto(protoRequest.getHighlight()));
            // // TODO add to protos
        } else if (protoRequest.hasSuggest()) {
            searchSourceBuilder.suggest(SuggestBuilderProtoUtils.fromProto(protoRequest.getSuggest()));
        } else if (protoRequest.getRescoreCount() > 0) {
            for (Rescore rescore : protoRequest.getRescoreList()) {
                searchSourceBuilder.addRescorer(RescorerBuilderProtoUtils.parseFromProto(rescore));
            }
            // // } else if (EXT_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
            // // extBuilders = new ArrayList<>();
            // // String extSectionName = null;
            // // while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            // // if (token == XContentParser.Token.FIELD_NAME) {
            // // extSectionName = parser.currentName();
            // // } else {
            // // SearchExtBuilder searchExtBuilder = parser.namedObject(SearchExtBuilder.class, extSectionName, null);
            // // if (searchExtBuilder.getWriteableName().equals(extSectionName) == false) {
            // // throw new IllegalStateException(
            // // "The parsed ["
            // // + searchExtBuilder.getClass().getName()
            // // + "] object has a "
            // // + "different writeable name compared to the name of the section that it was parsed from: found ["
            // // + searchExtBuilder.getWriteableName()
            // // + "] expected ["
            // // + extSectionName
            // // + "]"
            // // );
            // // }
            // // extBuilders.add(searchExtBuilder);
            // // }
            // // }
        } else if (protoRequest.hasSlice()) {
            // TODO
            searchSource().slice(SliceBuilderProtoUtils.fromProto(protoRequest.getSlice()));
        } else if (protoRequest.hasCollapse()) {
            searchSourceBuilder.collapse(CollapseBuilderProtoUtils.fromProto(protoRequest.getCollapse()));
        } else if (protoRequest.hasPit()) {
            searchSourceBuilder.pointInTimeBuilder(PointInTimeBuilderProtoUtils.fromProto(protoRequest.getPit()));
        } else if (protoRequest.getDerivedCount() > 0) {
            for (Map.Entry<String, DerivedField> entry : protoRequest.getDerivedMap().entrySet()) {
                // TODO fix protos
                String name = entry.getKey();
                DerivedField derivedField = entry.getValue();
                searchSourceBuilder.derivedField(
                    name,
                    derivedField.getType(),
                    ScriptProtoUtils.parseFromProtoRequest(derivedField.getScript())
                );
            }
        } else if (protoRequest.getDocvalueFieldsCount() > 0) {
            for (FieldAndFormat fieldAndFormatProto : protoRequest.getDocvalueFieldsList()) {
                /**
                 * Similar to {@link org.opensearch.search.fetch.subphase.FieldAndFormat#fromXContent(XContentParser)}
                */
                searchSourceBuilder.docValueField(fieldAndFormatProto.getField(), fieldAndFormatProto.getFormat());
            }

        } else if (protoRequest.getFieldsCount() > 0) {
            for (FieldAndFormat fieldAndFormatProto : protoRequest.getFieldsList()) {
                /**
                 * Similar to {@link org.opensearch.search.fetch.subphase.FieldAndFormat#fromXContent(XContentParser)}
                 */
                searchSourceBuilder.fetchField(fieldAndFormatProto.getField(), fieldAndFormatProto.getFormat());
            }
        } else if (protoRequest.getStatsCount() > 0) {
            searchSourceBuilder.stats(protoRequest.getStatsList());
        } else if (protoRequest.getSearchAfterCount() > 0) {
            searchSourceBuilder.searchAfter(SearchAfterBuilderProtoUtils.fromProto(protoRequest.getSearchAfterList()));
        }
    }
}
