package com.github.sftwnd.crayfish.examples.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.impl.DefaultExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by ashindarev on 28.02.17.
 */
public class ReconnectExample {

    private static final Logger logger = LoggerFactory.getLogger(ReconnectExample.class);

    public static void main(String[] args) throws IOException, TimeoutException, InterruptedException, NoSuchAlgorithmException, KeyManagementException, URISyntaxException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");

        connectionFactory.setConnectionTimeout(30000);
        connectionFactory.setAutomaticRecoveryEnabled(true);
        connectionFactory.setTopologyRecoveryEnabled(true);
        connectionFactory.setNetworkRecoveryInterval(10000);
        connectionFactory.setExceptionHandler(new DefaultExceptionHandler());
        connectionFactory.setRequestedHeartbeat(360);

        final CountDownLatch countDownLatch = new CountDownLatch(10);
        try (Connection connection = connectionFactory.newConnection()) {
            Channel channel = connection.createChannel();
            channel.basicConsume("queue", new Consumer() {
                @Override
                public void handleConsumeOk(String consumerTag) {
                    logger.info("handleConsumeOk({})", consumerTag);
                }

                @Override
                public void handleCancelOk(String consumerTag) {
                    logger.info("handleCancelOk({})", consumerTag);
                }

                @Override
                public void handleCancel(String consumerTag) throws IOException {
                    logger.info("handleCancel({})", consumerTag);
                }

                @Override
                public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
                    logger.info("handleShutdownSignal({}, {})", consumerTag, sig.getLocalizedMessage());
                }

                @Override
                public void handleRecoverOk(String consumerTag) {
                    logger.info("handleRecoverOk({})", consumerTag);
                }

                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    logger.info("handleDelivery({}, {}, {}, {})", consumerTag, envelope, properties, new String(body));
                    channel.basicAck(envelope.getDeliveryTag(), true);
                    countDownLatch.countDown();
                }
            });
            countDownLatch.await(900, TimeUnit.SECONDS);
        }
    }

}
