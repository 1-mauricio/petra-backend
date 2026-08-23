package com.marmorarias.fiscal.adapter;

import com.marmorarias.fiscal.NfeEmissionRequest;
import com.marmorarias.fiscal.NfeEmissionResult;
import com.marmorarias.fiscal.NfeGateway;
import com.marmorarias.fiscal.config.NfeProperties;
import org.springframework.stereotype.Component;

/**
 * Único adapter do NfeGateway hoje — não emite nada de verdade. Existe pra o ponto de plug ficar
 * documentado e testável (injeção via NfeGateway) antes de decidir provedor. Quando escolher um
 * (SEFAZ direto, Focus NFe, eNotas...), troque esta classe por uma implementação real do gateway
 * mantendo a mesma porta; nenhum outro código do núcleo precisa mudar.
 */
@Component
public class NfeGatewayNotConfiguredAdapter implements NfeGateway {

    private final NfeProperties properties;

    public NfeGatewayNotConfiguredAdapter(NfeProperties properties) {
        this.properties = properties;
    }

    @Override
    public NfeEmissionResult emitir(NfeEmissionRequest request) {
        if (!properties.configurado()) {
            throw new UnsupportedOperationException(
                    "Emissão de NFe não configurada — defina FISCAL_NFE_PROVIDER (env NFE_PROVIDER) e as credenciais do provedor escolhido.");
        }
        throw new UnsupportedOperationException("Provedor de NFe '" + properties.provider() + "' ainda sem adapter implementado.");
    }
}
