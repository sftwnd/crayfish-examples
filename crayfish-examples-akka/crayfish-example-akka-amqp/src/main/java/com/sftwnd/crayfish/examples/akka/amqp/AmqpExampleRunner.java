package com.github.sftwnd.crayfish.examples.akka.amqp;

import akka.actor.ActorSystem;
import com.github.sftwnd.crayfish.embedded.amqp.qpid.EmbeddedMessageBroker;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.ShutdownSignalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by ashindarev on 03.03.17.
 */
@Component
@Profile("crayfish-example-akka-amqp")
@DependsOn(value = {"crayfish-actorSystem"})
public class AmqpExampleRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AmqpExampleRunner.class);

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    ActorSystem actorSystem;

    @Value("${com.github.sftwnd.crayfish.amqp.local.port:5672}")
    private int port;

    private volatile EmbeddedMessageBroker embeddedMessageBroker = null;

    @Override
    public void run(String... args) throws Exception {

        embeddedMessageBroker = new EmbeddedMessageBroker(AMQP.PROTOCOL.PORT);

        final CountDownLatch startLatch = new CountDownLatch(1);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    embeddedMessageBroker.startUp();
                    logger.info("QPID Broker has been started.");
                } catch (Exception e) {
                    logger.error("Unable to start QPID Broker", e);
                } finally {
                    startLatch.countDown();
                }
            }
        }).start();

        startLatch.await(15, TimeUnit.SECONDS);

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setAutomaticRecoveryEnabled(true);
        connectionFactory.setTopologyRecoveryEnabled(true);

        try (Connection conn = connectionFactory.newConnection()) {
            logger.info("Connected: {}", conn);
            Channel channel = conn.createChannel();
            try {
                String queueName = "queue";
                channel.queueDeclare(queueName, true, false, false, null);
                String consumeStr = channel.basicConsume(queueName, new Consumer() {
                    @Override
                    public void handleConsumeOk(String consumerTag) {
                        logger.info(">>> handleConsumeOk({})", consumerTag);
                    }

                    @Override
                    public void handleCancelOk(String consumerTag) {
                        logger.info(">>> handleCancelOk({})", consumerTag);
                    }

                    @Override
                    public void handleCancel(String consumerTag) throws IOException {
                        logger.info(">>> handleCancel({})", consumerTag);
                    }

                    @Override
                    public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
                        logger.info(">>> handleShutdownSignal({}, {})", consumerTag, sig);
                    }

                    @Override
                    public void handleRecoverOk(String consumerTag) {
                        logger.info(">>> handleRecoverOk({})", consumerTag);
                    }

                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                        logger.info(">>> handleDelivery({})", new String(body));
                        channel.basicAck(envelope.getDeliveryTag(), true);
                    }
                });
                logger.info("basicConsume: {}", consumeStr);
                channel.basicPublish("", queueName, MessageProperties.PERSISTENT_TEXT_PLAIN, "Hello, World [1]!!!".getBytes());
                Thread.sleep(5000L);
                /**/
                new Thread(new Runnable() {
                    @Override
                    public void run() { try { embeddedMessageBroker.shutDown(); } catch (Exception e) { e.printStackTrace(); } }
                }).start();
                /**/
                Thread.sleep(2000L);
                /**/
                new Thread(new Runnable() {
                    @Override
                    public void run() { try { embeddedMessageBroker.startUp(); } catch (Exception e) { e.printStackTrace(); } }
                }).start();
                /**/
                Thread.sleep(12000L);
                channel.basicPublish("", queueName, MessageProperties.PERSISTENT_TEXT_PLAIN, "Hello, World [2]!!!".getBytes());
                logger.info("basicConsume: {}", consumeStr);
                Thread.sleep(2000L);
            } finally {
                if (channel != null && !channel.isOpen()) {
                    channel.close();
                }
            }

        }
        Thread.sleep(10000);
        /**/embeddedMessageBroker.shutDown();/**/
        System.exit(0);
    }

}
