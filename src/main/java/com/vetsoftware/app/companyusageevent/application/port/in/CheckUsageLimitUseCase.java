package com.vetsoftware.app.companyusageevent.application.port.in;

import com.vetsoftware.app.companyusageevent.application.command.CheckUsageLimitCommand;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Comprueba que la empresa todavía tenga margen en un eje contable antes de
 * dejar crear lo que lo consumiría. Puerto interno: no lo expone ningún
 * endpoint, lo llaman los casos de uso de otras features que crean el registro
 * contado (mascota, propietario, cita, servicio de spa o guardería), igual que
 * {@code AdjustCompanyCapacityUsageUseCase} para los ejes de existencias — por
 * eso el gate es el tenant y no un permiso propio.
 *
 * <p>
 * Lanza {@code CompanyUsageLimitExceededException} (409) si el eje ya está en
 * su techo. No escribe nada si hay margen: quien anota el hecho, una vez creado
 * el registro con su id, es {@code RecordCompanyUsageEventUseCase}.
 */
public interface CheckUsageLimitUseCase {

    @PreAuthorize("hasRole('SYSTEM') or @authz.isMyCompany(#command.companyId)")
    void execute(CheckUsageLimitCommand command);
}
