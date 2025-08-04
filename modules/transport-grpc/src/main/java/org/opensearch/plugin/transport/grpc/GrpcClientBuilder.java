/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.transport.grpc;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;

/**
 * Builder for {@link GrpcClient} instances.
 * <p>
 * Provides a fluent API for configuring gRPC client settings.
 * <p>
 * Example usage:
 * <pre>
 * GrpcClient client = GrpcClient.builder("localhost", 9400)
 *     .build();
 * </pre>
 *
 * @opensearch.internal
 */
public class GrpcClientBuilder {

    private final List<InetSocketAddress> nodes;

    /**
     * Package-private constructor. Use {@link GrpcClient#builder} instead.
     */
    GrpcClientBuilder(List<InetSocketAddress> nodes) {
        this.nodes = Objects.requireNonNull(nodes, "Nodes cannot be null");
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("At least one node must be provided");
        }
    }

    /**
     * Build the {@link GrpcClient} with the configured settings.
     *
     * @return a new GrpcClient instance
     */
    public GrpcClient build() {
        return new GrpcClient(nodes);
    }
}
