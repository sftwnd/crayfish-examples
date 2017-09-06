package com.github.sftwnd.crayfish.examples.rabbitmq;

import com.sftwnd.crayfish.amqp.consume.ConvertableAmqpConsumer;
import com.sftwnd.crayfish.amqp.consume.DefaultAMQPMessageConverter;
import com.sftwnd.crayfish.amqp.message.AMQPMessage;
import com.sftwnd.crayfish.amqp.message.AMQPMessagePayload;
import com.rabbitmq.client.BlockedListener;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.impl.DefaultExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * Created by ashindarev on 28.02.17.
 */
public class NonAcknowledgedExample {

    private static final Logger logger = LoggerFactory.getLogger(NonAcknowledgedExample.class);

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

        final CountDownLatch countDownLatch = new CountDownLatch(10);

        try (Connection connection = connectionFactory.newConnection()) {

            Channel channel = connection.createChannel();
            connection.addBlockedListener(new BlockedListener() {
                @Override
                public void handleBlocked(String s) throws IOException {
                    logger.info("handleBlocked: {}", s);
                }

                @Override
                public void handleUnblocked() throws IOException {
                    logger.info("handleUnblocked: {}");
                }
            });
            connection.addShutdownListener(new ShutdownListener() {
                @Override
                public void shutdownCompleted(ShutdownSignalException e) {
                    logger.info("shutdownCompleted: {}", e.toString());
                }
            });
            Map<Long, AMQPMessage<AMQPMessagePayload>> map = new HashMap<>();
            ConvertableAmqpConsumer<AMQPMessagePayload> consumer = new ConvertableAmqpConsumer<AMQPMessagePayload>(connection, "queue", 10, 4, DefaultAMQPMessageConverter.DEFAULT_AMQP_MESSAGE_CONVERTER) {
                @Override
                // Это конечная точка, в которой можно выплюнуть сообщение наружу
                public void onArrive(AMQPMessage<AMQPMessagePayload> message) {
                    logger.info("Message<{},{}> has been arrived", message.getTag(), message.getPayload());
                    map.put(message.getTag().getValue(), message);
                }
                @Override
                // Это конечная точка, в которой можно выплюнуть сообщение, выбросившее ошибку при преобразовании, наружу
                public void onException(AMQPMessage<IOException> message) {
                    logger.info("Message<{}:...> has been arrived with exception: {}", message.getTag(), message.getPayload() == null ? null : message.getPayload().getMessage());
                }
                @Override
                // Здесь конечная точка процесса подтверждения сообщения. На момент вызова сообщение подтверждено (на клиенте), но ack ещё мог и не пройти.
                public void onComplete(long tag) {
                    logger.info("Message[#{}] has been completed. (Consumer: {})", tag, getConsumerTag());
                }
                @Override
                // Здесь конечная точка процесса нотификации AMQP брокера о подтверждении пачки сообщений.
                public void onAck(long tag) {
                    logger.info("Tag[#{}] has been acknowledged. (Consumer: {})", tag, getConsumerTag());
                }
                @Override
                public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
                    super.handleShutdownSignal(consumerTag, sig);
                    logger.trace("handleShutdownSignal(consumerTag:{}, sig:{})", consumerTag, sig.getLocalizedMessage());
                }
            };

            consumer.start();

            while (map.keySet().size() < 10) {
                Thread.sleep(1000L);
            }

            List<AMQPMessage<AMQPMessagePayload>> list = map.entrySet().stream().filter((entry) -> entry.getKey().longValue() <= 6).map((e) -> e.getValue()).collect(Collectors.toList());
            list.forEach((e) -> {
                try {
                    consumer.messageComplete(e.getTag());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            });

            Thread.sleep(1000000L);
            logger.info("{}", map);


        }

    }

}
