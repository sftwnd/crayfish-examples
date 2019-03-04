package com.github.sftwnd.crayfish.examples.akka.sequence.distributed;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("sequenceHolderFSM")
@Scope("prototype")
public class SequenceActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(SequenceActor.class);

    private ActorRef rangeHolder;

    public static final Object REQUEST_VALUE = new Object();

    private RangeHolderFSM.Range range = null;

    public SequenceActor(String sequenceName) {
        this.rangeHolder = rangeHolder;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()

              .matchEquals(
                      REQUEST_VALUE
                     ,(event) -> {

                      }
              )

              .build();
    }


}
