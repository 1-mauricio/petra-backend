package com.marmorarias.identity.adapter.supabase;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credenciais pra chamar a Admin API do Supabase Auth (convite de usuário). Config própria, não
 * compartilhada com storage.supabase.* — mesmo projeto Supabase, mas rotacionar a service role key
 * de um não deve quebrar o outro silenciosamente sem aparecer no nome da config.
 */
@ConfigurationProperties(prefix = "supabase.admin")
public record SupabaseAdminProperties(String url, String serviceRoleKey) {
}
