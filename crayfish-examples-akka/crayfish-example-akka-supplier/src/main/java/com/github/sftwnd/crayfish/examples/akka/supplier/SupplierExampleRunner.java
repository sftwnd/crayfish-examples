package com.github.sftwnd.crayfish.examples.akka.supplier;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.pattern.CircuitBreaker;
import akka.pattern.PatternsCS;
import akka.routing.FromConfig;
import akka.util.Timeout;
import com.github.sftwnd.crayfish.akka.pattern.worker.SupplyWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Created by ashindarev on 03.03.17.
 */
@Component
//@Profile("crayfish-example-akka-supplier")
//@DependsOn(value = {"crayfish-actorSystem"})
public class SupplierExampleRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SupplierExampleRunner.class);

    @Autowired
    ActorSystem actorSystem;

    @Override
    public void run(String... args) throws Exception {

        new Thread(new Task(actorSystem)).run();

    }

    public static class Task implements Runnable {


        ActorSystem actorSystem;

        public Task(ActorSystem actorSystem) {
            this.actorSystem = actorSystem;
        }

        @Override
        public void run() {
            try {
                CircuitBreaker circuitBreaker = new CircuitBreaker(
                        actorSystem.dispatcher()
                        , actorSystem.scheduler()
                        , 4
                        , Duration.create(333, "ms")
                        , Duration.create(999, "ms")
                );

                ActorRef supplyActor = actorSystem.actorOf(FromConfig.getInstance().props(Props.create(SupplyWorker.class, circuitBreaker)), "supplier");


                ActorRef printActor = actorSystem.actorOf(Props.create(PrintActor.class), "printer");

                for (String str : new String[]{"Printer", "Asked"}) {
                    logger.info("=== {} >>>", str);
                    long tick = Instant.now().toEpochMilli();
                    while (Instant.now().toEpochMilli() - tick < 15000) {
                        if (printActor != null) {
                            supplyActor.tell(new SupplyTask(), printActor);
                        } else {
                            PatternsCS.ask(supplyActor, new SupplyTask(), new Timeout(5, TimeUnit.SECONDS))
                                    .whenCompleteAsync((msg, ex) -> {
                                        if (ex == null) {
                                            logger.info("1. Message: {}", String.valueOf(msg));
                                        } else {
                                            logger.error("2. Exceptin: {}", String.valueOf(ex));
                                        }
                                    });
                        }
                        Thread.sleep(250L);
                    }
                  //printActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
                    printActor = null;
                }

                CountDownLatch cdl = new CountDownLatch(1);

                logger.info("=== Wait for termination >>>");

                Thread.sleep(3000L);

                actorSystem.registerOnTermination(() -> cdl.countDown());
                actorSystem.terminate();
                cdl.await();
            } catch (InterruptedException iex) {
                logger.error("Task exception", iex);
            }
        }
    }

    public static class PrintActor extends AbstractActor {
        @Override
        public AbstractActor.Receive createReceive() {
            return receiveBuilder()
                  .matchAny (
                       (message) -> logger.info("3. Message: {}", String.valueOf(message))
                   )
                  .build();
        }
    }

    public static class SupplyTask implements Supplier<String> {

        private static long startTick = Instant.now().toEpochMilli();
        private static AtomicLong counter = new AtomicLong(0);

        private long id = counter.incrementAndGet();

        @Override
        public String get() {
            long tick = (Instant.now().toEpochMilli() - startTick) % 10000;
            if (tick < 1500L || (tick > 4500L && tick < 6500L)) {
                throw new RuntimeException("OOPS["+id+"] :( [" + tick + "]");
            }
            return "Success["+id+"] !!!";
        }
    }


}
