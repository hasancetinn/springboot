package com.ecommerce.demo.config;

import com.ecommerce.demo.dto.event.EmailEvent;
import com.ecommerce.demo.service.messaging.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MailTestRunner implements CommandLineRunner {

    private final KafkaProducerService kafkaProducerService;

    @Override
    public void run(String... args) {
        String testEmail = "cetinn.hsnn@gmail.com";
        log.info("[MailTestRunner] Triggering automatic startup test mail to: {}", testEmail);

        EmailEvent event = EmailEvent.builder()
                .to(testEmail)
                .subject("Startup Test Email")
                .body("This is an automated test email sent upon application startup to verify the Mailtrap integration.")
                .build();

        try {
            kafkaProducerService.sendEmailEvent(event);
            log.info("[MailTestRunner] Startup test mail event sent successfully.");
        } catch (Exception e) {
            log.error("[MailTestRunner] Failed to trigger startup test mail event", e);
        }
    }
}
