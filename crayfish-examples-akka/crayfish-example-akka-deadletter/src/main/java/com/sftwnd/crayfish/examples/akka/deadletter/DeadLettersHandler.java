package com.github.sftwnd.crayfish.examples.akka.deadletter;

import akka.actor.UntypedAbstractActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * Created by ashindarev on 02.03.17.
 */
public class DeadLettersHandler extends UntypedAbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(DeadLettersHandler.class);

    private CountDownLatch countDownLatch;

    public DeadLettersHandler(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void onReceive(Object deadLetter) throws Exception
    {
        logger.info("DeadLettersHandler received: {}",deadLetter.toString());
        countDownLatch.countDown();
    }
}
