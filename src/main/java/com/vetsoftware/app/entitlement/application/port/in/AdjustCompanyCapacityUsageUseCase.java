package com.vetsoftware.app.entitlement.application.port.in;

import com.vetsoftware.app.entitlement.application.command.AdjustCompanyCapacityUsageCommand;
import com.vetsoftware.app.entitlement.application.dto.CompanyCapacityDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Mover el consumo de una capacidad cuando el mundo real cambia: se da de alta
 * un usuario, se abre una sede, se conecta una terminal.
 *
 * <p>
 * Puerto interno: no lo expone ningun endpoint. Lo llaman los casos de uso que
 * crean o retiran el recurso contado, y por eso su gate es el tenant y no un
 * permiso propio --quien puede crear un empleado ya paso por
 * {@code employee.create}; pedirle ademas un permiso de capacidad seria un
 * segundo candado en la misma puerta que solo consigue que el alta falle en
 * produccion--.
 *
 * <p>
 * <strong>Que la empresa no tenga fila de esa unidad significa limite cero, no
 * ilimitado.</strong> {@code company_capacities} es una tabla derivada y su
 * {@code recalculated_at} es un indicador de salud, asi que una fila que falta
 * es un proceso caido y no una licencia. Se falla cerrado con
 * {@code CompanyCapacityNotFoundException}: leerlo al reves convertiria un
 * recalculo caido en barra libre de recursos facturables que nadie cobra y que
 * nadie nota.
 *
 * <p>
 * <strong>La reserva positiva se decide de forma atomica en el motor.</strong>
 * Un decremento sigue permitido cuando una baja de plan dejo a la empresa por
 * encima del techo; lo unico que no puede hacer es dejar el uso por debajo de
 * cero.
 */
public interface AdjustCompanyCapacityUsageUseCase {

    @PreAuthorize("hasRole('SYSTEM') or @authz.isMyCompany(#command.companyId)")
    CompanyCapacityDto execute(AdjustCompanyCapacityUsageCommand command);
}
