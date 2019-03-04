package com.sftwnd.crayfish.examples.akka.guarantee.simple;

import akka.actor.UntypedAbstractActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * Created by ashindarev on 02.03.17.
 */
public class MyActor extends UntypedAbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(MyActor.class);

    private CountDownLatch countDownLatch;

    public MyActor(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void onReceive(Object message) throws Exception
    {
        logger.info("MyActor received: {}", message.toString());
        countDownLatch.countDown();
    }

    @Override
    public void preStart() {
        logger.info("MyActor has been started.");
    }

    @Override
    public void postStop() {
        logger.info("MyActor has been stopped.");
    }

}