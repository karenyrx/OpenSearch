/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.transport.grpc;

import com.google.protobuf.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.protobufs.services.DocumentServiceGrpc;
import org.opensearch.protobufs.services.SearchServiceGrpc;
import org.opensearch.test.OpenSearchTestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.grpc.health.v1.HealthGrpc;
import io.grpc.stub.StreamObserver;

/**
 * Superclass for tests that interact with an external test cluster using gRPC transport.
 * Similar to OpenSearchRestTestCase but for gRPC protocol.
 *
 * @opensearch.internal
 */
public abstract class OpenSearchGRPCTestCase extends OpenSearchTestCase {

    protected static final Logger logger = LogManager.getLogger(OpenSearchGRPCTestCase.class);

    public static final String GRPC_HOST_PROPERTY = "tests.grpc.cluster";

    /**
     * A gRPC client for the running OpenSearch cluster
     */
    private static GrpcClient grpcClient;

    /**
     * Convert a gRPC protobuf message to its string representation for testing.
     */
    public static String messageToString(Message message) {
        return message.toString();
    }

    /**
     * Verify that a gRPC stream observer received a successful response.
     */
    public static <T> T awaitGrpcResponse(StreamObserver<T> observer, long timeoutSeconds) throws InterruptedException {
        if (observer instanceof TestStreamObserver) {
            TestStreamObserver<T> testObserver = (TestStreamObserver<T>) observer;
            testObserver.await(timeoutSeconds, TimeUnit.SECONDS);
            if (testObserver.getError() != null) {
                throw new AssertionError("gRPC call failed", testObserver.getError());
            }
            return testObserver.getResponse();
        }
        throw new IllegalArgumentException("Observer must be a TestStreamObserver");
    }

    /**
     * Create a test stream observer that can be used to wait for async gRPC responses.
     */
    public static <T> TestStreamObserver<T> createTestStreamObserver() {
        return new TestStreamObserver<>();
    }

    /**
     * Get the gRPC client instance for test subclasses to use.
     * Similar to client() in OpenSearchRestTestCase.
     */
    protected static GrpcClient grpcClient() {
        if (grpcClient == null) {
            throw new IllegalStateException("gRPC client not initialized. Call super.initGrpcClient() in @Before method");
        }
        return grpcClient;
    }

    /**
     * Convenience method to get the blocking search service stub.
     */
    protected static SearchServiceGrpc.SearchServiceBlockingStub searchStub() {
        return grpcClient().searchStub();
    }

    /**
     * Convenience method to get the async search service stub.
     */
    protected static SearchServiceGrpc.SearchServiceStub searchAsyncStub() {
        return grpcClient().searchAsyncStub();
    }

    /**
     * Convenience method to get the blocking document service stub.
     */
    protected static DocumentServiceGrpc.DocumentServiceBlockingStub documentStub() {
        return grpcClient().documentStub();
    }

    /**
     * Convenience method to get the async document service stub.
     */
    protected static DocumentServiceGrpc.DocumentServiceStub documentAsyncStub() {
        return grpcClient().documentAsyncStub();
    }

    /**
     * Convenience method to get the health service stub.
     */
    protected static HealthGrpc.HealthBlockingStub healthStub() {
        return grpcClient().healthStub();
    }

    /**
     * Get a random gRPC server address from the configured cluster.
     */
    protected InetSocketAddress randomGrpcAddress() {
        List<InetSocketAddress> nodes = grpcClient().getNodes();
        return randomFrom(nodes);
    }

    @Before
    public void initGrpcClient() throws IOException {
        if (grpcClient == null) {
            grpcClient = buildGrpcClient();
        }
        assertNotNull("gRPC client should be initialized", grpcClient);
        assertNotNull("Search stub should be available", grpcClient.searchStub());
        assertNotNull("Document stub should be available", grpcClient.documentStub());
        assertNotNull("Health stub should be available", grpcClient.healthStub());
    }

    private GrpcClient buildGrpcClient() throws IOException {
        String cluster = getTestGrpcCluster();
        String[] addresses = cluster.split(",");
        List<InetSocketAddress> hosts = new ArrayList<>(addresses.length);

        for (String address : addresses) {
            int portSeparator = address.lastIndexOf(':');
            if (portSeparator < 0) {
                throw new IllegalArgumentException("Illegal gRPC cluster address [" + address + "]");
            }
            String host = address.substring(0, portSeparator);
            int port = Integer.parseInt(address.substring(portSeparator + 1));
            hosts.add(new InetSocketAddress(host, port));
        }

        logger.info("Building gRPC client for addresses: {}", hosts);

        GrpcClient client = GrpcClient.builder(hosts).build();

        // Verify connectivity
        if (!client.isHealthy()) {
            logger.warn("gRPC cluster health check failed - cluster may not be ready");
        }

        return client;
    }

    protected String getTestGrpcCluster() {
        String cluster = System.getProperty(GRPC_HOST_PROPERTY);
        if (cluster == null) {
            throw new RuntimeException(
                "Must specify ["
                    + GRPC_HOST_PROPERTY
                    + "] system property with a comma delimited list of [host:port] "
                    + "to which to send gRPC requests"
            );
        }
        return cluster;
    }

    @After
    public void cleanUpCluster() throws Exception {
        // Cleanup logic can be added here similar to OpenSearchRestTestCase
        if (preserveClusterUponCompletion() == false) {
            // Add any gRPC-specific cleanup logic here
            logger.debug("Cleaning up gRPC test cluster");
        }
    }

    @AfterClass
    public static void closeGrpcClients() throws IOException {
        try {
            if (grpcClient != null) {
                logger.info("Closing gRPC client");
                grpcClient.close();
            }
        } catch (Exception e) {
            logger.warn("Error closing gRPC client", e);
        } finally {
            grpcClient = null;
        }
    }

    /**
     * Whether to preserve the cluster state upon completion of the test.
     * Override this method to return true to preserve the cluster state.
     */
    protected boolean preserveClusterUponCompletion() {
        return false;
    }

    /**
     * A test implementation of StreamObserver that can be used for testing async gRPC calls.
     */
    public static class TestStreamObserver<T> implements StreamObserver<T> {
        private final CountDownLatch latch = new CountDownLatch(1);
        private final AtomicReference<T> response = new AtomicReference<>();
        private final AtomicReference<Throwable> error = new AtomicReference<>();

        @Override
        public void onNext(T value) {
            response.set(value);
        }

        @Override
        public void onError(Throwable t) {
            error.set(t);
            latch.countDown();
        }

        @Override
        public void onCompleted() {
            latch.countDown();
        }

        public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }

        public T getResponse() {
            return response.get();
        }

        public Throwable getError() {
            return error.get();
        }
    }
}
