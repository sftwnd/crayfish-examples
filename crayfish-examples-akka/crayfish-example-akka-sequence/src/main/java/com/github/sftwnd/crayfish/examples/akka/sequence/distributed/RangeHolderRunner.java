package com.github.sftwnd.crayfish.examples.akka.sequence.distributed;

import akka.actor.*;
import akka.cluster.Cluster;
import akka.cluster.ddata.DistributedData;
import akka.cluster.singleton.ClusterSingletonManager;
import akka.cluster.singleton.ClusterSingletonManagerSettings;
import akka.cluster.singleton.ClusterSingletonProxy;
import akka.cluster.singleton.ClusterSingletonProxySettings;
import akka.dispatch.RequiresMessageQueue;
import akka.dispatch.SingleConsumerOnlyUnboundedMailbox;
import akka.japi.pf.ReceiveBuilder;
import com.github.sftwnd.crayfish.akka.spring.di.SpringExtension;
import com.github.sftwnd.crayfish.examples.akka.sequence.ClusterRunner;
import com.github.sftwnd.crayfish.examples.akka.sequence.SequenceActorFSM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;

import java.math.BigInteger;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by ashindarev on 03.03.17.
 */
@Component
@Profile("crayfish-example-akka-sequence-range")
public class RangeHolderRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(RangeHolderRunner.class);

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    @Qualifier("crayfish-actorSystem")
    ActorSystem actorSystem;

    private static AtomicInteger cnt = new AtomicInteger(0);
    private static int CACHE_SIZE = 1000;
    private static int REQUEST_SIZE = 1;
    private static int LIMIT = 25000;
    private static AtomicLong tick = new AtomicLong(0);

    @Override
    public void run(String... args) throws Exception {

        final Cluster        node = Cluster.get(actorSystem);
        final ActorRef replicator = DistributedData.get(actorSystem).replicator();
        final ClusterSingletonManagerSettings settings = ClusterSingletonManagerSettings.create(actorSystem);

        Thread.sleep(1000);

        actorSystem.actorOf (
                ClusterSingletonManager.props(
                        SpringExtension.SpringExtProvider.get(actorSystem).props("rangeHolderFSM", "ccirq", CACHE_SIZE, BigInteger.valueOf(11)).withDispatcher("prio-dispatcher")
                       ,PoisonPill.getInstance()
                       ,settings
                )
                ,"rangeHolderFSMSingleton"
        );

        ClusterSingletonProxySettings proxySettings = ClusterSingletonProxySettings.create(actorSystem);
        ActorRef rangeHolderFSM = actorSystem.actorOf(ClusterSingletonProxy.props("/user/rangeHolderFSMSingleton", proxySettings), "rangeHolderFSM");
        ActorRef printActor = actorSystem.actorOf(Props.create(PrintActor.class, rangeHolderFSM), "printActor");

        Thread.sleep(2000);

        rangeHolderFSM.tell(new RangeHolderFSM.Request(REQUEST_SIZE), printActor);

    }

    static class PrintActor extends AbstractActor
    //implements RequiresMessageQueue<RangeHolderFSM.AdminPriorityMailboxSemantics>
    {

        private static final Logger logger = LoggerFactory.getLogger(PrintActor.class);

        private ActorRef target;

        public PrintActor(ActorRef target) {
            this.target = target;
        }

        @Override
        public Receive createReceive() {
            ReceiveBuilder receiveBuilder = new ReceiveBuilder();

            receiveBuilder.matchAny(
                    (msg) -> {
                        //logger.info(">>> MSG: {}", String.valueOf(msg));
                        if (msg.getClass().equals(RangeHolderFSM.Empty.class)) {
                            final ActorRef sender = getSender();
                            final ActorRef self = getSelf();
                            getContext().system().scheduler().scheduleOnce(Duration.create(1, TimeUnit.SECONDS),
                                    () -> {
                                        sender.tell(new RangeHolderFSM.Request(REQUEST_SIZE), self);
                                    }, getContext().system().dispatcher());
                        } else if (msg.getClass().equals(RangeHolderFSM.Range.class)) {
                            if (tick.get() == 0L) {
                                synchronized (tick) {
                                    if (tick.get() == 0L) {
                                        tick.set(System.currentTimeMillis());
                                    }
                                }
                            }
                            RangeHolderFSM.Range range = (RangeHolderFSM.Range) msg;
                            int val = cnt.addAndGet(range.getSize());
                            if (val < LIMIT) {
                                target.tell(new RangeHolderFSM.Request(REQUEST_SIZE), getSelf());
                            } else {
                                logger.info(">>> LIMIT: {}", val);
                                long time = -1 * tick.addAndGet(-1 * System.currentTimeMillis());
                                logger.info(">>> SPEED: {}", Math.round(100000.0D * val/time)/100.0D);
                            }
                        }
                    }
            );

            return receiveBuilder.build();
        }

    }

}
