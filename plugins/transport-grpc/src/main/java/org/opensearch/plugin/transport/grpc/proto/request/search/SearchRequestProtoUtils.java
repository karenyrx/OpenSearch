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
import org.opensearch.ExceptionsHelper;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.action.search.SearchContextId;
import org.opensearch.action.support.IndicesOptions;
import org.opensearch.core.common.io.stream.NamedWriteableRegistry;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.plugin.transport.grpc.proto.request.common.FetchSourceContextProtoUtils;
import org.opensearch.plugin.transport.grpc.proto.request.search.suggest.TermSuggestionBuilderProtoUtils;
import org.opensearch.protobufs.SearchRequest;
import org.opensearch.protobufs.SearchRequestBody;
import org.opensearch.protobufs.TrackHits;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.search.RestSearchAction;
import org.opensearch.search.Scroll;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.fetch.StoredFieldsContext;
import org.opensearch.search.fetch.subphase.FetchSourceContext;
import org.opensearch.search.internal.SearchContext;
import org.opensearch.search.sort.SortOrder;
import org.opensearch.search.suggest.SuggestBuilder;
import org.opensearch.transport.client.Client;
import org.opensearch.transport.client.node.NodeClient;

import java.io.IOException;
import java.util.function.IntConsumer;

import static org.opensearch.action.ValidateActions.addValidationError;
import static org.opensearch.common.unit.TimeValue.parseTimeValue;
import static org.opensearch.search.suggest.SuggestBuilders.termSuggestion;

public class SearchRequestProtoUtils {
    protected static Logger logger = LogManager.getLogger(SearchRequestProtoUtils.class);

    private SearchRequestProtoUtils() {
        // Utility class, no instances
    }

    /**
     * Prepare the request for execution.
     * <p>
     * Similar to {@link RestSearchAction#prepareRequest(RestRequest, NodeClient)} ()}
     * Please ensure to keep both implementations consistent.
     *
     * @param request the request to execute
     * @param client
     * @return the action to execute
     * @throws IOException if an I/O exception occurred parsing the request and preparing for
     *                     execution
     */
    public static org.opensearch.action.search.SearchRequest prepareRequest(SearchRequest request, Client client) throws IOException {
        org.opensearch.action.search.SearchRequest searchRequest = new org.opensearch.action.search.SearchRequest();

        /*
         * We have to pull out the call to `source().size(size)` because
         * _update_by_query and _delete_by_query uses this same parsing
         * path but sets a different variable when it sees the `size`
         * url parameter.
         *
         * Note that we can't use `searchRequest.source()::size` because
         * `searchRequest.source()` is null right now. We don't have to
         * guard against it being null in the IntConsumer because it can't
         * be null later. If that is confusing to you then you are in good
         * company.
         */
        IntConsumer setSize = size -> searchRequest.source().size(size);
        // TODO avoid hidden cast to NodeClient here
        parseSearchRequest(searchRequest, request, ((NodeClient) client).getNamedWriteableRegistry(), setSize);
        return searchRequest;
    }

