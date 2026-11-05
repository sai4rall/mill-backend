package com.millsystem.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Development sender: writes the message to the backend log instead of sending an SMS.
 * Active when {@code app.sms.provider=log} (the default). Never use in production.
 */
@Component
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "log", matchIfMissing = true)
public class LoggingSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsSender.class);

    @Override
    public void send(String phone, String message) {
        log.warn("[DEV SMS — not actually sent] to {}: {}", phone, message);
    }
}
