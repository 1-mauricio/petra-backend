# Marmorarias — Backend

API do MVP de gestão para marmorarias. Spring Boot 3 (Java 21), Postgres via Supabase, Stripe para billing, arquitetura hexagonal por módulo (`domain` / `application` / `adapter`).

## Stack

- Java 21 + Spring Boot 3.3 (Web, Data JPA, OAuth2 Resource Server, Mail, Actuator)
- PostgreSQL (Supabase) + Flyway para migrações
- Stripe (assinaturas/billing da plataforma)
- springdoc-openapi (Swagger UI)
- Testcontainers + JUnit 5 para testes de integração

## Módulos (`src/main/java/com/marmorarias`)

| Módulo | Responsabilidade |
|---|---|
| `identity` | Usuários, tenants, RBAC, integração com Supabase Auth |
| `crm` | Leads e clientes |
| `quoting` | Catálogo e orçamentos |
| `measurement` | Medições de campo |
| `orders` | Pedidos e máquina de estados |
| `production` | Fila/etapas de produção |
| `delivery` | Entregas |
| `billing` | Contas a receber |
| `platformbilling` | Assinatura da plataforma (Stripe, limites de plano) |
| `platformadmin` | Administração multi-tenant |
| `fiscal` | Emissão de NF-e |
| `channels` | Storage e notificações |
| `common` | Infra compartilhada (web, erros) |

## Rodando localmente

```bash
cp .env.local.example .env.local
# preencha DB_URL/DB_USER/DB_PASSWORD, SUPABASE_*, STRIPE_* etc.
mvn spring-boot:run
```

A API sobe em `http://localhost:8080`. Swagger UI em `/swagger-ui.html`.

### Testes

```bash
mvn test
```

Testes de integração usam Testcontainers (sobe Postgres em Docker automaticamente).

### Build de produção

```bash
mvn -B package -DskipTests
# ou via Docker:
docker build -t marmorarias-backend .
```

## Variáveis de ambiente

Ver [`.env.local.example`](.env.local.example) — banco, JWKS do Supabase, CORS, Stripe, SMTP e provedor de NF-e.

## Migrações de banco

SQL versionado em `src/main/resources/db/migration` (Flyway) e `supabase/` (config/policies do projeto Supabase).