    /**
     * Parses a protobuf {@link org.opensearch.protobufs.SearchRequest} to a {@link org.opensearch.action.search.SearchRequest}.
     * This method is similar to the logic in {@link RestSearchAction#parseSearchRequest(org.opensearch.action.search.SearchRequest, RestRequest, XContentParser, NamedWriteableRegistry, IntConsumer)}
     * Specifically, this method handles the URL parameters, and internally calls {@link SearchSourceBuilderProtoUtils#parseProto(SearchSourceBuilder, SearchRequestBody)}
     *
     */
    public static void parseSearchRequest(
        org.opensearch.action.search.SearchRequest searchRequest,
        org.opensearch.protobufs.SearchRequest request,
        NamedWriteableRegistry namedWriteableRegistry,
        IntConsumer setSize
    ) throws IOException {
        if (searchRequest.source() == null) {
            searchRequest.source(new SearchSourceBuilder());
        }

        String[] indexArr = new String[request.getIndexCount()];
        for (int i = 0; i < request.getIndexCount(); i++) {
            indexArr[i] = request.getIndex(i);
        }
        searchRequest.indices(indexArr);

        // TODO check
        SearchSourceBuilderProtoUtils.parseProto(searchRequest.source(), request.getRequestBody());

        final int batchedReduceSize = request.hasBatchedReduceSize()
            ? request.getBatchedReduceSize()
            : searchRequest.getBatchedReduceSize();
        searchRequest.setBatchedReduceSize(batchedReduceSize);

        if (request.hasPreFilterShardSize()) {
            searchRequest.setPreFilterShardSize(request.getPreFilterShardSize());
        }

        if (request.hasMaxConcurrentShardRequests()) {
            // only set if we have the parameter since we auto adjust the max concurrency on the coordinator
            // based on the number of nodes in the cluster
            searchRequest.setMaxConcurrentShardRequests(request.getMaxConcurrentShardRequests());
        }

        if (request.hasAllowPartialSearchResults()) {
            // only set if we have the parameter passed to override the cluster-level default
            searchRequest.allowPartialSearchResults(request.getAllowPartialSearchResults());
        }
        if (request.hasPhaseTook()) {
            // only set if we have the parameter passed to override the cluster-level default
            // else phaseTook = null
            searchRequest.setPhaseTook(request.getPhaseTook());
        }
        // do not allow 'query_and_fetch' or 'dfs_query_and_fetch' search types
        // from the REST layer. these modes are an internal optimization and should
        // not be specified explicitly by the user.
        // if (request.hasSearchType()) {
        SearchRequest.SearchType searchType = request.getSearchType();
        if (SearchRequest.SearchType.SEARCH_TYPE_QUERY_THEN_FETCH.equals(searchType)
            || SearchRequest.SearchType.SEARCH_TYPE_DFS_QUERY_THEN_FETCH.equals(searchType)) {
            throw new IllegalArgumentException("Unsupported search type [" + searchType + "]");
        } else {
            searchRequest.searchType(SearchTypeProtoUtils.fromProto(request));
        }
        // }
        // System.out.println("before parseSearchSource = " + searchRequest.source());
        parseSearchSource(searchRequest.source(), request, setSize);
        // System.out.println("after parseSearchSource = " + searchRequest.source());

        if (request.hasRequestCache()) {
            searchRequest.requestCache(request.getRequestCache());
        }

        if (request.hasScroll()) {
            searchRequest.scroll(new Scroll(parseTimeValue(request.getScroll(), null, "scroll")));
        }

        searchRequest.routing(request.getRoutingCount() > 0 ? request.getRoutingList().toArray(new String[0]) : null);
        searchRequest.preference(request.hasPreference() ? request.getPreference() : null);
        searchRequest.indicesOptions(IndicesOptionsProtoUtils.fromRequest(request, searchRequest.indicesOptions()));
        searchRequest.pipeline(request.hasSearchPipeline() ? request.getSearchPipeline() : searchRequest.source().pipeline());

        // TODO test this
        System.out.println("===before checkProtoTotalHits = " + searchRequest.source());
        checkProtoTotalHits(request, searchRequest);
        System.out.println("===after checkProtoTotalHits = " + searchRequest.source());

        // TODO what does this line do?
        // request.paramAsBoolean(INCLUDE_NAMED_QUERIES_SCORE_PARAM, false);

        if (searchRequest.pointInTimeBuilder() != null) {
            preparePointInTime(searchRequest, request, namedWriteableRegistry);
        } else {
            searchRequest.setCcsMinimizeRoundtrips(
                request.hasCcsMinimizeRoundtrips() ? request.getCcsMinimizeRoundtrips() : searchRequest.isCcsMinimizeRoundtrips()
            );
        }

        searchRequest.setCancelAfterTimeInterval(
            request.hasCancelAfterTimeInterval()
                ? parseTimeValue(request.getCancelAfterTimeInterval(), null, "cancel_after_time_interval")
                : null
        );
    }

