package com.vetsoftware.app.entitlement.infrastructure.persistence;

import com.vetsoftware.app.subscription.infrastructure.persistence.SubscriptionJpaEntity;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Lectura del contrato desde este slice.
 *
 * <p>
 * Extiende {@link Repository} pelado y no {@code JpaRepository} a proposito: no
 * expone ni un solo metodo de escritura ni de lectura sin empresa sobre una
 * tabla que no es suya. Y sus consultas son <strong>nativas</strong> porque asi
 * dependen del esquema --que es contrato compartido y esta especificado-- y no
 * de como haya decidido mapear sus campos el slice {@code subscription}.
 */
public interface ContractSubscriptionJpaRepository extends Repository<SubscriptionJpaEntity, Long> {

    /**
     * El contrato vigente ese dia.
     *
     * <p>
     * <strong>Vigente = ya empezo y todavia no ha terminado</strong>, no "sin fecha
     * de fin" ni "status = ACTIVE". {@code PAST_DUE} y {@code READ_ONLY} son
     * contratos vigentes: el cliente debe, pero sigue trabajando. Los que salen del
     * criterio son {@code CANCELLED} y {@code EXPIRED}. Con el criterio equivocado
     * el error es invisible hasta que un cliente reclama.
     *
     * <p>
     * El {@code LIMIT 1} es defensa en profundidad: la base ya garantiza como mucho
     * un contrato vigente por empresa con el indice unico sobre
     * {@code active_marker}.
     */
    @Query(value = """
            SELECT s.id AS id, s.status AS status, s.trial_end_date AS trialEndDate
            FROM subscriptions s
            WHERE s.company_id = :companyId
              AND s.enabled = TRUE
              AND s.status IN ('TRIALING', 'ACTIVE', 'PAST_DUE', 'READ_ONLY')
              AND s.start_date <= :on
              AND (s.cancel_effective_date IS NULL OR s.cancel_effective_date > :on)
            ORDER BY s.start_date DESC, s.id DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<ContractSubscriptionView> findCurrentByCompanyId(@Param("companyId") Long companyId,
            @Param("on") LocalDate on);

    /**
     * El ultimo contrato de la empresa, en cualquier estado. Es lo que permite que
     * una cuenta cancelada conserve el acceso de solo lectura a lo que ya escribio
     * en vez de quedarse sin nada, que es la politica R18.
     */
    @Query(value = """
            SELECT s.id AS id, s.status AS status, s.trial_end_date AS trialEndDate
            FROM subscriptions s
            WHERE s.company_id = :companyId
              AND s.enabled = TRUE
            ORDER BY s.start_date DESC, s.id DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<ContractSubscriptionView> findLatestByCompanyId(@Param("companyId") Long companyId);
}
