/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.transport.grpc.proto.response.search;

import org.opensearch.protobufs.Collector;
import org.opensearch.search.profile.query.CollectorResult;
import org.opensearch.test.OpenSearchTestCase;

import java.util.ArrayList;
import java.util.List;

public class CollectorResultProtoUtilsTests extends OpenSearchTestCase {

    public void testToProtoBasic() {
        // Create a basic CollectorResult
        CollectorResult collector = new CollectorResult("SimpleCollector", "search_count", 1000L, List.of());

        // Convert to proto
        Collector protoCollector = CollectorResultProtoUtils.toProto(collector).build();

        // Verify
        assertEquals("SimpleCollector", protoCollector.getName());
        assertEquals("search_count", protoCollector.getReason());
        assertEquals(1000L, protoCollector.getTimeInNanos());
        assertEquals(0, protoCollector.getChildrenCount());
    }

    public void testToProtoWithChildren() {
        // Create child collectors
        CollectorResult child1 = new CollectorResult("ChildCollector1", "search_top_hits", 500L, List.of());
        CollectorResult child2 = new CollectorResult("ChildCollector2", "search_min_score", 300L, List.of());

        List<CollectorResult> children = List.of(child1, child2);

        // Create parent collector
        CollectorResult parent = new CollectorResult("ParentCollector", "search_multi", 1000L, children);

        // Convert to proto
        Collector protoCollector = CollectorResultProtoUtils.toProto(parent).build();

        // Verify
        assertEquals("ParentCollector", protoCollector.getName());
        assertEquals("search_multi", protoCollector.getReason());
        assertEquals(1000L, protoCollector.getTimeInNanos());
        assertEquals(2, protoCollector.getChildrenCount());
        assertEquals("ChildCollector1", protoCollector.getChildren(0).getName());
        assertEquals("search_top_hits", protoCollector.getChildren(0).getReason());
        assertEquals(500L, protoCollector.getChildren(0).getTimeInNanos());
        assertEquals("ChildCollector2", protoCollector.getChildren(1).getName());
        assertEquals("search_min_score", protoCollector.getChildren(1).getReason());
        assertEquals(300L, protoCollector.getChildren(1).getTimeInNanos());
    }

    public void testToProtoWithNestedChildren() {
        // Create nested hierarchy
        CollectorResult grandchild = new CollectorResult("GrandchildCollector", "aggregation", 100L, List.of());
        CollectorResult child = new CollectorResult("ChildCollector", "search_post_filter", 300L, List.of(grandchild));
        CollectorResult parent = new CollectorResult("ParentCollector", "search_count", 500L, List.of(child));

        // Convert to proto
        Collector protoCollector = CollectorResultProtoUtils.toProto(parent).build();

        // Verify nested structure
        assertEquals("ParentCollector", protoCollector.getName());
        assertEquals(1, protoCollector.getChildrenCount());
        assertEquals("ChildCollector", protoCollector.getChildren(0).getName());
        assertEquals(1, protoCollector.getChildren(0).getChildrenCount());
        assertEquals("GrandchildCollector", protoCollector.getChildren(0).getChildren(0).getName());
        assertEquals("aggregation", protoCollector.getChildren(0).getChildren(0).getReason());
        assertEquals(100L, protoCollector.getChildren(0).getChildren(0).getTimeInNanos());
    }

    public void testToProtoWithDifferentReasons() {
        // Test all standard collector reasons
        String[] reasons = {
            CollectorResult.REASON_SEARCH_COUNT,
            CollectorResult.REASON_SEARCH_TOP_HITS,
            CollectorResult.REASON_SEARCH_TERMINATE_AFTER_COUNT,
            CollectorResult.REASON_SEARCH_POST_FILTER,
            CollectorResult.REASON_SEARCH_MIN_SCORE,
            CollectorResult.REASON_SEARCH_MULTI,
            CollectorResult.REASON_AGGREGATION,
            CollectorResult.REASON_AGGREGATION_GLOBAL };

        for (String reason : reasons) {
            CollectorResult collector = new CollectorResult("TestCollector", reason, 1000L, List.of());
            Collector protoCollector = CollectorResultProtoUtils.toProto(collector).build();
            assertEquals(reason, protoCollector.getReason());
        }
    }

    public void testToProtoWithEmptyChildren() {
        // Create collector with explicitly empty children list
        List<CollectorResult> emptyChildren = new ArrayList<>();
        CollectorResult collector = new CollectorResult("TestCollector", "search_count", 1000L, emptyChildren);

        // Convert to proto
        Collector protoCollector = CollectorResultProtoUtils.toProto(collector).build();

        // Verify
        assertEquals(0, protoCollector.getChildrenCount());
    }
}
