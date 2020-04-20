package com.innocache.grpc.serviceimpl;
import com.innocache.grpc.GRPCServer;
import com.innocache.grpc.GreetingServiceGrpc;
import com.innocache.grpc.GreetingServiceOuterClass;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GreetingServiceImpl extends GreetingServiceGrpc.GreetingServiceImplBase {
    private static final Logger logger = Logger.getLogger(GreetingServiceImpl.class.getName());
    @Override
    public void greeting(GreetingServiceOuterClass.HelloRequest request,
                         StreamObserver<GreetingServiceOuterClass.HelloResponse> responseObserver) {
        // You must use a builder to construct a new Protobuffer object
        int sl = new Random().nextInt(1000) + 500;
        System.out.println("greeting got Request with response delay "+sl+" millisec: " + request);
        GreetingServiceOuterClass.HelloResponse response = GreetingServiceOuterClass.HelloResponse.newBuilder()
                .setGreeting("Hello there, " + request.getName())
                .build();
        try {
            Thread.sleep(sl);
        }catch (InterruptedException e) {}

        // Use responseObserver to send a single response back
        responseObserver.onNext(response);

        // When you are done, you must call onCompleted.
        responseObserver.onCompleted();
    }

    @Override
    public void greetingServerStream(GreetingServiceOuterClass.HelloRequest request, StreamObserver<GreetingServiceOuterClass.HelloResponse> responseObserver) {
        System.out.println("greetingServerStream got Request : " + request);
        List<GreetingServiceOuterClass.HelloResponse> responses = new ArrayList<>();

        responses.add(GreetingServiceOuterClass.HelloResponse.newBuilder()
                .setGreeting("Hello there, " + request.getName())
                .build());
        responses.add(GreetingServiceOuterClass.HelloResponse.newBuilder()
                .setGreeting("How are you?, " + request.getName())
                .build());
        responses.add(GreetingServiceOuterClass.HelloResponse.newBuilder()
                .setGreeting("Long time no see, " + request.getName())
                .build());
        responses.forEach(r -> responseObserver.onNext(r));
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<GreetingServiceOuterClass.HelloRequest> greetingClientStream(StreamObserver<GreetingServiceOuterClass.HelloResponse> responseObserver) {
        return new StreamObserver<GreetingServiceOuterClass.HelloRequest>(){
            List<String> names = new ArrayList<>();

            @Override
            public void onNext(GreetingServiceOuterClass.HelloRequest helloRequest) {
                System.out.println("greetingClientStream got Request : " + helloRequest);
                names.add(helloRequest.getName());
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
                Status status = Status.fromThrowable(t);
                logger.log(Level.WARNING, "greetingClientStream request Failed: {0}", status);
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(GreetingServiceOuterClass.HelloResponse.newBuilder().setGreeting("greetingClientStream - Hello "+String.join(", ", names)).build());
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<GreetingServiceOuterClass.HelloRequest> greetingBiStream(StreamObserver<GreetingServiceOuterClass.HelloResponse> responseObserver) {
        return new StreamObserver<GreetingServiceOuterClass.HelloRequest>() {
            @Override
            public void onNext(GreetingServiceOuterClass.HelloRequest helloRequest) {
                System.out.println("greetingBiStream got Request : " + helloRequest);
                responseObserver.onNext(GreetingServiceOuterClass.HelloResponse.newBuilder().setGreeting("greetingBiStream - Hello "+helloRequest.getName()).build());
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
                Status status = Status.fromThrowable(t);
                logger.log(Level.WARNING, "greetingBiStream request Failed: {0}", status);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }
}
