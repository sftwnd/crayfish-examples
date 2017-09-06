package com.github.sftwnd.crayfish.examples.spring.parameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class SpringParametersRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SpringParametersRunner.class);

     @Autowired
    ApplicationContext applicationContext;

    @Override
    public void run(String... args) throws Exception {

        logger.info("Loaded parameters: {}", applicationContext.getBean(SpringParameters.class).getParams());

    }

}