package com.innocache.grpc;

import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.logging.Level;

public class GRPCClient {
    private static final Logger logger = Logger.getLogger(GRPCClient.class.getName());
    public static void main( String[] args ) throws Exception
    {
        // Channel is the abstraction to connect to a service endpoint
        // Let's use plaintext communication because we don't have certs
        final ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:8980")
                .usePlaintext()
                .build();

        // It is up to the client to determine whether to block the call
        // Here we create a blocking stub, but an async stub,
        // or an async stub with Future are always possible.

        /*
        RPCs made through a blocking stub, as the name implies, block until the response from the service is available.
        The blocking stub contains one Java method for each unary and server-streaming method in the service definition.
        Blocking stubs do not support client-streaming or bidirectional-streaming RPCs.
        A new blocking stub is instantiated via the ServiceNameGrpc.newBlockingStub(Channel channel) static method.
         */
        GreetingServiceGrpc.GreetingServiceBlockingStub bstub = GreetingServiceGrpc.newBlockingStub(channel);

        /*
        RPCs made via a future stub wrap the return value of the asynchronous stub in a GrpcFuture<ResponseType>, which implements the com.google.common.util.concurrent.ListenableFuture interface.
        The future stub contains one Java method for each unary method in the service definition. Future stubs do not support streaming calls.
         */
        GreetingServiceGrpc.GreetingServiceFutureStub fstub = GreetingServiceGrpc.newFutureStub(channel);

        /*
        Asynchronous Stub
        RPCs made via an asynchronous stub operate entirely through callbacks on StreamObserver.
        The asynchronous stub contains one Java method for each method from the service definition.
        A new asynchronous stub is instantiated via the ServiceNameGrpc.newStub(Channel channel) static method.
         */
        GreetingServiceGrpc.GreetingServiceStub astub = GreetingServiceGrpc.newStub(channel);

        // Finally, make the simple RPC call using the blocking stub
        GreetingServiceOuterClass.HelloRequest request =
                GreetingServiceOuterClass.HelloRequest.newBuilder()
                        .setName("Sam")
                        .build();
        GreetingServiceOuterClass.HelloResponse response =
                bstub.greeting(request);
        System.out.println("Simple RPC blocking : "+response);

        // Finally, make the simple RPC call using the future stub timeout
        ListenableFuture<GreetingServiceOuterClass.HelloResponse> response1 =
                fstub.greeting(request);
        try{
            System.out.println("Simple RPC async wait 1 sec: "+response1.get(1, TimeUnit.SECONDS));
        }catch(TimeoutException te){
            logger.log(Level.WARNING, "Simple RPC async wait 1 sec failed: {0}", te.getCause());
        }

        // Finally, make the simple RPC call using the future stub not timeout
        ListenableFuture<GreetingServiceOuterClass.HelloResponse> response2 =
                fstub.greeting(request);
        try{
            System.out.println("Simple RPC async wait 2 sec: "+response2.get(2, TimeUnit.SECONDS));
        }catch(TimeoutException te){
            logger.log(Level.WARNING, "Simple RPC async wait 2 sec failed: {0}", te.getCause());
        }

        // Finally, make the Server Stream RPC call using the blocking stub
        Iterator<GreetingServiceOuterClass.HelloResponse> responseSS;
        try {
            responseSS = bstub.greetingServerStream(request);
        } catch (StatusRuntimeException ex) {
            logger.log(Level.WARNING, "RPC failed: {0}", ex.getStatus());
            return;
        }
        responseSS.forEachRemaining(r -> System.out.println("Server Stream RPC blocking : "+r));

        // Finally, make the Server Stream RPC call using the async stub
        final CountDownLatch finishLatch00 = new CountDownLatch(1);
        StreamObserver<GreetingServiceOuterClass.HelloResponse> responseSS2 = new StreamObserver<GreetingServiceOuterClass.HelloResponse>(){

            @Override
            public void onNext(GreetingServiceOuterClass.HelloResponse helloResponse) {
                System.out.println("ServerStream got server response : "+ helloResponse);
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
                Status status = Status.fromThrowable(t);
                logger.log(Level.WARNING, "HelloResponse Failed: {0}", status);
            }

            @Override
            public void onCompleted() {
                //logger.log(Level.INFO,"Finished HelloResponse");
                System.out.println("Server response complete.");
                finishLatch00.countDown();
            }
        };

        try {
            astub.greetingServerStream(request, responseSS2);
        } catch (StatusRuntimeException ex) {
            ex.printStackTrace();
            logger.log(Level.WARNING, "RPC failed: {0}", ex.getStatus());
        }
        finishLatch00.await(10, TimeUnit.SECONDS);


        // Finally, make the Client Stream RPC call using the astub
        final CountDownLatch finishLatch0 = new CountDownLatch(1);
        StreamObserver<GreetingServiceOuterClass.HelloResponse> responseCS1 = new StreamObserver<GreetingServiceOuterClass.HelloResponse>(){

            @Override
            public void onNext(GreetingServiceOuterClass.HelloResponse helloResponse) {
                System.out.println("ClientStream got server response : "+ helloResponse);
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
                Status status = Status.fromThrowable(t);
                logger.log(Level.WARNING, "HelloResponse Failed: {0}", status);
            }

            @Override
            public void onCompleted() {
                //logger.log(Level.INFO,"Finished HelloResponse");
                System.out.println("Server response complete.");
                finishLatch0.countDown();
            }
        };

        List<GreetingServiceOuterClass.HelloRequest> requests = new ArrayList<>();
        requests.add(GreetingServiceOuterClass.HelloRequest.newBuilder()
                .setName("Sam")
                .build());
        requests.add(GreetingServiceOuterClass.HelloRequest.newBuilder()
                .setName("Ben")
                .build());
        requests.add(GreetingServiceOuterClass.HelloRequest.newBuilder()
                .setName("Joe")
                .build());

        StreamObserver<GreetingServiceOuterClass.HelloRequest> requestObserver = astub.greetingClientStream(responseCS1);
        try {
            Random rand = new Random();
            for(GreetingServiceOuterClass.HelloRequest req : requests){
                Thread.sleep(rand.nextInt(1000) + 500);
                requestObserver.onNext(req);
            }
        } catch (RuntimeException ex) {
            logger.log(Level.WARNING, "RPC failed: {0}", ex);
            requestObserver.onError(ex);
            throw ex;
        }
        requestObserver.onCompleted();
        finishLatch0.await(10, TimeUnit.SECONDS);

        // Finally, make the bi-directional Stream RPC call using the astub
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<GreetingServiceOuterClass.HelloRequest> requestObserver1 = astub.greetingBiStream(new StreamObserver<GreetingServiceOuterClass.HelloResponse>(){
            @Override
            public void onNext(GreetingServiceOuterClass.HelloResponse helloResponse) {
                System.out.println("BiStream got server msg : "+ helloResponse);
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
                Status status = Status.fromThrowable(t);
                logger.log(Level.WARNING, "HelloServer Msg Failed: {0}", status);
            }

            @Override
            public void onCompleted() {
                System.out.println("Server msg complete.");
                finishLatch.countDown();
            }
        });

        try {
            Random rand = new Random();
            for(GreetingServiceOuterClass.HelloRequest req : requests){
                Thread.sleep(rand.nextInt(500) + 500);
                requestObserver1.onNext(req);
            }
        } catch (RuntimeException ex) {
            logger.log(Level.WARNING, "RPC failed: {0}", ex);
            requestObserver1.onError(ex);
            throw ex;
        }
        requestObserver1.onCompleted();

        finishLatch.await(20, TimeUnit.SECONDS);

        // A Channel should be shutdown before stopping the process.
        channel.shutdown();
        channel.awaitTermination(2, TimeUnit.SECONDS);
        channel.shutdownNow();
    }
}
