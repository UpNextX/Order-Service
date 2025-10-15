package org.upnext.orderservice.Configurations;

import org.springframework.context.annotation.Configuration;

@Configuration
public class MailRabbitMqConfig {
    public static final String EXCHANGE = "mail.exchange";
    public static final String MAIL_ROUTING_KEY = "mail.";
}
