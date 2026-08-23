package com.marmorarias.platformbilling.adapter.web;

import com.marmorarias.platformbilling.application.BillingService;
import com.marmorarias.platformbilling.application.WebhookSignatureInvalidException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing")
public class StripeWebhookController {

    private final BillingService billingService;

    public StripeWebhookController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody String payload,
                                               @RequestHeader("Stripe-Signature") String signatureHeader) {
        try {
            billingService.handleWebhookEvent(payload, signatureHeader);
            return ResponseEntity.ok().build();
        } catch (WebhookSignatureInvalidException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
