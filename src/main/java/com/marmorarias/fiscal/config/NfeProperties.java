package com.marmorarias.fiscal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Provedor ainda não escolhido — todo campo vazio até essa decisão de negócio ser tomada. */
@ConfigurationProperties(prefix = "fiscal.nfe")
public record NfeProperties(String provider, String apiKey, String ambiente) {

    public boolean configurado() {
        return provider != null && !provider.isBlank();
    }
}
