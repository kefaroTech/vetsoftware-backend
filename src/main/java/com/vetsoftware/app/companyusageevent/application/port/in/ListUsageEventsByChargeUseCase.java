package com.vetsoftware.app.companyusageevent.application.port.in;

import com.vetsoftware.app.companyusageevent.application.dto.CompanyUsageEventDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListUsageEventsByChargeUseCase {

    /**
     * El desglose de un cargo por excedente, hecho a hecho.
     *
     * <p>
     * <strong>Es LA consulta por la que esta tabla existe.</strong> Cuando un
     * cliente discute un excedente facturado, esto es lo que se le ensena: cada
     * hecho con su instante y su referencia. Sin ella el cargo es una cifra
     * afirmada.
     *
     * <p>
     * Recibe {@code companyId} y acota con el —lo respalda
     * {@code ix_cue_charge (company_id, charge_id)}—, de modo que el desglose de un
     * cargo nunca puede arrastrar hechos de otra clinica.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<CompanyUsageEventDto> listByCharge(Long companyId, Long chargeId, int page,
            int pageSize);
}
