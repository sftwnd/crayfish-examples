package com.github.sftwnd.crayfish.examples.akka.sequence.cached;

import akka.actor.*;
import akka.cluster.Cluster;
import akka.cluster.ddata.DistributedData;
import akka.cluster.singleton.ClusterSingletonManager;
import akka.cluster.singleton.ClusterSingletonManagerSettings;
import akka.cluster.singleton.ClusterSingletonProxy;
import akka.cluster.singleton.ClusterSingletonProxySettings;
import akka.japi.pf.ReceiveBuilder;
import com.github.sftwnd.crayfish.akka.spring.di.SpringExtension;
import com.github.sftwnd.crayfish.examples.akka.sequence.SequenceActorFSM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by ashindarev on 03.03.17.
 */
@Component
@Profile("crayfish-example-akka-sequence-cached")
@DependsOn(value = {"crayfish-actorSystem"})
public class DistributedRangeRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DistributedRangeRunner.class);

    @Autowired
    @Qualifier("crayfish-actorSystem")
    ActorSystem actorSystem;

    @Override
    public void run(String... args) throws Exception {

        final Cluster        node = Cluster.get(actorSystem);
        final ActorRef replicator = DistributedData.get(actorSystem).replicator();
        final ClusterSingletonManagerSettings settings = ClusterSingletonManagerSettings.create(actorSystem);

        ActorRef distributedRangeActorFSM = actorSystem.actorOf (
                ClusterSingletonManager.props(
                        SpringExtension.SpringExtProvider.get(actorSystem).props("distributedRangeActorFSM", "ccirq-id", BigInteger.valueOf(1250), 100 )
                       ,PoisonPill.getInstance()
                       ,settings
                )
                ,"distributedRangeActorFSM"
        );

        Thread.sleep(2000L);

    }

}



