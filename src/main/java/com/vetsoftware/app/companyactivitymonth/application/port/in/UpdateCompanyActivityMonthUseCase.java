package com.vetsoftware.app.companyactivitymonth.application.port.in;

import com.vetsoftware.app.companyactivitymonth.application.command.UpdateCompanyActivityMonthCommand;
import com.vetsoftware.app.companyactivitymonth.application.dto.CompanyActivityMonthDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateCompanyActivityMonthUseCase {

    /**
     * Recalcula el mes en curso sobre su propia fila.
     *
     * <p>
     * <strong>Es la operacion por la que esta tabla lleva bloqueo
     * optimista.</strong> La fila del mes vivo se reescribe cada dia hasta que el
     * mes termina, asi que dos recalculos concurrentes —el proceso nocturno y una
     * correccion a mano— se pisarian sin excepcion y sin log. El {@code @Version}
     * de la entidad convierte ese silencio en un 409.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas</strong>, y aqui ademas es
     * obligatorio: el command lleva un {@code id} y no lleva empresa, que es
     * exactamente la forma que vigila
     * {@code OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM}.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    CompanyActivityMonthDto execute(UpdateCompanyActivityMonthCommand command);
}
