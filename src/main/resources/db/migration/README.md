# Migrations — schema do domínio

Flyway, versionadas e imutáveis (`V1__...` a `V28__...`). Uma vez aplicada em
qualquer ambiente compartilhado, uma migration não é editada — correções viram
uma nova versão.

`V24`/`V25` não existem no histórico (arquivos removidos antes de aplicados
em qualquer ambiente compartilhado) — gap intencional, não um erro de
numeração a corrigir. Flyway não exige sequência contígua.

## Owner vs `app_user`

- **Owner** (quem o Flyway usa para rodar as migrations): dono das tabelas,
  cria roles, habilita RLS, concede grants. Tem `BYPASSRLS` implícito (é o
  dono/superuser), então nunca é bloqueado pelas policies.
- **`app_user`**: role com que o Spring Boot conecta no dia a dia. `LOGIN`,
  `NOBYPASSRLS`, não é dono de nenhuma tabela. Só tem `SELECT/INSERT/UPDATE/DELETE`
  nas tabelas de domínio, e apenas `SELECT/INSERT` em `stage_transition` e
  `audit_log` (append-only — `UPDATE`/`DELETE` são revogados explicitamente).
  A senha de `app_user` é definida fora das migrations (`ALTER ROLE ... PASSWORD`
  via secret manager), nunca commitada.

O Spring conecta com `spring.datasource.*` (`DB_USER=app_user` por padrão).
O Flyway usa um datasource próprio (`spring.flyway.url/user/password`, via
`FLYWAY_DB_URL`/`FLYWAY_DB_USER`/`FLYWAY_DB_PASSWORD`) — se não setados, caem
no mesmo valor de `DB_URL`/`DB_USER`/`DB_PASSWORD` (comportamento anterior,
preservado por segurança), então **é preciso setar `FLYWAY_DB_USER`/
`FLYWAY_DB_PASSWORD` explicitamente com as credenciais do owner** para
ativar a separação — sem isso, o app roda com `BYPASSRLS` (se `DB_USER` for
o owner) ou o Flyway falha ao criar roles/RLS (se `DB_USER` for `app_user`).

⚠️ No Supabase, o usuário do pooler tem o formato `postgres.<project_ref>`
(ex.: `postgres.gmdxlkxelhowqcxomlnv`) — isso *é* a role owner `postgres`,
não `app_user`. Se `DB_USER`/`DB_PASSWORD` do seu `.env` ainda apontam para
esse usuário, o app está rodando com `BYPASSRLS` agora, sem isolamento real
entre organizações. Corrija assim: mova o `postgres.<ref>` atual para
`FLYWAY_DB_USER`/`FLYWAY_DB_PASSWORD`, e troque `DB_USER`/`DB_PASSWORD` para
as credenciais reais de `app_user` (`ALTER ROLE app_user PASSWORD '...'`
uma vez, via secret manager/SQL editor do Supabase — nunca commitado).

Isso dá defesa em profundidade: mesmo que a camada de aplicação (Spring) tenha
um bug e monte uma query sem filtro de organização, a RLS no Postgres barra o
acesso cross-tenant — porque as tabelas estão com `FORCE ROW LEVEL SECURITY`,
que se aplica mesmo a quem não é o dono da conexão atual.

## Como o app seta `app.current_org_id`

Toda policy de RLS usa:

```sql
USING (organization_id = current_setting('app.current_org_id')::uuid)
```

O Spring, ao abrir a conexão/transação para uma requisição autenticada, extrai
`org_id` do claim do JWT (emitido pelo Supabase Auth) e executa, **dentro da
mesma transação**, antes de qualquer outra query:

```sql
SET LOCAL app.current_org_id = '<org_id do JWT>';
```

`SET LOCAL` (não `SET`) é essencial: o valor vale só até o fim da transação —
evita vazamento de contexto entre requisições que reusam a mesma conexão de
um pool.

Até `V23`, `current_setting(...)` era chamado sem `missing_ok`: se
`app.current_org_id` não estivesse setado, a chamada lançava erro — a query
falhava em vez de silenciosamente não filtrar nada. A partir de `V27` (ver
seção seguinte), as policies passaram a usar `missing_ok=true` para permitir
que uma sessão sete só uma das duas GUCs (tenant comum nunca seta
`app.is_platform_admin`; admin da plataforma não seta `app.current_org_id` em
leitura cross-org). O efeito de segurança é o mesmo: GUC ausente vira `NULL`,
e `organization_id = NULL` nunca é `true`, então a policy ainda nega acesso
por padrão — só que via "nenhuma linha casa" em vez de erro.

## Admin da plataforma (`V27`/`V28`)

Staff Petra (sem organização própria) é cadastrado em `platform_admin`
(allowlist, sem RLS). O hook do JWT (`V28`) injeta o claim
`is_platform_admin=true` para esses usuários, sem `org_id`/`role`.

O backend, atrás de `@PreAuthorize("hasRole('platform_admin')")`, seta:

```sql
SELECT set_config('app.is_platform_admin', 'true', true);
```

Toda policy de tabela org-scoped foi recriada em `V27` como:

```sql
USING (organization_id = current_setting('app.current_org_id', true)::uuid
    OR current_setting('app.is_platform_admin', true) = 'true')
```

Mesmo `app_user`, mesma fronteira de confiança do `app.current_org_id` — sem
role nem datasource novo. O bypass só existe quando o backend seta a GUC, e
isso só acontece atrás do `@PreAuthorize` acima (ver
`com.marmorarias.platformadmin`).

## Enums

Estados fechados (papel, status do pedido, do orçamento, da medição etc.) são
tipos `ENUM` nativos do Postgres — não tabelas de domínio — por serem mais
baratos, autoexplicativos no `\d` e já suficientes para o conjunto de valores
do MVP.

## Seed (`V12`)

Dados mínimos de exemplo para desenvolvimento local (1 organização, 3 usuários
— um por papel —, alguns materiais/itens de catálogo). **Não deve rodar em
produção.** Se um pipeline de prod usar as mesmas migrations, mova `V12` para
uma location Flyway separada (`flyway.locations` só inclui a pasta de seed em
perfis de dev/teste).
