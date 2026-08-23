package com.marmorarias.platformbilling.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Planos e URLs configurados por env — nenhum price id do Stripe fica hardcoded no código. */
@ConfigurationProperties(prefix = "billing.stripe")
public record BillingProperties(String secretKey, String webhookSecret, String successUrl, String cancelUrl,
                                 Map<String, String> prices) {

    public String priceIdForPlano(String plano) {
        String priceId = prices.get(plano);
        if (priceId == null) {
            throw new IllegalArgumentException("Plano desconhecido: " + plano);
        }
        return priceId;
    }
}
