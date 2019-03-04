package com.github.sftwnd.crayfish.examples.feign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 *  Загрузчик базового приложения командной строки
 */

@Component
@Profile(value = "crayfish-example-feign1")
//@ComponentScan(basePackages = "com.github.sftwnd.crayfish.examples.feign")
@EnableFeignClients
public class FeignRunner1 implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(FeignRunner1.class);

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    FeignRunner1Client feignRunner1Client;

    public static final int CNT = 100;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Page: {}", feignRunner1Client.page());

    }



}
