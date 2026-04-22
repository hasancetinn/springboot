package com.ecommerce.demo.service.messaging;

import com.ecommerce.demo.dto.event.EmailEvent;
import com.ecommerce.demo.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailConsumerService {

    private final MailService mailService;

    @KafkaListener(topics = "${app.kafka.topic.email}", groupId = "demo-group")
    public void consumeEmailEvent(EmailEvent event) {
        log.info(">>>> KAFKA CONSUMER: Received EmailEvent for: {}", event.getTo());
        log.info(">>>> KAFKA CONSUMER: Subject: {}", event.getSubject());
        try {
            if (event.getTemplate() != null && !event.getTemplate().isEmpty()) {
                mailService.sendHtmlEmail(event.getTo(), event.getSubject(), event.getBody());
            } else {
                mailService.sendSimpleEmail(event.getTo(), event.getSubject(), event.getBody());
            }
            log.info(">>>> KAFKA CONSUMER: Processed event for: {}", event.getTo());
        } catch (Exception e) {
            log.error(">>>> KAFKA CONSUMER: ERROR processing event for {}", event.getTo(), e);
        }
    }
}
