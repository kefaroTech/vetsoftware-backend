package com.vetsoftware.app.accountingperiod.application.port.in;

import com.vetsoftware.app.accountingperiod.application.dto.AccountingPeriodDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindAccountingPeriodUseCase {

    /**
     * <strong>La lectura por id tambien va cerrada a {@code hasRole('SYSTEM')} a
     * secas.</strong> Un {@code id} lo escribe el cliente en la URL y aqui no hay
     * ningun {@code companyId} con el que acotar la fila —la tabla no tiene
     * empresa—. Que el dato parezca inocuo no lo hace publico: el estado de cierre
     * de un mes dice a cualquiera que lo consulte cuando la plataforma cierra sus
     * libros y hasta que dia puede colarse un ajuste.
     *
     * @throws com.vetsoftware.app.accountingperiod.domain.AccountingPeriodNotFoundException
     *             si el periodo no existe
     */
    @PreAuthorize("hasRole('SYSTEM')")
    AccountingPeriodDto findById(Long id);
}
