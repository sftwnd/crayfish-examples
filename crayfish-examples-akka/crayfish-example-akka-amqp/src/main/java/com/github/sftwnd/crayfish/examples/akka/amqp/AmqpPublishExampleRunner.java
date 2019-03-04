package com.github.sftwnd.crayfish.examples.akka.amqp;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.github.sftwnd.crayfish.akka.spring.di.SpringExtension;
import com.github.sftwnd.crayfish.amqp.publish.AMQPPublish;
import com.github.sftwnd.crayfish.amqp.publish.AMQPPublishData;
import com.github.sftwnd.crayfish.amqp.publish.AMQPPublishTag;
import com.rabbitmq.client.AMQP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Created by ashindarev on 03.03.17.
 */
@Component
@Profile("crayfish-example-akka-amqp-publish")
@DependsOn(value = {"crayfish-actorSystem"})
public class AmqpPublishExampleRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AmqpPublishExampleRunner.class);

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    @Qualifier("crayfish-actorSystem")
    ActorSystem actorSystem;

    @Override
    public void run(String... args) throws Exception {

        ActorRef publisher = actorSystem.actorOf (
                SpringExtension.SpringExtProvider.get(actorSystem).props(
                        "crayfish-SimpleAmqpPublishActor","spp"
                )
                ,"akka-amqp-publish"
        );

        AMQP.BasicProperties basicProperties = new AMQP.BasicProperties.Builder()
                                                  .contentEncoding("application/text")
                                                  .deliveryMode(2)
                                                  .build();

        Thread.sleep(2000L);

        publisher.tell(
                new DefaultAMQPPublish (
                        new AMQPPublishTag("ps.pay_spp_bis_adapter", "ps.pay_add.200.999.1")
                       ,new AMQPPublishData(basicProperties, "OOPS!!!".getBytes())
                )
               ,ActorRef.noSender()
        );

        Thread.sleep(2000L);
        System.exit(0);
    }

}



class DefaultAMQPPublish implements AMQPPublish {

    private AMQPPublishTag tag;
    private AMQPPublishData data;

    public DefaultAMQPPublish(AMQPPublishTag tag, AMQPPublishData data) {
        this.tag = tag;
        this.data = data;
    }

    @Override
    public AMQPPublishTag getTag() {
        return tag;
    }

    @Override
    public AMQPPublishData getPayload() {
        return data;
    }
}