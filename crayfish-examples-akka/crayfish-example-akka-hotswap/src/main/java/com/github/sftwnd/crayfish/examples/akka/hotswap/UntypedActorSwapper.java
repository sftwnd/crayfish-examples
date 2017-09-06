package com.github.sftwnd.crayfish.examples.akka.hotswap;

import akka.actor.AbstractActor;
import akka.actor.UntypedAbstractActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

import static com.github.sftwnd.crayfish.examples.akka.hotswap.UntypedActorSwapper.Swap.SWAP;

/**
 * Created by ashindarev on 25.02.17.
 */

public class UntypedActorSwapper {

    private static final Logger logger = LoggerFactory.getLogger(UntypedActorSwapper.class);

    public static class Swap {
        public static Swap SWAP = new Swap();

        private Swap() {
        }

    }

    @Component
    @Scope("prototype")
    @Profile(value = "crayfish-examples-akka-hotswap")
    public static class Swapper extends UntypedAbstractActor {

        @Autowired
        private CountDownLatch countDownLatch;

        private AbstractActor.Receive hua = receiveBuilder().matchAny(
            m -> {
                logger.info("{}: Hua",getSelf().path().name());
                getContext().unbecome(); // resets the latest 'become'
                countDownLatch.countDown();
            }
        ).build();

        private AbstractActor.Receive ho = receiveBuilder().matchAny(
                m -> {
                    logger.info("{}: Ho",getSelf().path().name());
                    getContext().become(hua, true);
                    countDownLatch.countDown();
                }
        ).build();

        public void onReceive(Object message) {
            try {
                if (message == SWAP) {
                    logger.info("{}: Hi",getSelf().path().name());
                    getContext().become(ho, false); // this signals stacking of the new behavior
                } else {
                    unhandled(message);
                }
            } finally {
                countDownLatch.countDown();
            }
        }
    }

}