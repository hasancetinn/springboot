package com.ecommerce.demo.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmailEvent {
    private String to;
    private String subject;
    private String body;
    private String template;
}
