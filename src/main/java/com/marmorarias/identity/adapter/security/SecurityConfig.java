package com.marmorarias.identity.adapter.security;

import com.marmorarias.platformbilling.adapter.web.BillingAccessGateFilter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * O Spring só VALIDA o JWT do Supabase (via JWKS) — não emite, não faz login/refresh/reset.
 * RBAC (admin/comercial/producao) é reforçado na camada de aplicação via @PreAuthorize — aqui só
 * traduzimos o claim "role" em GrantedAuthority, em minúsculo, igual ao enum user_role do banco e
 * ao claim emitido pelo Supabase — todo @PreAuthorize do projeto usa hasAnyRole('admin', ...) etc.
 * Único SecurityFilterChain do app: platform_billing pluga aqui o próprio filtro de gate (402 em
 * escritas de org sem assinatura ATIVA) em vez de declarar outra configuração de segurança.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/api/billing/webhook"
    };

    /**
     * O decoder padrão do Spring só aceita RS256; o Supabase Auth assina com ES256 (chaves
     * assimétricas modernas — projetos legados usam HS256, fora de escopo aqui). Sem isso, todo
     * token válido é rejeitado com "no matching key(s) found" mesmo com o kid batendo no JWKS.
     */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri) {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .jwsAlgorithm(SignatureAlgorithm.ES256)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, BillingAccessGateFilter billingAccessGateFilter,
                                            TenantContextLoggingFilter tenantContextLoggingFilter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .addFilterAfter(tenantContextLoggingFilter, BearerTokenAuthenticationFilter.class)
                .addFilterAfter(billingAccessGateFilter, TenantContextLoggingFilter.class);
        return http.build();
    }

    /** Web (localhost:3000 em dev) chama a API cross-origin; sem isso o preflight OPTIONS falha. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private org.springframework.core.convert.converter.Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::authoritiesFromRoleClaim);
        return converter;
    }

    private List<GrantedAuthority> authoritiesFromRoleClaim(Jwt jwt) {
        String role = jwt.getClaimAsString("role");
        if (role == null) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
