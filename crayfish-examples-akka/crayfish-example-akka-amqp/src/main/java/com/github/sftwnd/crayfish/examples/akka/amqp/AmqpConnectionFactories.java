package com.github.sftwnd.crayfish.examples.akka.amqp;

import com.github.sftwnd.crayfish.spring.amqp.AmqpConnectionFactoriesConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Created by ashindarev on 05.08.16.
 */
@Configuration(value = "crayfish-example-akka-amqp")
// @ConfigurationProperties(prefix = "crayfish-example-akka-amqp", ignoreNestedProperties=false)
@ConfigurationProperties(prefix = "crayfish-example-akka-amqp")
public class AmqpConnectionFactories extends AmqpConnectionFactoriesConfiguration {

}
