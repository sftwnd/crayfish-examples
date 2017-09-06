package com.github.sftwnd.crayfish.examples.akka.aggregator;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedAbstractActor;
import com.sftwnd.crayfish.akka.pattern.aggregation.AggregatorFSM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by ashindarev on 03.03.17.
 */
@Component
@Profile("crayfish-example-akka-aggregator")
@DependsOn(value = {"crayfish-actorSystem"})
public class AggregatorExampleRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AggregatorExampleRunner.class);

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    ActorSystem actorSystem;

    @Override
    public void run(String... args) throws Exception {

        ActorRef infoActor = actorSystem.actorOf(Props.create(InfoActor.class), "info");
        ActorRef aggregateActor = actorSystem.actorOf(Props.create(AggregatorFSM.class, infoActor, 5, Duration.ofSeconds(2L)), "aggregator");
        CountDownLatch countDownLatch = new CountDownLatch(1);
        ActorRef watchActor = actorSystem.actorOf(Props.create(WatchActor.class, countDownLatch, aggregateActor), "watchActor");
        for (int i=0; i<17; i++) {
            aggregateActor.tell(AggregatorFSM.constructEvent(i+1), ActorRef.noSender());
            Thread.sleep(250L);
        }

        Thread.sleep(2500L);

        for (int i=0; i<9; i++) {
            aggregateActor.tell(AggregatorFSM.constructEvent(i+100), ActorRef.noSender());
            Thread.sleep(750L);
        }

        infoActor.tell(PoisonPill.getInstance(), ActorRef.noSender());

        countDownLatch.await(3, TimeUnit.SECONDS);

        actorSystem.terminate();

    }

    public static class WatchActor extends UntypedAbstractActor {

        private CountDownLatch countDownLatch;

        public WatchActor(CountDownLatch countDownLatch, ActorRef target) {
            this.countDownLatch = countDownLatch;
            context().watch(target);
        }

        @Override
        public void onReceive(Object message) throws Throwable {
            if (message != null && message instanceof Terminated) {
                this.countDownLatch.countDown();
            }
        }

    }

}