    /**
     * Similar to {@link RestSearchAction#parseSearchSource(SearchSourceBuilder, RestRequest, IntConsumer)}
     */
    private static void parseSearchSource(
        final SearchSourceBuilder searchSourceBuilder,
        org.opensearch.protobufs.SearchRequest request,
        IntConsumer setSize
    ) {
        QueryBuilder queryBuilder = ProtoActionsProtoUtils.urlParamsToQueryBuilder(request);
        if (queryBuilder != null) {
            searchSourceBuilder.query(queryBuilder);
        }
        if (request.hasFrom()) {
            searchSourceBuilder.from(request.getFrom());
        }
        if (request.hasSize()) {
            setSize.accept(request.getSize());
        }

        if (request.hasExplain()) {
            searchSourceBuilder.explain(request.getExplain());
        }

        if (request.hasVersion()) {
            searchSourceBuilder.version(request.getVersion());
        }

        if (request.hasSeqNoPrimaryTerm()) {
            searchSourceBuilder.seqNoAndPrimaryTerm(request.getSeqNoPrimaryTerm());
        }

        if (request.hasTimeout()) {
            searchSourceBuilder.timeout(parseTimeValue(request.getTimeout(), null, "timeout"));
        }

        if (request.hasVerbosePipeline()) {
            searchSourceBuilder.verbosePipeline(request.getVerbosePipeline());
        }

        if (request.hasTerminateAfter()) {
            int terminateAfter = request.getTerminateAfter();
            if (terminateAfter < 0) {
                throw new IllegalArgumentException("terminateAfter must be > 0");
            } else if (terminateAfter > 0) {
                searchSourceBuilder.terminateAfter(terminateAfter);
            }
        }
        StoredFieldsContext storedFieldsContext = StoredFieldsContextProtoUtils.fromProtoRequest(request);
        if (storedFieldsContext != null) {
            searchSourceBuilder.storedFields(storedFieldsContext);
        }
        if (request.getDocvalueFieldsCount() > 0) {
            for (String field : request.getDocvalueFieldsList()) {
                searchSourceBuilder.docValueField(field, null);
            }
        }
        FetchSourceContext fetchSourceContext = FetchSourceContextProtoUtils.parseFromProtoRequest(request);
        if (fetchSourceContext != null) {
            searchSourceBuilder.fetchSource(fetchSourceContext);
        }

        if (request.hasTrackScores()) {
            searchSourceBuilder.trackScores(request.getTrackScores());
        }

        if (request.hasIncludeNamedQueriesScore()) {
            searchSourceBuilder.includeNamedQueriesScores(request.getIncludeNamedQueriesScore());
        }

        if (request.hasTrackTotalHits()) {
            if (request.getTrackTotalHits().getTrackHitsCase() == TrackHits.TrackHitsCase.BOOL_VALUE) {
                searchSourceBuilder.trackTotalHits(request.getTrackTotalHits().getBoolValue());
            } else if (request.getTrackTotalHits().getTrackHitsCase() == TrackHits.TrackHitsCase.INT32_VALUE) {
                searchSourceBuilder.trackTotalHitsUpTo(request.getTrackTotalHits().getInt32Value());
            }
        }

        if (request.getSortCount() > 0) {
            for (SearchRequest.SortOrder sort : request.getSortList()) {
                String sortField = sort.getField();

                if (sort.hasDirection()) {
                    SearchRequest.SortOrder.Direction direction = sort.getDirection();
                    switch (direction) {
                        case DIRECTION_ASC:
                            searchSourceBuilder.sort(sortField, SortOrder.ASC);
                            break;
                        case DIRECTION_DESC:
                            searchSourceBuilder.sort(sortField, SortOrder.DESC);
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported sort direction " + direction.toString());
                    }
                } else {
                    searchSourceBuilder.sort(sortField);
                }
            }
        }

        if (request.getStatsCount() > 0) {
            searchSourceBuilder.stats(request.getStatsList());
        }

        if (request.hasSuggestField()) {
            String suggestField = request.getSuggestField();
            String suggestText = request.hasSuggestText() ? request.getSuggestText() : request.getQ();
            int suggestSize = request.hasSuggestSize() ? request.getSuggestSize() : 5;
            SearchRequest.SuggestMode suggestMode = request.getSuggestMode();
            searchSourceBuilder.suggest(
                new SuggestBuilder().addSuggestion(
                    suggestField,
                    termSuggestion(suggestField).text(suggestText)
                        .size(suggestSize)
                        .suggestMode(TermSuggestionBuilderProtoUtils.resolve(suggestMode))
                )
            );
        }
    }

