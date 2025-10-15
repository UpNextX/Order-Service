package org.upnext.orderservice.Configurations;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentRabbitMqConfig {

    public static final String EXCHANGE = "payment.exchange";
    public static final String SUCCESS_QUEUE = "payment.success";
    public static final String FAILURE_QUEUE = "payment.failure";
    public static final String SUCCESS_ROUTING_KEY = "payment.success";
    public static final String FAILURE_ROUTING_KEY = "payment.failure";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue successQueue() {
        return new Queue(SUCCESS_QUEUE, true);
    }

    @Bean
    public Queue failureQueue() {
        return new Queue(FAILURE_QUEUE, true);
    }

    @Bean
    public Binding successBinding(Queue successQueue, TopicExchange exchange) {
        return BindingBuilder.bind(successQueue).to(exchange).with(SUCCESS_ROUTING_KEY);
    }

    @Bean
    public Binding failureBinding(Queue failureQueue, TopicExchange exchange) {
        return BindingBuilder.bind(failureQueue).to(exchange).with(FAILURE_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
