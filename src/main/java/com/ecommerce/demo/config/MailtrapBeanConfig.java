package com.ecommerce.demo.config;

import io.mailtrap.config.MailtrapConfig;
import io.mailtrap.factory.MailtrapClientFactory;
import io.mailtrap.client.MailtrapClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailtrapBeanConfig {

    @Value("${app.mailtrap.token}")
    private String token;

    @Bean
    public MailtrapClient mailtrapClient() {
        MailtrapConfig config = new MailtrapConfig.Builder()
                .token(token)
                .build();
        
        return MailtrapClientFactory.createMailtrapClient(config);
    }
}
