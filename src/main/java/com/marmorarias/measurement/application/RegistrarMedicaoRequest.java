package com.marmorarias.measurement.application;

import java.util.List;
import java.util.UUID;

public record RegistrarMedicaoRequest(UUID orderId, List<PecaMedidaRequest> pecas) {
}
