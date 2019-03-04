package com.github.sftwnd.crayfish.examples.akka.aggregator;

import akka.actor.UntypedAbstractActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ashindarev on 10.03.17.
 */
public class InfoActor extends UntypedAbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(InfoActor.class);

    @Override
    public void onReceive(Object message) throws Throwable {
        logger.info("MESSAGE RECEIVED: [{}]", message);
    }

}
