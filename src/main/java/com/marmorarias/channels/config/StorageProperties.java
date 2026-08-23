package com.marmorarias.channels.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Credenciais do Supabase Storage — nenhuma fica hardcoded no código. */
@ConfigurationProperties(prefix = "storage.supabase")
public record StorageProperties(String url, String serviceRoleKey, String bucket) {
}
