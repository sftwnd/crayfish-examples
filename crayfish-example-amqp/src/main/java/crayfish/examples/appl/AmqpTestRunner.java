package crayfish.examples.appl;

import com.github.sftwnd.crayfish.embedded.amqp.qpid.EmbeddedMessageBroker;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.MessageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;


/**
 *  Загрузчик базового приложения командной строки
 */

@Component
@DependsOn(value = {"crayfish-spring-amqp"})
@Profile(value = {"crayfish-examples", "crayfish-cmd-line"})
public class AmqpTestRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AmqpTestRunner.class);

    @Autowired
    @Qualifier(value = "crayfish-amqp.local")
    ConnectionFactory connectionFactory;

    @Override
    public void run(String... args) throws Exception {

        logger.info("Start embeded Message Queue Broker");
        EmbeddedMessageBroker embeddedMessageBroker = new EmbeddedMessageBroker(AMQP.PROTOCOL.PORT);
        try {
              embeddedMessageBroker.startUp();

            logger.info("Connect to local AMQP service");

            try (Connection connection = /*embeddedMessageBroker.newConnection()*/connectionFactory.newConnection()/**/) {
                Channel channel = connection.createChannel();
                try {
                    logger.info("Create queue");
                    String queueName = channel.queueDeclare().getQueue();
                    logger.info("Queue '{}' has been created", queueName);
                    final CountDownLatch cdl = new CountDownLatch(1);
                    String consumer = channel.basicConsume(queueName, new DefaultConsumer(channel) {
                        @Override
                        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                            logger.info("Message has been received for consumer[tag:{}@{}]: {}", consumerTag, envelope.getDeliveryTag(), new String(body));
                            cdl.countDown();
                        }
                    });
                    channel.basicPublish("", queueName, MessageProperties.PERSISTENT_TEXT_PLAIN, "Hello, World!!!".getBytes());
                    cdl.await(3, TimeUnit.SECONDS);
                    if (cdl.getCount() > 0) {
                        logger.error("Message has not been delivery yet");
                    }
                } finally {
                    channel.close();
                }
            }
        } finally {
            try {
                  embeddedMessageBroker.tearDown();
            } catch (Throwable ex) {
                logger.error("Unable to tearDown embeddedMessageBroker");
            }
        }

    }

}
