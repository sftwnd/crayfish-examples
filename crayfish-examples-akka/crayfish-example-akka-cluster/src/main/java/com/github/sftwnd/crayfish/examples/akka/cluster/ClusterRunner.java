package com.github.sftwnd.crayfish.examples.akka.cluster;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.singleton.ClusterSingletonManager;
import akka.cluster.singleton.ClusterSingletonManagerSettings;
import akka.routing.ConsistentHashingPool;
import com.github.sftwnd.crayfish.akka.spring.di.SpringExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Created by ashindarev on 03.03.17.
 */
@Component
//@Profile("rayfish-example-akka-cluster")
//@DependsOn(value = {"crayfish-actorSystem"})
public class ClusterRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ClusterRunner.class);

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    @Qualifier("crayfish-actorSystem")
    ActorSystem actorSystem;

    public static class TestSingletonMessages {
        public static class UnregistrationOk{}
        public static class End{}
        public static class GetCurrent{}

        public static UnregistrationOk unregistrationOk() { return new UnregistrationOk(); }
        public static End end() { return new End(); }
        public static GetCurrent getCurrent() { return new GetCurrent(); }
    }

    @Override
    public void run(String... args) throws Exception {

        final ClusterSingletonManagerSettings settings = ClusterSingletonManagerSettings.create(actorSystem);
        //final ClusterSingletonManagerSettings settings1 = ClusterSingletonManagerSettings.create(actorSystem);

        /*
        ActorRef publisher = actorSystem.actorOf (
                SpringExtension.SpringExtProvider.get(actorSystem).props("crayfish-example-singletonActor" )
                ,"singletonActor"
        );
        */
        ActorRef cache = actorSystem.actorOf(
                new ConsistentHashingPool(3).props(
                        SpringExtension.SpringExtProvider.get(actorSystem).props("crayfish-example-singletonActor" )
                 )
                ,"singletonActor");

        /*

        ActorRef publisher = actorSystem.actorOf (
                ClusterSingletonManager.props(
                        SpringExtension.SpringExtProvider.get(actorSystem).props("crayfish-example-singletonActor" )
                        ,TestSingletonMessages.end()
                        ,settings
                )
                ,"singletonActor"
        );
        */
        /*
        ActorRef publisher = actorSystem.actorOf (
                SpringExtension.SpringExtProvider.get(actorSystem).props("crayfish-example-singletonActor" )
                ,"cluster-singleton-actor"
        );
        */
    }

}



