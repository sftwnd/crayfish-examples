package com.github.sftwnd.crayfish.examples.akka.deadletter;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import akka.actor.Props;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 *  Загрузчик базового приложения командной строки
 */

public class AkkaDeadletter  {

    private static final Logger logger = LoggerFactory.getLogger(AkkaDeadletter.class);

    public static void main(String[] args) throws InterruptedException {
        ActorSystem actorSystem = ActorSystem.create("actor-system", ConfigFactory.load("akka.conf"));
        CountDownLatch countDownLatch = new CountDownLatch(2);
        ActorRef existingActor = actorSystem.actorOf(Props.create(com.github.sftwnd.crayfish.examples.akka.deadletter.MyActor.class, countDownLatch),"ExistingActor");
        ActorRef DLH = actorSystem.actorOf(Props.create(com.github.sftwnd.crayfish.examples.akka.deadletter.DeadLettersHandler.class, countDownLatch), "DeadLetterHandler");
        actorSystem.eventStream().subscribe(DLH, DeadLetter.class);
        ActorSelection nonExist = actorSystem.actorSelection("akka://user/actorSystem/NonExistingActor");
        existingActor.tell("Hello Akka", existingActor);
        nonExist.tell("Hello Akka", DLH);
        countDownLatch.await();
        final CountDownLatch terminationLatch = new CountDownLatch(1);
        actorSystem.registerOnTermination(new Runnable() {
            @Override
            public void run() {
                terminationLatch.countDown();
            }
        });
        actorSystem.terminate();
        terminationLatch.await();
    }

}
