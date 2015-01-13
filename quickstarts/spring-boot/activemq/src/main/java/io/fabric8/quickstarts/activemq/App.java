package io.fabric8.quickstarts.activemq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;
import org.springframework.util.FileSystemUtils;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import java.io.File;

@Configuration
@EnableAutoConfiguration
public class App {

    private static String destination = "QuickStarts.ActiveMQ.Spring.Boot";

    @Bean
    Receiver receiver() {
        return new Receiver();
    }

    @Bean
    MessageListenerAdapter adapter(Receiver receiver) {
        MessageListenerAdapter messageListener
                = new MessageListenerAdapter(receiver);
        messageListener.setDefaultListenerMethod("receiveMessage");
        return messageListener;
    }

    @Bean
    SimpleMessageListenerContainer container(MessageListenerAdapter messageListener,
                                             ConnectionFactory connectionFactory) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setMessageListener(messageListener);
        container.setConnectionFactory(connectionFactory);
        container.setDestinationName(destination);
        return container;
    }

    public static void main(String[] args) {
        // Clean out any ActiveMQ data from a previous run
        FileSystemUtils.deleteRecursively(new File("activemq-data"));

        // Launch the application
        ConfigurableApplicationContext context = SpringApplication.run(App.class, args);

        // Send a message
        MessageCreator messageCreator = new MessageCreator() {
            @Override
            public Message createMessage(Session session) throws JMSException {
                return session.createTextMessage("<hello>world!</hello>");
            }
        };
        JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
        System.out.println("Sending a new message.");
        jmsTemplate.send(destination, messageCreator);
    }

}
