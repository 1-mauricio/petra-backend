package com.marmorarias.platformbilling.domain;

/**
 * Limites de uso por plano. Só o plano `basico` tem teto — `pro` é ilimitado.
 * Plano desconhecido é tratado como `basico` (fail-safe do lado mais restrito).
 */
public final class PlanLimits {

    public static final String PLANO_BASICO = "basico";
    public static final String PLANO_PRO = "pro";

    public static final int BASICO_MAX_USUARIOS = 3;
    public static final int BASICO_MAX_PEDIDOS_POR_MES = 50;

    private PlanLimits() {
    }

    public static boolean usuarioDentroDoLimite(String plano, long usuariosAtuais) {
        return PLANO_PRO.equals(plano) || usuariosAtuais < BASICO_MAX_USUARIOS;
    }

    public static boolean pedidoDentroDoLimite(String plano, long pedidosNoMesAtual) {
        return PLANO_PRO.equals(plano) || pedidosNoMesAtual < BASICO_MAX_PEDIDOS_POR_MES;
    }
}
