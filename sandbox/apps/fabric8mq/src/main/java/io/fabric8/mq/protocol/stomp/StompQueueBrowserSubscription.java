package io.fabric8.mq.protocol.stomp;

import io.fabric8.mq.protocol.ProtocolException;
import org.apache.activemq.command.ConsumerInfo;
import org.apache.activemq.command.MessageAck;
import org.apache.activemq.command.MessageDispatch;
import org.apache.activemq.command.TransactionId;
import org.apache.activemq.transport.stomp.Stomp;
import org.apache.activemq.transport.stomp.StompFrame;

import javax.jms.JMSException;
import java.io.IOException;

public class StompQueueBrowserSubscription extends StompSubscription {
    public StompQueueBrowserSubscription(StompProtocolConverter stompTransport, String subscriptionId, ConsumerInfo consumerInfo, String transformation) {
        super(stompTransport, subscriptionId, consumerInfo, transformation);
    }

    @Override
    void onMessageDispatch(MessageDispatch md, String ackId) throws IOException, JMSException {

        if (md.getMessage() != null) {
            super.onMessageDispatch(md, ackId);
        } else {
            StompFrame browseDone = new StompFrame(Stomp.Responses.MESSAGE);
            browseDone.getHeaders().put(Stomp.Headers.Message.SUBSCRIPTION, this.getSubscriptionId());
            browseDone.getHeaders().put(Stomp.Headers.Message.BROWSER, "end");
            browseDone.getHeaders().put(Stomp.Headers.Message.DESTINATION,
                                           protocolConverter.findTranslator(null).convertDestination(protocolConverter, this.destination));
            browseDone.getHeaders().put(Stomp.Headers.Message.MESSAGE_ID, "0");

            protocolConverter.sendToStomp(browseDone);
        }
    }

    @Override
    public MessageAck onStompMessageNack(String messageId, TransactionId transactionId) throws ProtocolException {
        throw new ProtocolException("Cannot Nack a message on a Queue Browser Subscription.");
    }
}
