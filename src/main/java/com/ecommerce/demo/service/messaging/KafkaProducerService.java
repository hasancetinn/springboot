package com.ecommerce.demo.service.messaging;

import com.ecommerce.demo.dto.event.EmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.email}")
    private String emailTopic;

    public void sendEmailEvent(EmailEvent event) {
        log.info("Producing email event for: {}", event.getTo());
        kafkaTemplate.send(emailTopic, event);
    }
}
