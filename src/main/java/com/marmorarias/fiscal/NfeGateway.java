package com.marmorarias.fiscal;

/**
 * Porta para emissão de NFe. Sem adapter funcional ainda — emitir requer decidir provedor
 * (SEFAZ direto ou terceirizado tipo Focus NFe/eNotas) e ter conta/credencial, fora do escopo de
 * código. Ver NfeGatewayNotConfiguredAdapter e fiscal.nfe.* em application.yml para o ponto de plug.
 */
public interface NfeGateway {

    NfeEmissionResult emitir(NfeEmissionRequest request);
}
