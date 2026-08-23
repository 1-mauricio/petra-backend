-- Adiciona org_name às claims do JWT (mesmo hook de V15), para a UI mostrar o nome da
-- organização em vez do id cru sem precisar de um novo endpoint só para isso.
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
BEGIN
    SELECT up.role::text, up.organization_id, o.nome
    INTO v_role, v_org_id, v_org_nome
    FROM user_profile up
    JOIN organization o ON o.id = up.organization_id
    WHERE up.id = (event->>'user_id')::uuid;

    claims := event->'claims';

    IF v_role IS NOT NULL THEN
        claims := jsonb_set(claims, '{role}', to_jsonb(v_role));
        claims := jsonb_set(claims, '{org_id}', to_jsonb(v_org_id::text));
        claims := jsonb_set(claims, '{org_name}', to_jsonb(v_org_nome));
    END IF;

    RETURN jsonb_set(event, '{claims}', claims);
END;
$$;
