package com.vetsoftware.app.publicholiday.application.port.in;

import com.vetsoftware.app.publicholiday.application.dto.PublicHolidayDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListPublicHolidaysUseCase {

    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('holiday.read') and @authz.isMyCompany(#companyId))")
    PageResult<PublicHolidayDto> listAll(Long companyId, int page, int pageSize);

    /**
     * Los festivos de un ano, sin paginar: son diecinueve y quien pinta un
     * calendario los quiere todos. Devolver una pagina obligaria al front a
     * recorrerla y a decidir el tamano, que es donde se cuela el ano incompleto.
     */
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('holiday.read') and @authz.isMyCompany(#companyId))")
    List<PublicHolidayDto> listByYear(int year, Long companyId);
}
