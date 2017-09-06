import com.sftwnd.crayfish.amqp.consume.DefaultAmqpConsumer;
import com.sftwnd.crayfish.amqp.message.AMQPMessage;
import com.sftwnd.crayfish.amqp.message.AMQPMessageTag;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.MessageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by ashindarev on 10.02.17.
 */
public class AckHandleSpeedTest {

    private static final Logger logger = LoggerFactory.getLogger(AckHandleSpeedTest.class);

    public static void main(String[] args) throws Exception {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setUri("amqp://localhost:5672");
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");
        AtomicLong ack = new AtomicLong(0);
        final int LIMIT = 10000;
        try (Connection connection = connectionFactory.newConnection()) {
            final Channel channel = connection.createChannel();
            try {
                String queueName = "queue";//channel.queueDeclare().getQueue();
                channel.queueDeclare(queueName, true, false, false, null);
                channel.queuePurge(queueName);
                final AtomicLong firstTick = new AtomicLong(0L);
                Queue<AMQPMessageTag> tags = new ConcurrentLinkedQueue<>();

                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                Channel channel = null;
                                try {
                                    try {
                                        for (int i = 0; i < LIMIT+100; i++) {
                                            channel = connection.createChannel();
                                            channel.basicPublish("", queueName, MessageProperties.PERSISTENT_TEXT_PLAIN, "Hello, World!!!" .getBytes());
                                        }
                                    } finally {
                                        if (channel != null && channel.isOpen()) {
                                            channel.close();
                                        }
                                    }
                                } catch (Exception ex) {

                                }
                            }
                        }
                ).start();

                DefaultAmqpConsumer consumer = new DefaultAmqpConsumer<String>(connection, queueName, 250, 100) {

                    @Override
                    public String payload(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                        return new String(body);
                    }

                    @Override
                    public void onArrive(AMQPMessage<String> message) {
                        if (firstTick.get() == 0) {
                            firstTick.set(System.currentTimeMillis());
                        }
                        // logger.trace("Message<{},{}> has been arrived", message.getTag(), message.getPayload());
                        tags.add(message.getTag());
                    }

                    @Override
                    public void onAck(long tag) {
                        ack.set(tag);
                    }

                };
                consumer.start();
                while(ack.get() < LIMIT) {
                    AMQPMessageTag tag = tags.poll();
                    if (tag != null) {
                        consumer.messageComplete(tag);
                    }
                }
                consumer.messageComplete(null);
                System.out.println(1000.0*LIMIT/(System.currentTimeMillis()-firstTick.get()));
            } finally {
                if (channel != null && channel.isOpen()) {
                    channel.close();
                }
            }
        }
    }

}
