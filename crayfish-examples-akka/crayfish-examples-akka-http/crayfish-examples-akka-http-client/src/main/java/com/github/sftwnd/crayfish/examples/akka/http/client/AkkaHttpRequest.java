package com.github.sftwnd.crayfish.examples.akka.http.client;


import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class AkkaHttpRequest {

    public static void main(String[] args) throws InterruptedException {
        final ActorSystem system = ActorSystem.create();
        final Materializer materializer = ActorMaterializer.create(system);
        Instant tick = Instant.now();
        final int cnt = 200;
        CountDownLatch cdl = new CountDownLatch(cnt);
        System.out.println(tick.toString());
        AtomicInteger successCnt = new AtomicInteger(0);
        AtomicInteger throwableCnt = new AtomicInteger(0);

        Semaphore semaphore = new Semaphore(32);
        for (long i=0; i < cnt; i++) {
            semaphore.acquire();
            CompletionStage<HttpResponse> responseFuture =
                Http.get(system)
                    .singleRequest(HttpRequest.create("http://google.ru"), materializer)
                    .whenComplete((response, throwable) -> {
                        if (response != null) {
                            successCnt.incrementAndGet();
                        }
                        if (throwable != null) {
                            if (throwableCnt.incrementAndGet() == 1) {
                                System.err.println(throwable.toString());
                            }
                        }
                        semaphore.release();
                        cdl.countDown();
                        System.err.println(cdl.getCount());
                });
        }
        cdl.await();
        Duration d = Duration.between(tick, Instant.now());
        System.out.println(d);
        System.out.println(1000.0d*cnt/d.toMillis());
        System.out.println(successCnt.get());
        System.out.println(throwableCnt.get());
        System.out.println(successCnt.get() + throwableCnt.get());
        system.terminate();
    }

/*
        CompletionStage<HttpResponse> responseFuture =
                Http.get(system)
                        .singleRequest(HttpRequest.create("http://akka.io"), materializer);

        CompletionStage<IOResult> done = responseFuture.thenCompose(response -> {

            Source<ByteString, Object> source = response.entity().getDataBytes();

            // note that it is not safe/correct to create the outputstream outside of the
            // lambda/creator given to fromOutputStream
            Sink<ByteString, CompletionStage<IOResult>> sink =
                    StreamConverters.fromOutputStream(HttpClientEx);

            // just to make the type clear, ofc you can just return it
            CompletionStage<IOResult> completionStage = source.toMat(sink, Keep.right()).run(materializer);

            return completionStage;
        });


        done.thenAccept((result) -> {
            if (result.wasSuccessful())
                System.out.println("Done, wrote " + result.getCount() + " bytes");
            else
                System.out.println("Failed: " + result.getError().getMessage());
        });
        */
}