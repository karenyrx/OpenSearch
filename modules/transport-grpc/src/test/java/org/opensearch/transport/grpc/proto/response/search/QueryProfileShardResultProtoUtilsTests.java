/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.transport.grpc.proto.response.search;

import org.opensearch.protobufs.SearchProfile;
import org.opensearch.search.profile.ProfileResult;
import org.opensearch.search.profile.query.CollectorResult;
import org.opensearch.search.profile.query.QueryProfileShardResult;
import org.opensearch.test.OpenSearchTestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryProfileShardResultProtoUtilsTests extends OpenSearchTestCase {

    public void testToProtoBasic() {
        // Create profile results
        Map<String, Long> breakdown = new HashMap<>();
        breakdown.put("advance", 1000L);
        breakdown.put("build_scorer", 2000L);

        ProfileResult queryProfile = new ProfileResult("TermQuery", "field:value", breakdown, Map.of(), 5000L, List.of());

        List<ProfileResult> queryProfileResults = List.of(queryProfile);

        // Create collector result
        CollectorResult collector = new CollectorResult("SimpleCollector", "search_count", 3000L, List.of());

        // Create QueryProfileShardResult
        QueryProfileShardResult queryProfileShardResult = new QueryProfileShardResult(queryProfileResults, 1000L, collector);

        // Convert to proto
        SearchProfile searchProfile = QueryProfileShardResultProtoUtils.toProto(queryProfileShardResult).build();

        // Verify
        assertEquals(1, searchProfile.getQueryCount());
        assertEquals("TermQuery", searchProfile.getQuery(0).getType());
        assertEquals("field:value", searchProfile.getQuery(0).getDescription());
        assertEquals(5000L, searchProfile.getQuery(0).getTimeInNanos());
        assertEquals(1000L, searchProfile.getRewriteTime());
        assertEquals(1, searchProfile.getCollectorCount());
        assertEquals("SimpleCollector", searchProfile.getCollector(0).getName());
        assertEquals("search_count", searchProfile.getCollector(0).getReason());
        assertEquals(3000L, searchProfile.getCollector(0).getTimeInNanos());
    }

    public void testToProtoWithMultipleQueries() {
        // Create multiple query profile results
        Map<String, Long> breakdown1 = new HashMap<>();
        breakdown1.put("advance", 1000L);
        ProfileResult query1 = new ProfileResult("TermQuery", "field1:value1", breakdown1, Map.of(), 2000L, List.of());

        Map<String, Long> breakdown2 = new HashMap<>();
        breakdown2.put("advance", 1500L);
        ProfileResult query2 = new ProfileResult("TermQuery", "field2:value2", breakdown2, Map.of(), 2500L, List.of());

        List<ProfileResult> queryProfileResults = List.of(query1, query2);

        // Create collector
        CollectorResult collector = new CollectorResult("MultiCollector", "search_multi", 5000L, List.of());

        // Create QueryProfileShardResult
        QueryProfileShardResult queryProfileShardResult = new QueryProfileShardResult(queryProfileResults, 500L, collector);

        // Convert to proto
        SearchProfile searchProfile = QueryProfileShardResultProtoUtils.toProto(queryProfileShardResult).build();

        // Verify
        assertEquals(2, searchProfile.getQueryCount());
        assertEquals("TermQuery", searchProfile.getQuery(0).getType());
        assertEquals("field1:value1", searchProfile.getQuery(0).getDescription());
        assertEquals(2000L, searchProfile.getQuery(0).getTimeInNanos());
        assertEquals("TermQuery", searchProfile.getQuery(1).getType());
        assertEquals("field2:value2", searchProfile.getQuery(1).getDescription());
        assertEquals(2500L, searchProfile.getQuery(1).getTimeInNanos());
        assertEquals(500L, searchProfile.getRewriteTime());
    }

    public void testToProtoWithCollectorHierarchy() {
        // Create query profile
        Map<String, Long> breakdown = new HashMap<>();
        breakdown.put("advance", 1000L);
        ProfileResult queryProfile = new ProfileResult("BooleanQuery", "bool query", breakdown, Map.of(), 3000L, List.of());

        // Create collector hierarchy
        CollectorResult childCollector = new CollectorResult("ChildCollector", "aggregation", 500L, List.of());
        CollectorResult parentCollector = new CollectorResult("ParentCollector", "search_top_hits", 1500L, List.of(childCollector));

        // Create QueryProfileShardResult
        QueryProfileShardResult queryProfileShardResult = new QueryProfileShardResult(List.of(queryProfile), 200L, parentCollector);

        // Convert to proto
        SearchProfile searchProfile = QueryProfileShardResultProtoUtils.toProto(queryProfileShardResult).build();

        // Verify collector hierarchy
        assertEquals(1, searchProfile.getCollectorCount());
        assertEquals("ParentCollector", searchProfile.getCollector(0).getName());
        assertEquals(1, searchProfile.getCollector(0).getChildrenCount());
        assertEquals("ChildCollector", searchProfile.getCollector(0).getChildren(0).getName());
        assertEquals("aggregation", searchProfile.getCollector(0).getChildren(0).getReason());
    }

    public void testToProtoWithQueryChildren() {
        // Create child query profiles
        Map<String, Long> childBreakdown = new HashMap<>();
        childBreakdown.put("advance", 100L);
        ProfileResult child1 = new ProfileResult("TermQuery", "child1", childBreakdown, Map.of(), 500L, List.of());
        ProfileResult child2 = new ProfileResult("TermQuery", "child2", childBreakdown, Map.of(), 600L, List.of());

        // Create parent query profile with children
        Map<String, Long> parentBreakdown = new HashMap<>();
        parentBreakdown.put("advance", 1000L);
        ProfileResult parent = new ProfileResult("BooleanQuery", "parent", parentBreakdown, Map.of(), 2000L, List.of(child1, child2));

        // Create collector
        CollectorResult collector = new CollectorResult("TestCollector", "search_count", 1000L, List.of());

        // Create QueryProfileShardResult
        QueryProfileShardResult queryProfileShardResult = new QueryProfileShardResult(List.of(parent), 100L, collector);

        // Convert to proto
        SearchProfile searchProfile = QueryProfileShardResultProtoUtils.toProto(queryProfileShardResult).build();

        // Verify query children
        assertEquals(1, searchProfile.getQueryCount());
        assertEquals(2, searchProfile.getQuery(0).getChildrenCount());
        assertEquals("TermQuery", searchProfile.getQuery(0).getChildren(0).getType());
        assertEquals("child1", searchProfile.getQuery(0).getChildren(0).getDescription());
        assertEquals("TermQuery", searchProfile.getQuery(0).getChildren(1).getType());
        assertEquals("child2", searchProfile.getQuery(0).getChildren(1).getDescription());
    }

    public void testToProtoWithZeroRewriteTime() {
        // Create simple query profile
        Map<String, Long> breakdown = new HashMap<>();
        breakdown.put("advance", 500L);
        ProfileResult queryProfile = new ProfileResult("MatchAllQuery", "*:*", breakdown, Map.of(), 1000L, List.of());

        // Create collector
        CollectorResult collector = new CollectorResult("SimpleCollector", "search_count", 800L, List.of());

        // Create QueryProfileShardResult with zero rewrite time
        QueryProfileShardResult queryProfileShardResult = new QueryProfileShardResult(List.of(queryProfile), 0L, collector);

        // Convert to proto
        SearchProfile searchProfile = QueryProfileShardResultProtoUtils.toProto(queryProfileShardResult).build();

        // Verify
        assertEquals(0L, searchProfile.getRewriteTime());
    }
}
