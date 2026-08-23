package com.marmorarias.platformbilling.application;

public class WebhookSignatureInvalidException extends RuntimeException {

    public WebhookSignatureInvalidException(String message, Throwable cause) {
        super(message, cause);
    }
}
