package com.marmorarias.channels.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** SMTP configurado por env — nenhum segredo no código (ver CLAUDE.md). Vazio em dev: adapter só loga. */
@ConfigurationProperties(prefix = "notification.mail")
public record MailProperties(String host, int port, String username, String password, String from) {

    public boolean configurado() {
        return host != null && !host.isBlank();
    }
}
