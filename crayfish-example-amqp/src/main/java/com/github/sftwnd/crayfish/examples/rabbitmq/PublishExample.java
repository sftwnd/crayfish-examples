package com.github.sftwnd.crayfish.examples.rabbitmq;

import com.rabbitmq.client.*;
import com.rabbitmq.client.impl.DefaultExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;

import static java.lang.Math.random;

/**
 * Created by ashindarev on 28.02.17.
 */
public class PublishExample {

    private static final Logger logger = LoggerFactory.getLogger(PublishExample.class);

    public static void main(String[] args) throws Exception {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");
        connectionFactory.setHost("localhost");
        connectionFactory.setPort(5672);

        connectionFactory.setConnectionTimeout(30000);
        connectionFactory.setAutomaticRecoveryEnabled(true);
        connectionFactory.setTopologyRecoveryEnabled(true);
        connectionFactory.setNetworkRecoveryInterval(10000);
        connectionFactory.setExceptionHandler(new DefaultExceptionHandler());
        connectionFactory.setRequestedHeartbeat(360);

        try (Connection connection = connectionFactory.newConnection();
        ) {

            Channel channel = connection.createChannel();

            channel.addConfirmListener(new ConfirmListener() {
                @Override
                public void handleAck(long deliveryTag, boolean multiple) throws IOException {
                    logger.info("handleAck({}, {})", deliveryTag, multiple);
                }

                @Override
                public void handleNack(long deliveryTag, boolean multiple) throws IOException {
                    logger.info("handleNack({}, {})", deliveryTag, multiple);

                }
            });

            channel.addReturnListener(new ReturnListener() {
                @Override
                public void handleReturn(int replyCode, String replyText, String exchange, String routingKey, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    logger.info("handleReturn({}, {}, {}, {}, {})", replyCode, replyText, exchange, routingKey, properties, new String(body));
                }
            });

            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .messageId(Instant.now().toString()).deliveryMode(2)
                    .priority(0).type(String.class.getCanonicalName()).build();

            channel.basicPublish("", "queue", props, "Oops!!!".getBytes());
            Thread.sleep(10000L);

        }
    }

}
