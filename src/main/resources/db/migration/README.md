# Migrations — schema do domínio

Flyway, versionadas e imutáveis (`V1__...` a `V12__...`). Uma vez aplicada em
qualquer ambiente compartilhado, uma migration não é editada — correções viram
uma nova versão.

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
um pool. Se `app.current_org_id` não for setado, `current_setting(...)` sem o
segundo argumento (`missing_ok`) lança erro — a query falha em vez de silenciosamente
não filtrar nada.

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
