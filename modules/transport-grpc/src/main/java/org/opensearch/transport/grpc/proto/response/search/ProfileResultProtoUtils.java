/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.transport.grpc.proto.response.search;

import org.opensearch.search.profile.ProfileResult;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class for converting ProfileResult objects to Protocol Buffers.
 * ProfileResult is used for both query and aggregation profiling.
 */
public class ProfileResultProtoUtils {

    private static final String TIMING_TYPE_START_TIME_SUFFIX = "_start_time";

    private ProfileResultProtoUtils() {
        // Utility class, no instances
    }

    /**
     * Converts a ProfileResult to its QueryProfile Protocol Buffer representation.
     * Similar to {@link ProfileResult#toXContent(XContentBuilder, Params)}
     *
     * @param result The ProfileResult to convert
     * @return A Protocol Buffer QueryProfile builder
     */
    public static org.opensearch.protobufs.QueryProfile.Builder toQueryProfileProto(ProfileResult result) {
        org.opensearch.protobufs.QueryProfile.Builder builder = org.opensearch.protobufs.QueryProfile.newBuilder();

        builder.setType(result.getQueryName());
        builder.setDescription(result.getLuceneDescription());
        builder.setTimeInNanos(result.getTime());
        createBreakdownView(builder, result.getTimeBreakdown());

        if (false == result.getProfiledChildren().isEmpty()) {
            for (ProfileResult child : result.getProfiledChildren()) {
                builder.addChildren(toQueryProfileProto(child));
            }
        }

        return builder;
    }

    /**
     * Converts a ProfileResult to its AggregationProfile Protocol Buffer representation.
     * Similar to {@link ProfileResult#toXContent(XContentBuilder, Params)}
     *
     * @param result The ProfileResult to convert
     * @return A Protocol Buffer AggregationProfile builder
     */
    public static org.opensearch.protobufs.AggregationProfile.Builder toAggregationProfileProto(ProfileResult result) {
        org.opensearch.protobufs.AggregationProfile.Builder builder = org.opensearch.protobufs.AggregationProfile.newBuilder();

        builder.setType(result.getQueryName());
        builder.setDescription(result.getLuceneDescription());
        builder.setTimeInNanos(result.getTime());
        createAggregationBreakdownView(builder, result.getTimeBreakdown());
        if (false == result.getDebugInfo().isEmpty()) {
            org.opensearch.protobufs.AggregationProfileDebug.Builder debugBuilder = org.opensearch.protobufs.AggregationProfileDebug
                .newBuilder();
            // Map debug info to specific fields if present
            Object segmentsWithMultiValuedOrds = result.getDebugInfo().get("segments_with_multi_valued_ords");
            if (segmentsWithMultiValuedOrds instanceof Integer) {
                debugBuilder.setSegmentsWithMultiValuedOrds((Integer) segmentsWithMultiValuedOrds);
            }
            Object collectionStrategy = result.getDebugInfo().get("collection_strategy");
            if (collectionStrategy instanceof String) {
                debugBuilder.setCollectionStrategy((String) collectionStrategy);
            }
            Object segmentsWithSingleValuedOrds = result.getDebugInfo().get("segments_with_single_valued_ords");
            if (segmentsWithSingleValuedOrds instanceof Integer) {
                debugBuilder.setSegmentsWithSingleValuedOrds((Integer) segmentsWithSingleValuedOrds);
            }
            Object totalBuckets = result.getDebugInfo().get("total_buckets");
            if (totalBuckets instanceof Integer) {
                debugBuilder.setTotalBuckets((Integer) totalBuckets);
            }
            builder.setDebug(debugBuilder);
        }

        if (false == result.getProfiledChildren().isEmpty()) {
            for (ProfileResult child : result.getProfiledChildren()) {
                builder.addChildren(toAggregationProfileProto(child));
            }
        }

        return builder;
    }

