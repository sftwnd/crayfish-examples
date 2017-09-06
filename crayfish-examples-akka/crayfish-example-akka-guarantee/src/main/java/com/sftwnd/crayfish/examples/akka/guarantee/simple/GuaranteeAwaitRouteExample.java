package com.github.sftwnd.crayfish.examples.akka.guarantee.simple;

import akka.actor.ActorPaths;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.sftwnd.crayfish.akka.utils.ActorSystemTermination;
import com.sftwnd.crayfish.akka.utils.GuaranteeAwaitRoute;
import com.typesafe.config.ConfigFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by ashindarev on 02.03.17.
 */
public class GuaranteeAwaitRouteExample {

    public static void main(String[] args) throws InterruptedException {
        ActorSystem actorSystem = ActorSystem.create("actor-system", ConfigFactory.load("akka.conf"));
        System.out.println(actorSystem.toString());
        CountDownLatch countDownLatch = new CountDownLatch(1);
      //ActorRef guaranteeRouteFSM = actorSystem.actorOf(Props.create(GuaranteeAwaitRoute.class, "OOPS!!!", ActorPaths.fromString("akka://actor-system/user/myActor"), ActorRef.noSender() ));
        GuaranteeAwaitRoute.guarantee(actorSystem, "OOPS!!!", ActorPaths.fromString("akka://actor-system/user/myActor"), ActorRef.noSender());
        Thread.sleep(4000L);
        ActorRef myActor = actorSystem.actorOf(Props.create(MyActor.class, countDownLatch), "myActor");
        countDownLatch.await();
        ActorSystemTermination.terminateAndAwait(actorSystem, 3, TimeUnit.SECONDS);

    }

}
