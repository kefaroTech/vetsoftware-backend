package com.vetsoftware.app.taxreturn.application.port.in;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.taxreturn.application.dto.TaxReturnDto;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListTaxReturnsUseCase {

    /**
     * Todas las declaraciones, paginadas.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas porque el puerto no transporta
     * ningun {@code companyId}</strong>, la señal que examina
     * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}. Aqui no hay empresa que
     * transportar: la tabla no tiene la columna y las declaraciones son de Lumbre.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<TaxReturnDto> listAll(int page, int pageSize);

    /** Las de un periodo fiscal, para armarlo y cotejarlo. */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<TaxReturnDto> listByFiscalPeriod(String fiscalPeriodKey, int page, int pageSize);

    /**
     * <strong>Las que quedan en firme antes de una fecha.</strong>
     *
     * <p>
     * No es un informe decorativo: es la consulta de la que sale hasta cuando hay
     * que conservar los soportes. El detalle de {@code company_usage_events} no se
     * purga mientras exista una declaracion de renta que cubra el año gravable del
     * cargo al que apunta y cuya firmeza no haya pasado. Purgar antes destruye la
     * prueba que sostiene el cargo por excedente, y el fallo aparece cuando la DIAN
     * la pide.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<TaxReturnDto> listBecomingFinalBefore(LocalDate limit, int page, int pageSize);
}
