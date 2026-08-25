-- Estende o hook de V20 (org_name) + V15: staff Petra (platform_admin) não tem
-- user_profile/organização, então ganha só o claim is_platform_admin=true, sem
-- role/org_id/org_name. Fluxo de usuário comum inalterado.
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
    v_org_nome text;
    v_user_id uuid := (event->>'user_id')::uuid;
BEGIN
    claims := event->'claims';

    IF EXISTS (SELECT 1 FROM platform_admin WHERE user_id = v_user_id) THEN
        claims := jsonb_set(claims, '{is_platform_admin}', to_jsonb(true));
        RETURN jsonb_set(event, '{claims}', claims);
    END IF;

    SELECT up.role::text, up.organization_id, o.nome
    INTO v_role, v_org_id, v_org_nome
    FROM user_profile up
    JOIN organization o ON o.id = up.organization_id
    WHERE up.id = v_user_id;

    IF v_role IS NOT NULL THEN
        claims := jsonb_set(claims, '{role}', to_jsonb(v_role));
        claims := jsonb_set(claims, '{org_id}', to_jsonb(v_org_id::text));
        claims := jsonb_set(claims, '{org_name}', to_jsonb(v_org_nome));
    END IF;

    RETURN jsonb_set(event, '{claims}', claims);
END;
$$;