    /**
     * Creates the breakdown view for query profiling by mapping the breakdown map to proto fields.
     * Removes start time fields similar to {@link ProfileResult#toXContent}
     *
     * @param builder The QueryProfile builder
     * @param breakdown The breakdown map
     */
    private static void createBreakdownView(org.opensearch.protobufs.QueryProfile.Builder builder, Map<String, Long> breakdown) {
        Map<String, Long> modifiedBreakdown = new LinkedHashMap<>(breakdown);
        removeStartTimeFields(modifiedBreakdown);

        org.opensearch.protobufs.QueryBreakdown.Builder breakdownBuilder = org.opensearch.protobufs.QueryBreakdown.newBuilder();
        for (Map.Entry<String, Long> entry : modifiedBreakdown.entrySet()) {
            String key = entry.getKey();
            Long value = entry.getValue();

            switch (key) {
                case "advance":
                    breakdownBuilder.setAdvance(value);
                    break;
                case "advance_count":
                    breakdownBuilder.setAdvanceCount(value);
                    break;
                case "build_scorer":
                    breakdownBuilder.setBuildScorer(value);
                    break;
                case "build_scorer_count":
                    breakdownBuilder.setBuildScorerCount(value);
                    break;
                case "create_weight":
                    breakdownBuilder.setCreateWeight(value);
                    break;
                case "create_weight_count":
                    breakdownBuilder.setCreateWeightCount(value);
                    break;
                case "match":
                    breakdownBuilder.setMatch(value);
                    break;
                case "match_count":
                    breakdownBuilder.setMatchCount(value);
                    break;
                case "shallow_advance":
                    breakdownBuilder.setShallowAdvance(value);
                    break;
                case "shallow_advance_count":
                    breakdownBuilder.setShallowAdvanceCount(value);
                    break;
                case "score":
                    breakdownBuilder.setScore(value);
                    break;
                case "score_count":
                    breakdownBuilder.setScoreCount(value);
                    break;
                case "compute_max_score":
                    breakdownBuilder.setComputeMaxScore(value);
                    break;
                case "compute_max_score_count":
                    breakdownBuilder.setComputeMaxScoreCount(value);
                    break;
                case "set_min_competitive_score":
                    breakdownBuilder.setSetMinCompetitiveScore(value);
                    break;
                case "set_min_competitive_score_count":
                    breakdownBuilder.setSetMinCompetitiveScoreCount(value);
                    break;
                case "next_doc":
                    breakdownBuilder.setNextDoc(value);
                    break;
                case "next_doc_count":
                    breakdownBuilder.setNextDocCount(value);
                    break;
            }
        }
        builder.setBreakdown(breakdownBuilder);
    }

    /**
     * Creates the breakdown view for aggregation profiling by mapping the breakdown map to proto fields.
     * Removes start time fields similar to {@link ProfileResult#toXContent}
     *
     * @param builder The AggregationProfile builder
     * @param breakdown The breakdown map
     */
    private static void createAggregationBreakdownView(
        org.opensearch.protobufs.AggregationProfile.Builder builder,
        Map<String, Long> breakdown
    ) {
        Map<String, Long> modifiedBreakdown = new LinkedHashMap<>(breakdown);
        removeStartTimeFields(modifiedBreakdown);

        org.opensearch.protobufs.AggregationBreakdown.Builder breakdownBuilder = org.opensearch.protobufs.AggregationBreakdown.newBuilder();
        for (Map.Entry<String, Long> entry : modifiedBreakdown.entrySet()) {
            String key = entry.getKey();
            Long value = entry.getValue();

            switch (key) {
                case "initialize":
                    breakdownBuilder.setInitialize(value);
                    break;
                case "initialize_count":
                    breakdownBuilder.setInitializeCount(value);
                    break;
                case "collect":
                    breakdownBuilder.setCollect(value);
                    break;
                case "collect_count":
                    breakdownBuilder.setCollectCount(value);
                    break;
                case "build_aggregation":
                    breakdownBuilder.setBuildAggregation(value);
                    break;
                case "build_aggregation_count":
                    breakdownBuilder.setBuildAggregationCount(value);
                    break;
                case "build_leaf_collector":
                    breakdownBuilder.setBuildLeafCollector(value);
                    break;
                case "build_leaf_collector_count":
                    breakdownBuilder.setBuildLeafCollectorCount(value);
                    break;
                case "reduce":
                    breakdownBuilder.setReduce(value);
                    break;
                case "reduce_count":
                    breakdownBuilder.setReduceCount(value);
                    break;
                case "post_collection":
                    breakdownBuilder.setPostCollection(value);
                    break;
                case "post_collection_count":
                    breakdownBuilder.setPostCollectionCount(value);
                    break;
            }
        }
        builder.setBreakdown(breakdownBuilder);
    }

    /**
     * Removes start time fields from the breakdown map.
     * Similar to {@link ProfileResult#removeStartTimeFields(Map)}
     *
     * @param modifiedBreakdown The breakdown map to modify
     */
    private static void removeStartTimeFields(Map<String, Long> modifiedBreakdown) {
        Iterator<Map.Entry<String, Long>> iterator = modifiedBreakdown.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (entry.getKey().endsWith(TIMING_TYPE_START_TIME_SUFFIX)) {
                iterator.remove();
            }
        }
    }
}
