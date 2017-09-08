import com.github.sftwnd.crayfish.amqp.consume.DefaultAmqpConsumer;
import com.github.sftwnd.crayfish.amqp.message.AMQPMessage;
import com.github.sftwnd.crayfish.amqp.message.AMQPMessageTag;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.MessageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by ashindarev on 10.02.17.
 */
public class AckHandleRecoverTest {

    private static final Logger logger = LoggerFactory.getLogger(AckHandleRecoverTest.class);

    public static void main(String[] args) throws Exception {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setUri("amqp://localhost:5672");
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");
        AtomicLong ack = new AtomicLong(0);
        try (Connection connection = connectionFactory.newConnection()) {
            Channel channel = connection.createChannel();
            try {
                String queueName = "queue";//channel.queueDeclare().getQueue();
                channel.queueDeclare(queueName, true, false, false, null);
                channel.queuePurge(queueName);
                for (int i=0; i<50; i++) {
                    channel.basicPublish("", queueName, MessageProperties.PERSISTENT_TEXT_PLAIN, "Hello, World!!!" .getBytes());
                }
                List<AMQPMessageTag> list = new ArrayList<>();
                DefaultAmqpConsumer consumer = new DefaultAmqpConsumer<String>(connection, queueName, 16, 10) {

                    @Override
                    public String payload(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                        return new String(body);
                    }

                    @Override
                    public void onArrive(AMQPMessage<String> message) {
                        logger.trace("Message<{},{}> has been arrived", message.getTag(), message.getPayload());
                        list.add(message.getTag());
                    }

                    @Override
                    public void onAck(long tag) {
                        logger.debug("ACK[{}:{}]",getConsumerTag(), tag);
                        ack.set(tag);
                    }

                };
                consumer.start();
                while(ack.get() < 50) {
                    Thread.sleep(2000L);
                    if (list.size() > 0) {
                        consumer.messageComplete(list.remove(0));
                    }
                }
                consumer.messageComplete(null);
            } finally {
                if (channel != null && channel.isOpen()) {
                    channel.close();
                }
            }
        }
    }

}
