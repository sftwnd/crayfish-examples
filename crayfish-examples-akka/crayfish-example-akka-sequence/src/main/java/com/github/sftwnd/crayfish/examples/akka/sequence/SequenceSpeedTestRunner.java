package com.github.sftwnd.crayfish.examples.akka.sequence;

import akka.actor.*;
import akka.cluster.Cluster;
import akka.cluster.ddata.DistributedData;
import akka.cluster.singleton.ClusterSingletonManager;
import akka.cluster.singleton.ClusterSingletonManagerSettings;
import akka.cluster.singleton.ClusterSingletonProxy;
import akka.cluster.singleton.ClusterSingletonProxySettings;
import com.github.sftwnd.crayfish.akka.spring.di.SpringExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Created by ashindarev on 03.03.17.
 */
@Component
@Profile("crayfish-example-akka-sequence-speed")
//@DependsOn(value = {"crayfish-actorSystem"})
public class SequenceSpeedTestRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SequenceSpeedTestRunner.class);

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    @Qualifier("crayfish-actorSystem")
    ActorSystem actorSystem;

    @Override
    public void run(String... args) throws Exception {

        final Cluster        node = Cluster.get(actorSystem);
        final ActorRef replicator = DistributedData.get(actorSystem).replicator();
        final ClusterSingletonManagerSettings settings = ClusterSingletonManagerSettings.create(actorSystem);

        Thread.sleep(3000);

        ActorRef sequenceActorFSMSingleton = actorSystem.actorOf (
                ClusterSingletonManager.props(
                        SpringExtension.SpringExtProvider.get(actorSystem).props("sequenceActorFSM", BigInteger.valueOf(1023L), 1000 )
                       ,PoisonPill.getInstance()
                       ,settings
                )
                ,"sequenceActorFSMSingleton"
        );

        ClusterSingletonProxySettings proxySettings = ClusterSingletonProxySettings.create(actorSystem);
        ActorRef sequenceActorFSM = actorSystem.actorOf(ClusterSingletonProxy.props("/user/sequenceActorFSMSingleton", proxySettings), "sequenceActorFSM");

        final int cnt = 50000;
        final int actorCnt = 2;
        final CountDownLatch prerunCountdownLatch = new CountDownLatch(actorCnt);
        final CountDownLatch runCountdownLatch    = new CountDownLatch(1);
        final CountDownLatch startCountdownLatch  = new CountDownLatch(actorCnt);
        final CountDownLatch countdownLatch       = new CountDownLatch(cnt);

        IntStream.range(0, actorCnt).forEach(
            id -> {
                new Thread(() -> {
                    try {
                        logger.info("Sequence reader #{} has been runned", id);
                        final ActorRef speedActor = actorSystem.actorOf(Props.create(SpeedActor.class, startCountdownLatch, countdownLatch), "speedActor_"+id);
                        prerunCountdownLatch.countDown();
                        logger.info("Sequence reader #{} has been initialized [{}]", id, speedActor);
                        runCountdownLatch.await();
                        logger.info("Sequence reader #{} has been started", id);
                        IntStream.range(0, (cnt + actorCnt - 1)/actorCnt + 100).forEach(i -> sequenceActorFSM.tell(SequenceActorFSM.Request.NEXT, speedActor));
                        countdownLatch.await();
                    } catch (InterruptedException iex) {
                        logger.error("Exception: {}", iex);
                    }
                }).start();
            }
        );

        prerunCountdownLatch.await();
        long tick = Instant.now().toEpochMilli();
        runCountdownLatch.countDown();
        startCountdownLatch.await();
        countdownLatch.await();
        tick = Instant.now().toEpochMilli() - tick;
        logger.warn("SPEED: {}", 1000.0*cnt/tick);
        actorSystem.terminate();

    }

    static class SpeedActor extends AbstractActor {

        private CountDownLatch countDownLatch;
        private CountDownLatch startCountDownLatch;

        public SpeedActor(CountDownLatch startCountDownLatch, CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
            this.startCountDownLatch = startCountDownLatch;
            logger.info("SpeedActor {} has been created", self());
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
           .match(
                BigInteger.class
               ,(val) -> {
                    if (startCountDownLatch != null) {
                        startCountDownLatch.countDown();
                        startCountDownLatch = null;
                        logger.info("StartCountDownLatch has been decreased");
                    }
                    countDownLatch.countDown();
                }
            )
           .matchAny(
                (msg) -> logger.info(">>> MSG: {}", String.valueOf(msg))
            )
           .build();
        }

    }

}



