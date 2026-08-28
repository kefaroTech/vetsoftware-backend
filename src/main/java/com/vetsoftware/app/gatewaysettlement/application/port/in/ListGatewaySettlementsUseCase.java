package com.vetsoftware.app.gatewaysettlement.application.port.in;

import com.vetsoftware.app.gatewaysettlement.application.dto.GatewaySettlementDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/** Los lotes liquidados, de lo mas reciente a lo mas antiguo. */
public interface ListGatewaySettlementsUseCase {

    /**
     * <strong>{@code hasRole('SYSTEM')} a secas y sin filtro de empresa que
     * ofrecer.</strong> No es que este listado se olvidara del {@code companyId}:
     * es que la tabla no tiene ninguno y no puede tenerlo. Cualquier otra expresion
     * aqui —incluida una acotada por una clave foranea ajena, como la entrada de
     * banco— caeria en lo que persigue {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}, y
     * con razon: la entrada de banco tampoco es de nadie.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<GatewaySettlementDto> listAll(int page, int pageSize);
}
