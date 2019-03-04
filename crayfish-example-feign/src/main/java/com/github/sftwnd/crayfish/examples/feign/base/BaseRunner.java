package com.github.sftwnd.crayfish.examples.feign.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 *  Загрузчик базового приложения командной строки
 */

@Component
@Profile(value = "feign-base-client")
@EnableFeignClients("com.github.sftwnd.crayfish.examples.feign.base")
public class BaseRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(BaseRunner.class);

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    BaseClient client;

    @Override
    public void run(String... args) throws Exception {
        //BaseClient client = applicationContext.getBean(BaseClient.class);
        logger.info("Page: {}", client.page());
    }

}
