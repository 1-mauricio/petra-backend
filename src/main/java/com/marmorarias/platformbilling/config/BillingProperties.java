package com.marmorarias.platformbilling.config;

import com.marmorarias.platformbilling.domain.PlanLimits;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Planos e URLs configurados por env — nenhum price id do Stripe fica hardcoded no código. */
@ConfigurationProperties(prefix = "billing.stripe")
public record BillingProperties(String secretKey, String webhookSecret, String successUrl, String cancelUrl,
                                 Map<String, String> prices, String planoOverride) {

    /** Só em local (BILLING_PLANO_OVERRIDE): fixa o plano e destrava o gate de assinatura sem Stripe real. */
    public boolean overrideAtivo() {
        return planoOverride != null && !planoOverride.isBlank();
    }

    public String priceIdForPlano(String plano) {
        String priceId = prices.get(plano);
        if (priceId == null) {
            throw new IllegalArgumentException("Plano desconhecido: " + plano);
        }
        return priceId;
    }

    /** Inverso de priceIdForPlano. Price id não reconhecido (ou sem subscription) cai no plano mais restrito. */
    public String planoForPriceId(String priceId) {
        return prices.entrySet().stream()
                .filter(entry -> entry.getValue().equals(priceId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(PlanLimits.PLANO_BASICO);
    }
}
