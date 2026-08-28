package com.vetsoftware.app.companylimitevent.infrastructure.persistence;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Los recuentos reales de los ejes que hoy se pueden contar.
 *
 * <p>
 * <strong>Extiende {@link Repository} pelado y sus consultas son
 * nativas</strong>, igual que {@code ContractSubscriptionJpaRepository} en
 * {@code entitlement} y por la misma razon: no expone ni un solo metodo de
 * escritura sobre tablas que no son suyas, y al ser nativas dependen del
 * esquema --que es contrato compartido y esta especificado-- y no de como haya
 * decidido mapear sus campos la rodaja duenya. Cuelga de la entidad propia de
 * esta feature para no importar tres entidades ajenas solo para colgar SQL de
 * ellas.
 *
 * <p>
 * <strong>Cada predicado es exactamente el que mueve las altas y las
 * bajas.</strong> Eso no es un detalle: si el recuento contara algo distinto de
 * lo que el contador suma y resta, produciria un desvio en cada pasada sobre
 * contadores perfectamente sanos, y el hecho {@code USAGE_RECONCILED} pasaria
 * de señal a ruido en una noche.
 *
 * <ul>
 * <li>{@code USER} → {@code employees.enabled}, que es lo que mueve
 * {@code EntitlementEmployeeCapacityAdapter}: el alta y la invitacion reservan,
 * la reactivacion de un empleado deshabilitado reserva y la baja --que es
 * logica, {@code enabled = false}-- libera.
 * <li>{@code BRANCH} → {@code branches.active}: crear y activar reservan,
 * desactivar libera. La tabla no usa {@code enabled} para esto.
 * <li>{@code TERMINAL} → {@code cash_terminals.active}, misma forma.
 * </ul>
 */
public interface RealUsageCountJpaRepository extends Repository<CompanyLimitEventJpaEntity, Long> {

    @Query(value = """
            SELECT COUNT(*) FROM employees e
            WHERE e.company_id = :companyId AND e.enabled = TRUE
            """, nativeQuery = true)
    int countActiveUsers(@Param("companyId") Long companyId);

    @Query(value = """
            SELECT COUNT(*) FROM branches b
            WHERE b.company_id = :companyId AND b.active = TRUE
            """, nativeQuery = true)
    int countActiveBranches(@Param("companyId") Long companyId);

    @Query(value = """
            SELECT COUNT(*) FROM cash_terminals t
            WHERE t.company_id = :companyId AND t.active = TRUE
            """, nativeQuery = true)
    int countActiveTerminals(@Param("companyId") Long companyId);
}
