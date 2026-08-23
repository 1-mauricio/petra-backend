package com.marmorarias.identity.adapter.supabase;

import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Convite de usuário é sempre pelo Supabase Auth (GoTrue admin API) — o Spring nunca cria conta
 * nem senha (ver CLAUDE.md).
 */
@Component
public class SupabaseAuthAdminClient {

    private final SupabaseAdminProperties properties;
    private final RestClient restClient;

    public SupabaseAuthAdminClient(SupabaseAdminProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    /** Dispara o e-mail de convite do Supabase Auth e retorna o id do auth.users criado. */
    public UUID convidar(String email) {
        Map<String, Object> response;
        try {
            response = restClient.post()
                    .uri(properties.url() + "/auth/v1/invite")
                    .header("Authorization", "Bearer " + properties.serviceRoleKey())
                    .header("apikey", properties.serviceRoleKey())
                    .body(Map.of("email", email))
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                throw new IllegalArgumentException("Não foi possível convidar " + email
                        + " — e-mail já cadastrado ou inválido.", e);
            }
            throw e;
        }
        Object id = response == null ? null : response.get("id");
        if (!(id instanceof String idString)) {
            throw new IllegalStateException("Resposta inesperada do Supabase Auth ao convidar " + email);
        }
        return UUID.fromString(idString);
    }
}
