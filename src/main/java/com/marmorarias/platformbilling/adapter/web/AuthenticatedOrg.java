package com.marmorarias.platformbilling.adapter.web;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/** Lê org_id/role das claims do JWT do Supabase Auth (ver CLAUDE.md — Spring só valida, não emite). */
public final class AuthenticatedOrg {

    private AuthenticatedOrg() {
    }

    public static UUID orgId(Jwt jwt) {
        String orgId = jwt.getClaimAsString("org_id");
        if (orgId == null) {
            throw new IllegalStateException("JWT sem claim org_id");
        }
        return UUID.fromString(orgId);
    }
}
