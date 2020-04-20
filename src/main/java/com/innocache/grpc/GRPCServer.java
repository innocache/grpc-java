package com.innocache.grpc;

import com.innocache.grpc.serviceimpl.GreetingServiceImpl;
import io.grpc.BindableService;
import io.grpc.ServerBuilder;
import io.grpc.Server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.List;

public class GRPCServer {
    private static final Logger logger = Logger.getLogger(GRPCServer.class.getName());

    private final int port;
    private final Server server;

    /**
     * Create a server using serverBuilder with services added.
     */
    public GRPCServer(List<BindableService> services, int port) {
        this.port = port;
        ServerBuilder sb = ServerBuilder.forPort(this.port);
        services.forEach(s -> sb.addService(s));
        server = sb.build();
    }

    /**
     * Start serving requests.
     */
    public void start() throws IOException {
        server.start();
        logger.info("GRPCServer started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    GRPCServer.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
                System.err.println("*** server shut down");
            }
        });
    }

    /**
     * Stop serving requests and shutdown resources.
     */
    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * Main method.  This comment makes the linter happy.
     */
    public static void main(String[] args) throws Exception {
        List<BindableService> services = new ArrayList<>();
        services.add(new GreetingServiceImpl());
        GRPCServer server = new GRPCServer(services,8980);
        server.start();
        server.blockUntilShutdown();
    }
}