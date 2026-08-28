package com.vetsoftware.app.gatewaysettlement.application.usecase;

import com.vetsoftware.app.gatewaysettlement.application.dto.GatewaySettlementDto;
import com.vetsoftware.app.gatewaysettlement.application.port.in.FindGatewaySettlementUseCase;
import com.vetsoftware.app.gatewaysettlement.application.port.out.GatewaySettlementRepository;
import com.vetsoftware.app.gatewaysettlement.domain.GatewaySettlementNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "gateway.settlement.find")
@Service
public class FindGatewaySettlementService implements FindGatewaySettlementUseCase {

    private final GatewaySettlementRepository repository;

    public FindGatewaySettlementService(GatewaySettlementRepository repository) {
        this.repository = repository;
    }

    /**
     * La carga es ancha porque no existe otra: la tabla no tiene empresa. Lo que
     * exime a este servicio de {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} no es una
     * excepcion escrita a mano, es que el puerto de salida no declara ninguna
     * variante acotada que este servicio pudiera estar ignorando. Quien protege la
     * fila es el {@code hasRole('SYSTEM')} del puerto de entrada.
     */
    @Override
    public GatewaySettlementDto findById(Long id) {
        return repository.findById(id).map(GatewaySettlementDto::from)
                .orElseThrow(() -> new GatewaySettlementNotFoundException(id));
    }
}
