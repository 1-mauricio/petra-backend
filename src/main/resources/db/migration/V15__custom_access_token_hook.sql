-- Auth Hook do Supabase (custom_access_token): injeta org_id e role no JWT a partir de
-- user_profile, para o Spring conseguir validar o token via JWKS e já achar as claims que a
-- RLS e o RBAC da aplicação esperam. search_path fixo por ser SECURITY DEFINER.
CREATE OR REPLACE FUNCTION public.custom_access_token_hook(event jsonb)
RETURNS jsonb
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    claims jsonb;
    v_role text;
    v_org_id uuid;
BEGIN
    SELECT role::text, organization_id INTO v_role, v_org_id
    FROM user_profile
    WHERE id = (event->>'user_id')::uuid;

    claims := event->'claims';

    IF v_role IS NOT NULL THEN
        claims := jsonb_set(claims, '{role}', to_jsonb(v_role));
        claims := jsonb_set(claims, '{org_id}', to_jsonb(v_org_id::text));
    END IF;

    RETURN jsonb_set(event, '{claims}', claims);
END;
$$;

GRANT USAGE ON SCHEMA public TO supabase_auth_admin;
GRANT EXECUTE ON FUNCTION public.custom_access_token_hook TO supabase_auth_admin;
REVOKE EXECUTE ON FUNCTION public.custom_access_token_hook FROM authenticated, anon, public;

-- user_profile tem FORCE ROW LEVEL SECURITY (V11) — o hook roda como supabase_auth_admin, que
-- não é dono da tabela nem superuser, então precisa de uma policy própria para ler o papel/org
-- do usuário antes de o token existir (não há app.current_org_id setado nesse momento).
CREATE POLICY user_profile_auth_hook ON user_profile
    FOR SELECT TO supabase_auth_admin USING (true);
