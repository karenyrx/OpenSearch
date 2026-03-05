/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.transport.grpc.proto.response.search;

import org.opensearch.protobufs.AggregationProfile;
import org.opensearch.protobufs.QueryProfile;
import org.opensearch.search.profile.ProfileResult;
import org.opensearch.test.OpenSearchTestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileResultProtoUtilsTests extends OpenSearchTestCase {

    public void testToQueryProfileProtoBasic() {
        // Create a basic ProfileResult for query
        Map<String, Long> breakdown = new HashMap<>();
        breakdown.put("advance", 1000L);
        breakdown.put("advance_count", 5L);
        breakdown.put("build_scorer", 2000L);
        breakdown.put("build_scorer_count", 3L);

        ProfileResult result = new ProfileResult("TermQuery", "field:value", breakdown, Map.of(), 5000L, List.of());

        // Convert to proto
        QueryProfile.Builder builder = ProfileResultProtoUtils.toQueryProfileProto(result);
        QueryProfile queryProfile = builder.build();

        // Verify
        assertEquals("TermQuery", queryProfile.getType());
        assertEquals("field:value", queryProfile.getDescription());
        assertEquals(5000L, queryProfile.getTimeInNanos());
        assertEquals(1000L, queryProfile.getBreakdown().getAdvance());
        assertEquals(5L, queryProfile.getBreakdown().getAdvanceCount());
        assertEquals(2000L, queryProfile.getBreakdown().getBuildScorer());
        assertEquals(3L, queryProfile.getBreakdown().getBuildScorerCount());
    }

    public void testToQueryProfileProtoWithAllBreakdownFields() {
        // Create ProfileResult with all breakdown fields
        Map<String, Long> breakdown = new HashMap<>();
        breakdown.put("advance", 100L);
        breakdown.put("advance_count", 1L);
        breakdown.put("build_scorer", 200L);
        breakdown.put("build_scorer_count", 2L);
        breakdown.put("create_weight", 300L);
        breakdown.put("create_weight_count", 3L);
        breakdown.put("match", 400L);
        breakdown.put("match_count", 4L);
        breakdown.put("shallow_advance", 500L);
        breakdown.put("shallow_advance_count", 5L);
        breakdown.put("score", 600L);
        breakdown.put("score_count", 6L);
        breakdown.put("compute_max_score", 700L);
        breakdown.put("compute_max_score_count", 7L);
        breakdown.put("set_min_competitive_score", 800L);
        breakdown.put("set_min_competitive_score_count", 8L);
        breakdown.put("next_doc", 900L);
        breakdown.put("next_doc_count", 9L);

        ProfileResult result = new ProfileResult("BooleanQuery", "bool query", breakdown, Map.of(), 10000L, List.of());

        // Convert to proto
        QueryProfile queryProfile = ProfileResultProtoUtils.toQueryProfileProto(result).build();

        // Verify all breakdown fields
        assertEquals(100L, queryProfile.getBreakdown().getAdvance());
        assertEquals(1L, queryProfile.getBreakdown().getAdvanceCount());
        assertEquals(200L, queryProfile.getBreakdown().getBuildScorer());
        assertEquals(2L, queryProfile.getBreakdown().getBuildScorerCount());
        assertEquals(300L, queryProfile.getBreakdown().getCreateWeight());
        assertEquals(3L, queryProfile.getBreakdown().getCreateWeightCount());
        assertEquals(400L, queryProfile.getBreakdown().getMatch());
        assertEquals(4L, queryProfile.getBreakdown().getMatchCount());
        assertEquals(500L, queryProfile.getBreakdown().getShallowAdvance());
        assertEquals(5L, queryProfile.getBreakdown().getShallowAdvanceCount());
        assertEquals(600L, queryProfile.getBreakdown().getScore());
        assertEquals(6L, queryProfile.getBreakdown().getScoreCount());
        assertEquals(700L, queryProfile.getBreakdown().getComputeMaxScore());
        assertEquals(7L, queryProfile.getBreakdown().getComputeMaxScoreCount());
        assertEquals(800L, queryProfile.getBreakdown().getSetMinCompetitiveScore());
        assertEquals(8L, queryProfile.getBreakdown().getSetMinCompetitiveScoreCount());
        assertEquals(900L, queryProfile.getBreakdown().getNextDoc());
        assertEquals(9L, queryProfile.getBreakdown().getNextDocCount());
    }

    public void testToQueryProfileProtoWithChildren() {
        // Create child ProfileResults
        Map<String, Long> childBreakdown = new HashMap<>();
        childBreakdown.put("advance", 100L);
        childBreakdown.put("advance_count", 1L);

        ProfileResult child1 = new ProfileResult("TermQuery", "child1:value", childBreakdown, Map.of(), 500L, List.of());
        ProfileResult child2 = new ProfileResult("TermQuery", "child2:value", childBreakdown, Map.of(), 600L, List.of());

        // Create parent ProfileResult
        Map<String, Long> parentBreakdown = new HashMap<>();
        parentBreakdown.put("advance", 1000L);
        parentBreakdown.put("advance_count", 5L);

        List<ProfileResult> children = List.of(child1, child2);
        ProfileResult parent = new ProfileResult("BooleanQuery", "parent query", parentBreakdown, Map.of(), 2000L, children);

        // Convert to proto
        QueryProfile queryProfile = ProfileResultProtoUtils.toQueryProfileProto(parent).build();

        // Verify
        assertEquals("BooleanQuery", queryProfile.getType());
        assertEquals(2, queryProfile.getChildrenCount());
        assertEquals("TermQuery", queryProfile.getChildren(0).getType());
        assertEquals("child1:value", queryProfile.getChildren(0).getDescription());
        assertEquals(500L, queryProfile.getChildren(0).getTimeInNanos());
        assertEquals("TermQuery", queryProfile.getChildren(1).getType());
        assertEquals("child2:value", queryProfile.getChildren(1).getDescription());
        assertEquals(600L, queryProfile.getChildren(1).getTimeInNanos());
    }

    public void testToAggregationProfileProtoBasic() {
        // Create a basic ProfileResult for aggregation
        Map<String, Long> breakdown = new HashMap<>();
        breakdown.put("initialize", 100L);
        breakdown.put("initialize_count", 1L);
        breakdown.put("collect", 200L);
        breakdown.put("collect_count", 2L);
        breakdown.put("build_aggregation", 300L);
        breakdown.put("build_aggregation_count", 3L);

        ProfileResult result = new ProfileResult("TermsAggregator", "terms aggregation", breakdown, Map.of(), 1000L, List.of());

        // Convert to proto
        AggregationProfile aggProfile = ProfileResultProtoUtils.toAggregationProfileProto(result).build();

        // Verify
        assertEquals("TermsAggregator", aggProfile.getType());
        assertEquals("terms aggregation", aggProfile.getDescription());
        assertEquals(1000L, aggProfile.getTimeInNanos());
        assertEquals(100L, aggProfile.getBreakdown().getInitialize());
        assertEquals(1L, aggProfile.getBreakdown().getInitializeCount());
        assertEquals(200L, aggProfile.getBreakdown().getCollect());
        assertEquals(2L, aggProfile.getBreakdown().getCollectCount());
        assertEquals(300L, aggProfile.getBreakdown().getBuildAggregation());
        assertEquals(3L, aggProfile.getBreakdown().getBuildAggregationCount());
    }

    public void testToAggregationProfileProtoWithAllBreakdownFields() {
        // Create ProfileResult with all aggregation breakdown fields
        Map<String, Long> breakdown = new HashMap<>();
        breakdown.put("initialize", 100L);
        breakdown.put("initialize_count", 1L);
        breakdown.put("collect", 200L);
        breakdown.put("collect_count", 2L);
        breakdown.put("build_aggregation", 300L);
        breakdown.put("build_aggregation_count", 3L);
        breakdown.put("build_leaf_collector", 400L);
        breakdown.put("build_leaf_collector_count", 4L);
        breakdown.put("reduce", 500L);
        breakdown.put("reduce_count", 5L);
        breakdown.put("post_collection", 600L);
        breakdown.put("post_collection_count", 6L);

        ProfileResult result = new ProfileResult("CardinalityAggregator", "cardinality agg", breakdown, Map.of(), 2000L, List.of());

        // Convert to proto
        AggregationProfile aggProfile = ProfileResultProtoUtils.toAggregationProfileProto(result).build();

        // Verify all breakdown fields
        assertEquals(100L, aggProfile.getBreakdown().getInitialize());
        assertEquals(1L, aggProfile.getBreakdown().getInitializeCount());
        assertEquals(200L, aggProfile.getBreakdown().getCollect());
        assertEquals(2L, aggProfile.getBreakdown().getCollectCount());
        assertEquals(300L, aggProfile.getBreakdown().getBuildAggregation());
        assertEquals(3L, aggProfile.getBreakdown().getBuildAggregationCount());
        assertEquals(400L, aggProfile.getBreakdown().getBuildLeafCollector());
        assertEquals(4L, aggProfile.getBreakdown().getBuildLeafCollectorCount());
        assertEquals(500L, aggProfile.getBreakdown().getReduce());
        assertEquals(5L, aggProfile.getBreakdown().getReduceCount());
        assertEquals(600L, aggProfile.getBreakdown().getPostCollection());
        assertEquals(6L, aggProfile.getBreakdown().getPostCollectionCount());
    }

    public void testToAggregationProfileProtoWithDebugInfo() {
        // Create ProfileResult with debug info
        Map<String, Long> breakdown = new HashMap<>();
        breakdown.put("collect", 1000L);
        breakdown.put("collect_count", 10L);

        Map<String, Object> debugInfo = new HashMap<>();
        debugInfo.put("segments_with_multi_valued_ords", 5);
        debugInfo.put("collection_strategy", "breadth_first");
        debugInfo.put("segments_with_single_valued_ords", 3);
        debugInfo.put("total_buckets", 100);

        ProfileResult result = new ProfileResult("TermsAggregator", "terms agg", breakdown, debugInfo, 2000L, List.of());

        // Convert to proto
        AggregationProfile aggProfile = ProfileResultProtoUtils.toAggregationProfileProto(result).build();

        // Verify debug info
        assertTrue(aggProfile.getDebug() != null);
        assertEquals(5, aggProfile.getDebug().getSegmentsWithMultiValuedOrds());
        assertEquals("breadth_first", aggProfile.getDebug().getCollectionStrategy());
        assertEquals(3, aggProfile.getDebug().getSegmentsWithSingleValuedOrds());
        assertEquals(100, aggProfile.getDebug().getTotalBuckets());
    }

    public void testToAggregationProfileProtoWithChildren() {
        // Create child ProfileResults
        Map<String, Long> childBreakdown = new HashMap<>();
        childBreakdown.put("collect", 100L);
        childBreakdown.put("collect_count", 1L);

        ProfileResult child1 = new ProfileResult("TermsAggregator", "child1", childBreakdown, Map.of(), 500L, List.of());
        ProfileResult child2 = new ProfileResult("MaxAggregator", "child2", childBreakdown, Map.of(), 600L, List.of());

        // Create parent ProfileResult
        Map<String, Long> parentBreakdown = new HashMap<>();
        parentBreakdown.put("collect", 1000L);
        parentBreakdown.put("collect_count", 5L);

        List<ProfileResult> children = List.of(child1, child2);
        ProfileResult parent = new ProfileResult("GlobalAggregator", "global agg", parentBreakdown, Map.of(), 2000L, children);

        // Convert to proto
        AggregationProfile aggProfile = ProfileResultProtoUtils.toAggregationProfileProto(parent).build();

        // Verify
        assertEquals("GlobalAggregator", aggProfile.getType());
        assertEquals(2, aggProfile.getChildrenCount());
        assertEquals("TermsAggregator", aggProfile.getChildren(0).getType());
        assertEquals("child1", aggProfile.getChildren(0).getDescription());
        assertEquals(500L, aggProfile.getChildren(0).getTimeInNanos());
        assertEquals("MaxAggregator", aggProfile.getChildren(1).getType());
        assertEquals("child2", aggProfile.getChildren(1).getDescription());
        assertEquals(600L, aggProfile.getChildren(1).getTimeInNanos());
    }

    public void testRemoveStartTimeFields() {
        // Create ProfileResult with start_time fields that should be filtered out
        Map<String, Long> breakdown = new HashMap<>();
        breakdown.put("advance", 1000L);
        breakdown.put("advance_start_time", 500L); // Should be removed
        breakdown.put("build_scorer", 2000L);
        breakdown.put("build_scorer_start_time", 600L); // Should be removed

        ProfileResult result = new ProfileResult("TermQuery", "test", breakdown, Map.of(), 5000L, List.of());

        // Convert to proto
        QueryProfile queryProfile = ProfileResultProtoUtils.toQueryProfileProto(result).build();

        // Verify start_time fields are not in the breakdown
        // The proto should only have the non-start-time fields
        assertEquals(1000L, queryProfile.getBreakdown().getAdvance());
        assertEquals(2000L, queryProfile.getBreakdown().getBuildScorer());
        // Start time fields should not be mapped to any proto fields, so we can't verify their absence directly
        // but we can verify that only the expected fields are present with correct values
    }

    public void testEmptyChildren() {
        // Create ProfileResult with empty children
        Map<String, Long> breakdown = new HashMap<>();
        breakdown.put("advance", 1000L);

        ProfileResult result = new ProfileResult("TermQuery", "test", breakdown, Map.of(), 1000L, List.of());

        // Convert to proto
        QueryProfile queryProfile = ProfileResultProtoUtils.toQueryProfileProto(result).build();

        // Verify
        assertEquals(0, queryProfile.getChildrenCount());
    }
}
