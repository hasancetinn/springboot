package com.ecommerce.demo.controller;

import com.ecommerce.demo.dto.event.EmailEvent;
import com.ecommerce.demo.service.messaging.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
@Slf4j
public class MailTestController {

    private final KafkaProducerService kafkaProducerService;

    @GetMapping("/mail")
    public String triggerTestMail(@RequestParam String email) {
        log.info("Triggering test mail event for: {}", email);
        
        EmailEvent event = EmailEvent.builder()
                .to(email)
                .subject("Test Email via Kafka")
                .body("This is a test email sent from the MailTestController via Kafka pipeline.")
                .build();
        
        kafkaProducerService.sendEmailEvent(event);
        
        return "Test email event produced for: " + email + ". Check logs for consumption and delivery.";
    }
}
