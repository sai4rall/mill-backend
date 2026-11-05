package com.millsystem.sms;

/**
 * Sends a text message to an Indian mobile number (10 digits, no country code).
 * The active implementation is chosen by {@code app.sms.provider}; add a new
 * implementation (e.g. MSG91, Twilio) annotated with a matching {@code @ConditionalOnProperty}.
 */
public interface SmsSender {
    void send(String phone, String message);
}
