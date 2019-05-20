
package simulatiesysteem.jms.gateway;

import com.rabbitmq.client.*;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author Rabie Bkhiti
 */
public class MessageSenderGateway {

    private Channel channel;
    private String channelName;
    private String exchangeName = "";

    public MessageSenderGateway(String channelName) {
        this.channelName = channelName;

        ConnectionFactory factory = new ConnectionFactory();

        factory.setHost("192.168.24.180");
        factory.setPort(1011);

        Connection connection = null;

        try {
            connection = factory.newConnection();
            channel = connection.createChannel();

            channel.queueDeclare(channelName, false, false, false, null);
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    public void SendMessage(String body, AMQP.BasicProperties props) throws IOException {
        channel.basicPublish(exchangeName, channelName, props, body.getBytes());
    }

    public Channel getChannel() {
        return channel;
    }
}
