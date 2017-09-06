package com.github.sftwnd.crayfish.examples.spring.parameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ashindarev on 05.08.16.
 */
@Configuration
@ConfigurationProperties(prefix = "example.spring", ignoreNestedProperties=false)
public class SpringParameters implements BeanFactoryAware {

    private static final Logger logger = LoggerFactory.getLogger(SpringParameters.class);

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    private BeanFactory beanFactory;

    private Map<String, Map<String, String>> params = new HashMap<>();

    public Map<String, Map<String, String>> getParams() {
        return this.params;
    }

    @PostConstruct
    public void configure() {
        logger.info("..... {}", String.valueOf(params.keySet()));
        ConfigurableBeanFactory configurableBeanFactory = (ConfigurableBeanFactory) beanFactory;

    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }


}
