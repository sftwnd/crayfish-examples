package com.github.sftwnd.crayfish.examples.feign.authorization;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@EnableFeignClients("com.github.sftwnd.crayfish.examples.feign.authorization")
@EnableCircuitBreaker
public class BisBrtBalanceRequestRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(BisBrtBalanceRequestRunner.class);

    @Autowired
    ApplicationContext applicationContext;

    @Override
    public void run(String... args) throws Exception {

        BisBrtBalanceService bisBrtBalanceService = applicationContext.getBean(BisBrtBalanceService.class);

        try {
            logger.info("\n\nbis-brt-balance-service: {}\n", bisBrtBalanceService.balances("1515", 200));
        } catch (Exception ex) {
            logger.error("Exception...", ex);
        }

    }

    @Bean
    ErrorDecoder getErrorDecoder() {
        return new ErrorDecoder.Default() {
            @Override
            public Exception decode(String methodKey, Response response) {
                byte[] buff = new byte[response.body().length()];
                try {
                    response.body().asInputStream().read(buff);
                } catch (IOException ioex) {

                }
                if (response.status() == 404) {
                    logger.error(
                            "Оййй. 404-я ошибка :( `{}`"
                           ,new String(buff)
                    );
                }
                return super.decode(methodKey, response);
            }

        };
    }

}