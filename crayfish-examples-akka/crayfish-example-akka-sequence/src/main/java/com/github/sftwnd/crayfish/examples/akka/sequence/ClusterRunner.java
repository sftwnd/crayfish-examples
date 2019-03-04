package com.github.sftwnd.crayfish.examples.akka.sequence;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ddata.DistributedData;
import akka.cluster.singleton.ClusterSingletonManager;
import akka.cluster.singleton.ClusterSingletonManagerSettings;
import akka.cluster.singleton.ClusterSingletonProxy;
import akka.cluster.singleton.ClusterSingletonProxySettings;
import akka.japi.pf.ReceiveBuilder;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/**
 * Created by ashindarev on 03.03.17.
 */
@Component
@Profile("crayfish-example-akka-sequence")
//@DependsOn(value = {"crayfish-actorSystem"})
public class ClusterRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ClusterRunner.class);

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

        Thread.sleep(2000);

        logger.info("===================================");


        ActorRef sequenceActorFSMSingleton = actorSystem.actorOf (
                // SpringExtension.SpringExtProvider.get(actorSystem).props("sequenceActorFSM" )
                // SpringExtension.SpringExtProvider.get(actorSystem).props("sequenceActorFSM", 1023L, 5 )
                ClusterSingletonManager.props(
                        SpringExtension.SpringExtProvider.get(actorSystem).props("sequenceActorFSM", BigInteger.valueOf(1023L), 5 )
                       ,PoisonPill.getInstance()
                       ,settings
                )
                ,"sequenceActorFSMSingleton"
        );

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final ActorRef myActor = actorSystem.actorOf(Props.create(MyActor.class));

                    ClusterSingletonProxySettings proxySettings = ClusterSingletonProxySettings.create(actorSystem);
                    ActorRef sequenceActorFSM =
                            actorSystem.actorOf(ClusterSingletonProxy.props("/user/sequenceActorFSMSingleton", proxySettings),
                                    "sequenceActorFSM");
                    while(true) {
                        sequenceActorFSM.tell(SequenceActorFSM.Request.NEXT, myActor);
                        Thread.sleep(100L * ThreadLocalRandom.current().nextInt(45, 75));
                    }
                } catch (InterruptedException iex) {
                    logger.error("Process has been interrupted...", iex);
                }
            }
        }).start();

        Thread.sleep(2000L);

        logger.info("===================================");

        //actorSystem.terminate();

    }

    static class MyActor extends AbstractActor {

        private static final Logger logger = LoggerFactory.getLogger(MyActor.class);

        @Override
        public Receive createReceive() {
            ReceiveBuilder receiveBuilder = new ReceiveBuilder();

            receiveBuilder.matchAny(
                    (msg) -> logger.info(">>> MSG: {}", String.valueOf(msg))
            );

            return receiveBuilder.build();
        }
    }

}