    /**
     * Similar to{@link RestSearchAction#preparePointInTime(org.opensearch.action.search.SearchRequest, RestRequest, NamedWriteableRegistry)}
     */
    static void preparePointInTime(
        org.opensearch.action.search.SearchRequest request,
        org.opensearch.protobufs.SearchRequest protoRequest,
        NamedWriteableRegistry namedWriteableRegistry
    ) {
        assert request.pointInTimeBuilder() != null;
        ActionRequestValidationException validationException = null;
        if (request.indices().length > 0) {
            validationException = addValidationError("[indices] cannot be used with point in time", validationException);
        }
        if (request.indicesOptions() != org.opensearch.action.search.SearchRequest.DEFAULT_INDICES_OPTIONS) {
            validationException = addValidationError("[indicesOptions] cannot be used with point in time", validationException);
        }
        if (request.routing() != null) {
            validationException = addValidationError("[routing] cannot be used with point in time", validationException);
        }
        if (request.preference() != null) {
            validationException = addValidationError("[preference] cannot be used with point in time", validationException);
        }
        if (protoRequest.hasCcsMinimizeRoundtrips() && protoRequest.getCcsMinimizeRoundtrips()) {
            validationException = addValidationError("[ccs_minimize_roundtrips] cannot be used with point in time", validationException);
            request.setCcsMinimizeRoundtrips(false);
        }
        ExceptionsHelper.reThrowIfNotNull(validationException);

        final IndicesOptions indicesOptions = request.indicesOptions();
        final IndicesOptions stricterIndicesOptions = IndicesOptions.fromOptions(
            indicesOptions.ignoreUnavailable(),
            indicesOptions.allowNoIndices(),
            false,
            false,
            false,
            true,
            true,
            indicesOptions.ignoreThrottled()
        );
        request.indicesOptions(stricterIndicesOptions);
        final SearchContextId searchContextId = SearchContextId.decode(namedWriteableRegistry, request.pointInTimeBuilder().getId());
        request.indices(searchContextId.getActualIndices());
    }

    /**
     * Keep implementation consistent with {@link RestSearchAction#checkRestTotalHits(RestRequest, org.opensearch.action.search.SearchRequest)}
     */
    public static void checkProtoTotalHits(SearchRequest protoRequest, org.opensearch.action.search.SearchRequest searchRequest) {

        boolean totalHitsAsInt = protoRequest.hasRestTotalHitsAsInt() ? protoRequest.getRestTotalHitsAsInt() : false;
        if (totalHitsAsInt == false) {
            return;
        }
        if (searchRequest.source() == null) {
            searchRequest.source(new SearchSourceBuilder());
        }
        Integer trackTotalHitsUpTo = searchRequest.source().trackTotalHitsUpTo();
        if (trackTotalHitsUpTo == null) {
            searchRequest.source().trackTotalHits(true);
        } else if (trackTotalHitsUpTo != SearchContext.TRACK_TOTAL_HITS_ACCURATE
            && trackTotalHitsUpTo != SearchContext.TRACK_TOTAL_HITS_DISABLED) {
                throw new IllegalArgumentException(
                    "["
                        + "rest_total_hits_as_int"
                        + "] cannot be used "
                        + "if the tracking of total hits is not accurate, got "
                        + trackTotalHitsUpTo
                );
            }
    }
}
