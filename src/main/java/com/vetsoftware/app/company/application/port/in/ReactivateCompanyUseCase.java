package com.vetsoftware.app.company.application.port.in;

import com.vetsoftware.app.company.application.dto.CompanyDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * El camino de vuelta de {@link DeleteCompanyUseCase}.
 *
 * <p>
 * <b>Archivar una empresa era un viaje de ida.</b> {@code CompanyJpaEntity}
 * lleva {@code @SQLDelete(... SET enabled = false ...)} y
 * {@code @SQLRestriction("enabled = true")}: borrar una empresa la archiva, y
 * desde ese momento <b>ninguna consulta JPA vuelve a verla</b> —ni
 * {@code findById}, porque la restriccion se aplica tambien a la carga por
 * clave primaria—. No habia operacion inversa, asi que deshacer un archivado
 * hecho por error exigia un {@code UPDATE} a mano contra produccion: sin
 * autorizacion que lo mediara, sin rastro de auditoria de quien lo hizo y con
 * la {@code version} de bloqueo optimista sin mover, que es como se pisa en
 * silencio la siguiente escritura de esa fila.
 *
 * <p>
 * <b>{@code hasRole('SYSTEM')} a secas</b>, igual que sus hermanos
 * {@code DeleteCompanyUseCase} y {@code UpdateCompanyUseCase}: la operacion
 * recibe un {@code id} y no un {@code companyId}, es decir no esta acotada a
 * ningun tenant. Y no podria estarlo — el empleado de una empresa archivada no
 * tiene sesion con la que pedir nada, asi que el unico principal capaz de
 * restaurarla es el de plataforma. Quien archiva es quien restaura.
 */
public interface ReactivateCompanyUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    CompanyDto execute(Long id);
}
