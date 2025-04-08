package org.opensearch.plugin.transport.grpc.proto.request.search.sort;

import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.protobufs.FieldWithOrderMap;
import org.opensearch.protobufs.ScoreSort;
import org.opensearch.search.sort.FieldSortBuilder;
import org.opensearch.search.sort.SortBuilder;
import org.opensearch.search.sort.SortOrder;

import java.util.List;
import java.util.Map;

import static org.opensearch.plugin.transport.grpc.proto.request.search.sort.SortBuilderProtoUtils.fieldOrScoreSort;

public class FieldSortBuilderProtoUtils {
    private FieldSortBuilderProtoUtils() {
        // Utility class, no instances
    }

    /**
     * Similar to {@link FieldSortBuilder#fromXContent(XContentParser, String)}
     *
     * @param sortBuilder
     * @param fieldWithOrderMap
     */
    public static void fromProto(List<SortBuilder<?>> sortBuilder, FieldWithOrderMap fieldWithOrderMap) {
        for (Map.Entry<String, ScoreSort> entry : fieldWithOrderMap.getFieldWithOrderMapMap().entrySet()) {

            String fieldName = entry.getKey();
            ScoreSort scoreSort = entry.getValue();

            SortOrder order = SortOrderProtoUtils.fromProto(scoreSort.getOrder());

            sortBuilder.add(fieldOrScoreSort(fieldName).order(order));
        }
    }
}
