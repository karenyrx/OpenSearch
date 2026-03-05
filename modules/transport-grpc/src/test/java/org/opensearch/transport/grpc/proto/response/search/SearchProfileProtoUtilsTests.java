/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.transport.grpc.proto.response.search;

import org.opensearch.protobufs.FetchProfile;
import org.opensearch.protobufs.Profile;
import org.opensearch.protobufs.ShardProfile;
import org.opensearch.search.profile.NetworkTime;
import org.opensearch.search.profile.ProfileResult;
import org.opensearch.search.profile.ProfileShardResult;
import org.opensearch.search.profile.aggregation.AggregationProfileShardResult;
import org.opensearch.search.profile.fetch.FetchProfileShardResult;
import org.opensearch.search.profile.query.CollectorResult;
import org.opensearch.search.profile.query.QueryProfileShardResult;
import org.opensearch.test.OpenSearchTestCase;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchProfileProtoUtilsTests extends OpenSearchTestCase {

    // Tests for FetchProfileShardResultProtoUtils

    public void testFetchProfileToProtoBasic() {
        // Create fetch profile result
        Map<String, Long> breakdown = new HashMap<>();
        breakdown.put("load_source", 100L);
        breakdown.put("load_source_count", 5L);

        Map<String, Object> debugInfo = new HashMap<>();
        debugInfo.put("stored_fields", List.of("field1", "field2"));
        debugInfo.put("fast_path", 1);

        ProfileResult fetchResult = new ProfileResult("FetchPhase", "fetch", breakdown, debugInfo, 1000L, List.of());
        FetchProfileShardResult fetchProfileShardResult = new FetchProfileShardResult(List.of(fetchResult));

        // Convert to proto
        FetchProfile fetchProfile = FetchProfileShardResultProtoUtils.toProto(fetchProfileShardResult).build();

        // Verify
        assertEquals("FetchPhase", fetchProfile.getType());
        assertEquals("fetch", fetchProfile.getDescription());
        assertEquals(1000L, fetchProfile.getTimeInNanos());
        assertEquals(100, fetchProfile.getBreakdown().getLoadSource());
        assertEquals(5, fetchProfile.getBreakdown().getLoadSourceCount());
        assertEquals(2, fetchProfile.getDebug().getStoredFieldsCount());
        assertEquals("field1", fetchProfile.getDebug().getStoredFields(0));
        assertEquals("field2", fetchProfile.getDebug().getStoredFields(1));
        assertEquals(1, fetchProfile.getDebug().getFastPath());
    }

    public void testFetchProfileToProtoWithChildren() {
        // Create child fetch results
        Map<String, Long> childBreakdown = new HashMap<>();
        childBreakdown.put("load_source", 50L);

        ProfileResult child = new ProfileResult("ChildFetch", "child", childBreakdown, Map.of(), 500L, List.of());

        // Create parent fetch result
        Map<String, Long> parentBreakdown = new HashMap<>();
        parentBreakdown.put("load_source", 100L);

        ProfileResult parent = new ProfileResult("ParentFetch", "parent", parentBreakdown, Map.of(), 1000L, List.of(child));
        FetchProfileShardResult fetchProfileShardResult = new FetchProfileShardResult(List.of(parent));

        // Convert to proto
        FetchProfile fetchProfile = FetchProfileShardResultProtoUtils.toProto(fetchProfileShardResult).build();

        // Verify
        assertEquals("ParentFetch", fetchProfile.getType());
        assertEquals(1, fetchProfile.getChildrenCount());
        assertEquals("ChildFetch", fetchProfile.getChildren(0).getType());
        assertEquals("child", fetchProfile.getChildren(0).getDescription());
        assertEquals(500L, fetchProfile.getChildren(0).getTimeInNanos());
    }

    public void testFetchProfileToProtoWithEmptyResults() {
        // Create empty fetch profile result
        FetchProfileShardResult fetchProfileShardResult = new FetchProfileShardResult(Collections.emptyList());

        // Convert to proto
        FetchProfile fetchProfile = FetchProfileShardResultProtoUtils.toProto(fetchProfileShardResult).build();

        // Verify - should return empty/default profile
        assertEquals("", fetchProfile.getType());
        assertEquals("", fetchProfile.getDescription());
        assertEquals(0L, fetchProfile.getTimeInNanos());
    }

    // Tests for ProfileShardResultProtoUtils

    public void testProfileShardResultToProtoBasic() {
        // Create query profile
        Map<String, Long> breakdown = new HashMap<>();
        breakdown.put("advance", 1000L);
        ProfileResult queryProfile = new ProfileResult("TermQuery", "field:value", breakdown, Map.of(), 2000L, List.of());
        CollectorResult collector = new CollectorResult("SimpleCollector", "search_count", 1000L, List.of());
        QueryProfileShardResult queryProfileShardResult = new QueryProfileShardResult(List.of(queryProfile), 500L, collector);

        // Create fetch profile
        ProfileResult fetchResult = new ProfileResult("FetchPhase", "fetch", Map.of(), Map.of(), 500L, List.of());
        FetchProfileShardResult fetchProfileShardResult = new FetchProfileShardResult(List.of(fetchResult));

        // Create aggregation profile (empty)
        AggregationProfileShardResult aggProfileShardResult = new AggregationProfileShardResult(Collections.emptyList());

        // Create network time
        NetworkTime networkTime = new NetworkTime(100L, 200L);

        // Create ProfileShardResult
        ProfileShardResult profileShardResult = new ProfileShardResult(
            List.of(queryProfileShardResult),
            aggProfileShardResult,
            fetchProfileShardResult,
            networkTime
        );

        // Convert to proto
        ShardProfile shardProfile = ProfileShardResultProtoUtils.toProto("[node1][index1][0]", profileShardResult).build();

        // Verify
        assertEquals("[node1][index1][0]", shardProfile.getId());
        assertEquals(1, shardProfile.getSearchesCount());
        assertEquals("TermQuery", shardProfile.getSearches(0).getQuery(0).getType());
        assertEquals("FetchPhase", shardProfile.getFetch().getType());
    }

    public void testProfileShardResultToProtoThrowsExceptionForAggregations() {
        // Create query profile
        Map<String, Long> breakdown = new HashMap<>();
        breakdown.put("collect", 1000L);
        ProfileResult aggProfile = new ProfileResult("TermsAggregator", "terms", breakdown, Map.of(), 2000L, List.of());

        // Create non-empty aggregation profile
        AggregationProfileShardResult aggProfileShardResult = new AggregationProfileShardResult(List.of(aggProfile));

        // Create minimal query and fetch profiles
        ProfileResult queryProfile = new ProfileResult("MatchAll", "*:*", Map.of(), Map.of(), 100L, List.of());
        CollectorResult collector = new CollectorResult("Collector", "search", 100L, List.of());
        QueryProfileShardResult queryProfileShardResult = new QueryProfileShardResult(List.of(queryProfile), 0L, collector);
        FetchProfileShardResult fetchProfileShardResult = new FetchProfileShardResult(Collections.emptyList());
        NetworkTime networkTime = new NetworkTime(0L, 0L);

        // Create ProfileShardResult with aggregations
        ProfileShardResult profileShardResult = new ProfileShardResult(
            List.of(queryProfileShardResult),
            aggProfileShardResult,
            fetchProfileShardResult,
            networkTime
        );

        // Verify exception is thrown
        UnsupportedOperationException exception = expectThrows(
            UnsupportedOperationException.class,
            () -> ProfileShardResultProtoUtils.toProto("[node1][index1][0]", profileShardResult)
        );
        assertEquals("aggregation profile results are not supported in gRPC yet", exception.getMessage());
    }

    public void testProfileShardResultToProtoWithoutFetch() {
        // Create query profile only
        Map<String, Long> breakdown = new HashMap<>();
        breakdown.put("advance", 1000L);
        ProfileResult queryProfile = new ProfileResult("TermQuery", "field:value", breakdown, Map.of(), 2000L, List.of());
        CollectorResult collector = new CollectorResult("SimpleCollector", "search_count", 1000L, List.of());
        QueryProfileShardResult queryProfileShardResult = new QueryProfileShardResult(List.of(queryProfile), 500L, collector);

        // Create empty fetch profile
        FetchProfileShardResult fetchProfileShardResult = new FetchProfileShardResult(Collections.emptyList());

        // Create empty aggregation profile
        AggregationProfileShardResult aggProfileShardResult = new AggregationProfileShardResult(Collections.emptyList());

        // Create network time
        NetworkTime networkTime = new NetworkTime(50L, 100L);

        // Create ProfileShardResult
        ProfileShardResult profileShardResult = new ProfileShardResult(
            List.of(queryProfileShardResult),
            aggProfileShardResult,
            fetchProfileShardResult,
            networkTime
        );

        // Convert to proto
        ShardProfile shardProfile = ProfileShardResultProtoUtils.toProto("[node1][index1][0]", profileShardResult).build();

        // Verify
        assertEquals("[node1][index1][0]", shardProfile.getId());
        assertEquals(1, shardProfile.getSearchesCount());
        // Fetch should not be set for empty fetch results
        assertEquals("", shardProfile.getFetch().getType());
    }

    // Tests for SearchProfileShardResultsProtoUtils

    public void testSearchProfileShardResultsToProtoWithMap() {
        // Create profile results for multiple shards
        Map<String, ProfileShardResult> shardResults = new HashMap<>();

        // Shard 1
        ProfileResult query1 = new ProfileResult("TermQuery", "shard1", Map.of("advance", 1000L), Map.of(), 2000L, List.of());
        CollectorResult collector1 = new CollectorResult("Collector1", "search", 1000L, List.of());
        QueryProfileShardResult queryProfile1 = new QueryProfileShardResult(List.of(query1), 100L, collector1);
        ProfileShardResult shardResult1 = new ProfileShardResult(
            List.of(queryProfile1),
            new AggregationProfileShardResult(Collections.emptyList()),
            new FetchProfileShardResult(Collections.emptyList()),
            new NetworkTime(10L, 20L)
        );
        shardResults.put("[node1][index1][0]", shardResult1);

        // Shard 2
        ProfileResult query2 = new ProfileResult("TermQuery", "shard2", Map.of("advance", 1500L), Map.of(), 2500L, List.of());
        CollectorResult collector2 = new CollectorResult("Collector2", "search", 1200L, List.of());
        QueryProfileShardResult queryProfile2 = new QueryProfileShardResult(List.of(query2), 150L, collector2);
        ProfileShardResult shardResult2 = new ProfileShardResult(
            List.of(queryProfile2),
            new AggregationProfileShardResult(Collections.emptyList()),
            new FetchProfileShardResult(Collections.emptyList()),
            new NetworkTime(15L, 25L)
        );
        shardResults.put("[node1][index1][1]", shardResult2);

        // Convert to proto
        Profile profile = SearchProfileShardResultsProtoUtils.toProto(shardResults);

        // Verify
        assertEquals(2, profile.getShardsCount());
        // Results should be sorted by key
        assertEquals("[node1][index1][0]", profile.getShards(0).getId());
        assertEquals("[node1][index1][1]", profile.getShards(1).getId());
        assertEquals("TermQuery", profile.getShards(0).getSearches(0).getQuery(0).getType());
        assertEquals("shard1", profile.getShards(0).getSearches(0).getQuery(0).getDescription());
        assertEquals("TermQuery", profile.getShards(1).getSearches(0).getQuery(0).getType());
        assertEquals("shard2", profile.getShards(1).getSearches(0).getQuery(0).getDescription());
    }

    public void testSearchProfileShardResultsToProtoWithEmptyMap() {
        // Create empty profile results
        Map<String, ProfileShardResult> shardResults = new HashMap<>();

        // Convert to proto
        Profile profile = SearchProfileShardResultsProtoUtils.toProto(shardResults);

        // Verify
        assertEquals(0, profile.getShardsCount());
    }

    public void testSearchProfileShardResultsToProtoSortedKeys() {
        // Create profile results with keys that need sorting
        Map<String, ProfileShardResult> shardResults = new HashMap<>();

        // Add shards in non-sorted order
        String[] keys = { "[node2][index][0]", "[node1][index][1]", "[node1][index][0]", "[node3][index][0]" };

        for (String key : keys) {
            ProfileResult query = new ProfileResult("TermQuery", key, Map.of(), Map.of(), 1000L, List.of());
            CollectorResult collector = new CollectorResult("Collector", "search", 500L, List.of());
            QueryProfileShardResult queryProfile = new QueryProfileShardResult(List.of(query), 0L, collector);
            ProfileShardResult shardResult = new ProfileShardResult(
                List.of(queryProfile),
                new AggregationProfileShardResult(Collections.emptyList()),
                new FetchProfileShardResult(Collections.emptyList()),
                new NetworkTime(0L, 0L)
            );
            shardResults.put(key, shardResult);
        }

        // Convert to proto
        Profile profile = SearchProfileShardResultsProtoUtils.toProto(shardResults);

        // Verify keys are sorted
        assertEquals(4, profile.getShardsCount());
        assertEquals("[node1][index][0]", profile.getShards(0).getId());
        assertEquals("[node1][index][1]", profile.getShards(1).getId());
        assertEquals("[node2][index][0]", profile.getShards(2).getId());
        assertEquals("[node3][index][0]", profile.getShards(3).getId());
    }
}
