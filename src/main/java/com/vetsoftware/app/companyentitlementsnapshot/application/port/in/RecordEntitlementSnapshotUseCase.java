package com.vetsoftware.app.companyentitlementsnapshot.application.port.in;

import com.vetsoftware.app.companyentitlementsnapshot.application.command.RecordEntitlementSnapshotCommand;
import com.vetsoftware.app.companyentitlementsnapshot.application.dto.CompanyEntitlementSnapshotDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Guarda la foto de un recálculo de permisos.
 *
 * <h2>Por qué el gate es la empresa y NO una autoridad</h2>
 *
 * <p>
 * <strong>Este puerto es un efecto del sistema, no una capacidad
 * concedible.</strong> No lo expone ningún endpoint —{@code
 * CompanyEntitlementSnapshotController} solo declara lecturas—: lo llama
 * {@code CompanyEntitlementSnapshotAdapter} bajo el principal de quien disparó
 * el recálculo, como consecuencia de una operación que ese empleado <em>ya</em>
 * tenía permiso para hacer. Exigirle además un
 * {@code hasAuthority('companyEntitlementSnapshot.create')} sería modelar como
 * permiso de usuario algo que el usuario nunca pide.
 *
 * <p>
 * <strong>Y no habría dónde sembrar esa autoridad.</strong> {@code base_roles}
 * tiene una sola fila —{@code ADMIN}, desde el changeset 266—: el rol base de
 * empleado no existe en este esquema. Colgarla de {@code ADMIN} dejaría sin
 * foto todo recálculo disparado por un empleado que no sea administrador, y
 * este adapter <em>no</em> se traga la excepción: el recálculo pedido por la
 * clínica reventaría con {@code AccessDeniedException} en vez de reparar sus
 * permisos, que es justo la palanca de reparación que este puerto sostiene. Por
 * eso {@code companyEntitlementSnapshot.create} nunca se sembró (ver el
 * comentario del changeset 370) y por eso el guard no lo nombra.
 *
 * <p>
 * El tenant tiene que caber en el gate porque el recálculo se dispara desde la
 * propia clínica; un guard cerrado a plataforma dejaría precisamente esos
 * recálculos sin foto.
 */
public interface RecordEntitlementSnapshotUseCase {

    @PreAuthorize("hasRole('SYSTEM') or @authz.isMyCompany(#command.companyId)")
    CompanyEntitlementSnapshotDto execute(RecordEntitlementSnapshotCommand command);
}
