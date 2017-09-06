package com.github.sftwnd.crayfish.examples.akka.hotswap;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.sftwnd.crayfish.akka.spring.di.SpringExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *  Загрузчик базового приложения командной строки
 */

@Component
@Profile(value = "crayfish-examples-akka-hotswap")
public class AkkaHotswapRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AkkaHotswapRunner.class);

    @Autowired
    ApplicationContext applicationContext;

    @Bean
    CountDownLatch getCountDownLatch() {
        return new CountDownLatch(6);
    }

    @Override
    public void run(String... args) throws Exception {

        ActorSystem actorSystem = applicationContext.getBean(ActorSystem.class);
        logger.info("AKKA System: {}", actorSystem);

        ActorRef swaps[] = new ActorRef[] {
                actorSystem.actorOf(SpringExtension.SpringExtProvider.get(actorSystem).props("untypedActorSwapper.Swapper"), "hotSwap1")
               ,actorSystem.actorOf(SpringExtension.SpringExtProvider.get(actorSystem).props("untypedActorSwapper.Swapper"), "hotSwap2")
        };

        for (int i=0; i<6; i++) {
            for (ActorRef swap:swaps) {
                swap.tell(UntypedActorSwapper.Swap.SWAP, ActorRef.noSender()); // logs Hi, Ho, Hua...
            }
        }

        CountDownLatch countDownLatch = applicationContext.getBean(CountDownLatch.class);
        countDownLatch.await(10, TimeUnit.SECONDS);
        actorSystem.terminate();

    }

}
