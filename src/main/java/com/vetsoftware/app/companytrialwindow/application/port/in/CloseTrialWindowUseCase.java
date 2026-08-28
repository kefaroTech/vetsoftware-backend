package com.vetsoftware.app.companytrialwindow.application.port.in;

import com.vetsoftware.app.companytrialwindow.application.dto.CompanyTrialWindowDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Cierra el reloj de la empresa. Es lo que libera el marcador de «una abierta
 * por empresa» y permite que una campaña de recuperación abra otra años
 * después.
 *
 * <p>
 * <strong>Cerrar no es acortar.</strong> No toca ni {@code start_date} ni
 * {@code window_days} ni {@code end_date}: escribe la fecha en que la ventana
 * dejó de estar abierta. Las concesiones que colgaban de ella conservan sus
 * fechas, que es lo que hace que reponer un módulo no invente fecha nueva.
 *
 * <p>
 * Autorización: {@code hasRole('SYSTEM')} a secas. Lo dispara el barrido de
 * plataforma o una decisión comercial, nunca el cliente.
 */
public interface CloseTrialWindowUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    CompanyTrialWindowDto execute(Long companyId);
}
