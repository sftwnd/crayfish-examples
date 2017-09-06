package com.github.sftwnd.crayfish.examples.feign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *  Загрузчик базового приложения командной строки
 */

@Component
@Profile(value = "crayfish-example-feign")
public class FeignRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(FeignRunner.class);

    //@Autowired
    //GitHubExample.GitHub gitHub;

    public static final int CNT = 100;

    @Override
    public void run(String... args) throws Exception {
      //logger.info("{} has been started. {} has been connected", this.getClass().getSimpleName(), gitHub);
      //GitHubExample.process(gitHub);
        CountDownLatch cnt = new CountDownLatch(CNT);
        AtomicInteger  reqs = new AtomicInteger(CNT);
        CountDownLatch countDownLatch = new CountDownLatch(32);
        for (long i=countDownLatch.getCount(); i>0; i--) {
            new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
                            countDownLatch.countDown();
                            try {
                                countDownLatch.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            GoogleRuPage.GoogleRu googleRu = GoogleRuPage.GoogleRu.connect();
                            while (reqs.decrementAndGet() >= 0) {
                                try {
                                    String page = googleRu.page();
                                } finally {
                                    cnt.countDown();
                                    logger.info("CNT: "+cnt.getCount());
                                }
                            }
                        }
                    }
            ).start();
        }
        countDownLatch.await();
        Instant instant = Instant.now();
        cnt.await();
        logger.info("Duration: {}", String.valueOf(Duration.between(instant, Instant.now())));

    }



}
