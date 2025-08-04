/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.transport.grpc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.protobufs.services.DocumentServiceGrpc;
import org.opensearch.protobufs.services.SearchServiceGrpc;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;

/**
 * Client that connects to an OpenSearch cluster through gRPC.
 * <p>
 * Similar to RestClient but for gRPC transport. Handles connection management,
 * load balancing, failover, and service stub creation.
 * <p>
 * Must be created using {@link GrpcClientBuilder}. The hosts that are part of
 * the cluster need to be provided at creation time.
 * <p>
  * Provides both synchronous and asynchronous service stubs for:
 * - SearchService (search operations)
 * - DocumentService (document operations)
 * - HealthService (cluster health checks)
 * <p>
 * Connection management includes health monitoring and basic node selection.
 *
 * @opensearch.internal
 */
public class GrpcClient implements Closeable {

    private static final Logger logger = LogManager.getLogger(GrpcClient.class);

    private final List<InetSocketAddress> nodes;
    private final ManagedChannel channel;
    private final AtomicInteger lastNodeIndex = new AtomicInteger(0);

    // Service stubs (cached for reuse)
    private volatile SearchServiceGrpc.SearchServiceBlockingStub searchStub;
    private volatile SearchServiceGrpc.SearchServiceStub searchAsyncStub;
    private volatile DocumentServiceGrpc.DocumentServiceBlockingStub documentStub;
    private volatile DocumentServiceGrpc.DocumentServiceStub documentAsyncStub;
    private volatile HealthGrpc.HealthBlockingStub healthStub;

    /**
    * Package-private constructor. Use {@link GrpcClientBuilder} instead.
    */
    GrpcClient(List<InetSocketAddress> nodes) {
        this.nodes = Collections.unmodifiableList(nodes);

        // For now, connect to first node. Future: implement load balancing
        InetSocketAddress primaryNode = nodes.get(0);
        this.channel = createChannel(primaryNode);

        // Initialize service stubs
        initializeServiceStubs();

        logger.info("gRPC client initialized for nodes: {}", nodes);
    }

    /**
     * Creates a new {@link GrpcClientBuilder} to help with {@link GrpcClient} creation.
     */
    public static GrpcClientBuilder builder(List<InetSocketAddress> nodes) {
        return new GrpcClientBuilder(nodes);
    }

    /**
     * Creates a new {@link GrpcClientBuilder} for a single node.
     */
    public static GrpcClientBuilder builder(String host, int port) {
        return new GrpcClientBuilder(Collections.singletonList(new InetSocketAddress(host, port)));
    }

    /**
     * Get the blocking search service stub.
     */
    public SearchServiceGrpc.SearchServiceBlockingStub searchStub() {
        return searchStub;
    }

    /**
     * Get the async search service stub.
     */
    public SearchServiceGrpc.SearchServiceStub searchAsyncStub() {
        return searchAsyncStub;
    }

    /**
     * Get the blocking document service stub.
     */
    public DocumentServiceGrpc.DocumentServiceBlockingStub documentStub() {
        return documentStub;
    }

    /**
     * Get the async document service stub.
     */
    public DocumentServiceGrpc.DocumentServiceStub documentAsyncStub() {
        return documentAsyncStub;
    }

    /**
     * Get the health service stub.
     */
    public HealthGrpc.HealthBlockingStub healthStub() {
        return healthStub;
    }

    /**
     * Get the underlying gRPC channel.
     */
    public ManagedChannel getChannel() {
        return channel;
    }

    /**
     * Get the list of configured cluster nodes.
     */
    public List<InetSocketAddress> getNodes() {
        return nodes;
    }

    /**
     * Perform a health check against the cluster.
     * @return true if cluster is healthy, false otherwise
     */
    public boolean isHealthy() {
        try {
            HealthCheckResponse response = healthStub.check(HealthCheckRequest.getDefaultInstance());
            return response.getStatus() == HealthCheckResponse.ServingStatus.SERVING;
        } catch (Exception e) {
            logger.debug("Health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get the current connectivity state of the underlying channel.
     */
    public ConnectivityState getConnectivityState() {
        return channel.getState(false);
    }

    /**
     * Get a random node from the configured cluster for load balancing.
     * Future enhancement: implement sophisticated load balancing.
     */
    public InetSocketAddress getRandomNode() {
        if (nodes.isEmpty()) {
            throw new IllegalStateException("No nodes configured");
        }
        int index = lastNodeIndex.getAndIncrement() % nodes.size();
        return nodes.get(index);
    }

    private ManagedChannel createChannel(InetSocketAddress address) {
        NettyChannelBuilder builder = NettyChannelBuilder.forAddress(address.getHostString(), address.getPort()).usePlaintext();

        return builder.build();
    }

    private void initializeServiceStubs() {
        this.searchStub = SearchServiceGrpc.newBlockingStub(channel);
        this.searchAsyncStub = SearchServiceGrpc.newStub(channel);
        this.documentStub = DocumentServiceGrpc.newBlockingStub(channel);
        this.documentAsyncStub = DocumentServiceGrpc.newStub(channel);
        this.healthStub = HealthGrpc.newBlockingStub(channel);
    }

    @Override
    public void close() throws IOException {
        if (channel != null && !channel.isShutdown()) {
            logger.info("Shutting down gRPC client");
            channel.shutdown();

            try {
                if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warn("gRPC channel did not terminate gracefully, forcing shutdown");
                    channel.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                channel.shutdownNow();
                throw new IOException("Interrupted while shutting down gRPC client", e);
            }
        }
    }
}
