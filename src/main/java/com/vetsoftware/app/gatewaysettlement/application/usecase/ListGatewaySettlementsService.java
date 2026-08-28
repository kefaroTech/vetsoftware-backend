package com.vetsoftware.app.gatewaysettlement.application.usecase;

import com.vetsoftware.app.gatewaysettlement.application.dto.GatewaySettlementDto;
import com.vetsoftware.app.gatewaysettlement.application.port.in.ListGatewaySettlementsUseCase;
import com.vetsoftware.app.gatewaysettlement.application.port.out.GatewaySettlementRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "gateway.settlement.list.all")
@Service
public class ListGatewaySettlementsService implements ListGatewaySettlementsUseCase {

    private final GatewaySettlementRepository repository;

    public ListGatewaySettlementsService(GatewaySettlementRepository repository) {
        this.repository = repository;
    }

    /**
     * Los totales son los de la consulta y no se recalculan sobre el contenido ya
     * paginado: {@code PageResult.map} conserva los metadatos intactos.
     */
    @Override
    public PageResult<GatewaySettlementDto> listAll(int page, int pageSize) {
        return repository.findAll(page, pageSize).map(GatewaySettlementDto::from);
    }
}
