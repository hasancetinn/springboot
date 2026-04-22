package com.ecommerce.demo.service;

import io.mailtrap.client.MailtrapClient;
import io.mailtrap.model.request.emails.MailtrapMail;
import io.mailtrap.model.request.emails.Address;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final MailtrapClient mailtrapClient;
    @Value("${app.mailtrap.from.email:hello@demomailtrap.co}")
    private String fromEmail;
    @Value("${app.mailtrap.from.name:E-Commerce Demo}")
    private String fromName;

    public void sendSimpleEmail(String to, String subject, String body) {
        log.info("Sending simple email to: {} using Mailtrap SDK", to);
        
        MailtrapMail mail = MailtrapMail.builder()
                .from(new Address(fromEmail, fromName))
                .to(List.of(new Address(to)))
                .subject(subject)
                .text(body)
                .build();

        try {
            log.info("Invoking Mailtrap SDK for simple email...");
            var response = mailtrapClient.send(mail);
            log.info("Mailtrap SDK response for simple email: {}", response);
            log.info("Simple email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to send simple email to {} via Mailtrap SDK", to, e);
            throw new IllegalStateException("Mailtrap simple email send failed", e);
        }
    }

    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        log.info("Sending HTML email to: {} using Mailtrap SDK", to);

        MailtrapMail mail = MailtrapMail.builder()
                .from(new Address(fromEmail, fromName))
                .to(List.of(new Address(to)))
                .subject(subject)
                .html(htmlBody)
                .build();

        try {
            log.info("Invoking Mailtrap SDK for HTML email...");
            var response = mailtrapClient.send(mail);
            log.info("Mailtrap SDK response for HTML email: {}", response);
            log.info("HTML email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to send HTML email to {} via Mailtrap SDK", to, e);
            throw new IllegalStateException("Mailtrap HTML email send failed", e);
        }
    }
}
