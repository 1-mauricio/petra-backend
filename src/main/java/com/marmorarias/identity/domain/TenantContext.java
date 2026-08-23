package com.marmorarias.identity.domain;

import java.util.UUID;

/** Identidade resolvida do JWT para a requisição atual: quem, de qual organização, com qual papel. */
public record TenantContext(UUID organizationId, UUID userId, Role role) {
}
