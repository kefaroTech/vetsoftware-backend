package com.vetsoftware.app.entitlement.infrastructure.persistence;

import com.vetsoftware.app.subscription.infrastructure.persistence.SubscriptionJpaEntity;
import java.time.LocalDate;
import java.util.List;
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
            SELECT s.id AS id, s.status AS status, s.trial_end_date AS trialEndDate,
                   s.start_date AS startDate
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
            SELECT s.id AS id, s.status AS status, s.trial_end_date AS trialEndDate,
                   s.start_date AS startDate
            FROM subscriptions s
            WHERE s.company_id = :companyId
              AND s.enabled = TRUE
            ORDER BY s.start_date DESC, s.id DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<ContractSubscriptionView> findLatestByCompanyId(@Param("companyId") Long companyId);

    /**
     * Solo la fecha de firma del contrato mas antiguo vivo de la empresa.
     *
     * <p>
     * <strong>{@code ASC} y no {@code DESC}</strong>, al contrario que las dos de
     * arriba, y es toda la intencion: D-74 protege a quien firmo <em>antes</em> de
     * que el eje existiera, asi que la fecha que manda es la primera vez que esta
     * empresa firmo, no la del contrato mas reciente. Con {@code DESC}, cualquier
     * contrato posterior --una renovacion, un cambio de plan-- reactivaria en
     * silencio limites que el cliente nunca acepto, que es justo el apagado masivo
     * que D-74 existe para impedir.
     *
     * <p>
     * Devuelve la fecha desnuda: quien pregunta esta en la rama en la que el
     * contador ya iba a fallar y no necesita el contrato entero.
     */
    @Query(value = """
            SELECT s.start_date
            FROM subscriptions s
            WHERE s.company_id = :companyId
              AND s.enabled = TRUE
            ORDER BY s.start_date ASC, s.id ASC
            LIMIT 1
            """, nativeQuery = true)
    Optional<LocalDate> findEarliestStartDateByCompanyId(@Param("companyId") Long companyId);

    /**
     * Toma el candado de los contratos de una empresa. <strong>Es lo primero que
     * hace un recalculo, siempre</strong> (R-ENT-08).
     *
     * <p>
     * El orden de bloqueo no es una preferencia: es lo unico que evita el
     * interbloqueo real entre el recalculo y la confirmacion de un otrosi. Los dos
     * tocan el contrato y los permisos de la misma empresa; si uno toma primero el
     * contrato y el otro primero los permisos, se esperan mutuamente y el motor
     * mata a uno de los dos con un error que le llega al cliente. Con el contrato
     * siempre primero, el segundo recalculo se pone en fila y sale correcto.
     *
     * <p>
     * Y resuelve el otro caso, que no es un interbloqueo sino algo peor porque no
     * falla: un recalculo que corre mientras se confirma un otrosi lee las lineas
     * <em>antes</em> del cambio, borra y reinserta <em>despues</em>, y deja a la
     * empresa sin la linea nueva sin un solo error en ningun log.
     *
     * <p>
     * Devuelve ids y no entidades a proposito: lo que hace falta es el candado, no
     * los datos, y materializar las filas solo cargaria memoria para tirarla.
     */
    @Query(value = """
            SELECT s.id
            FROM subscriptions s
            WHERE s.company_id = :companyId
            ORDER BY s.id
            FOR UPDATE
            """, nativeQuery = true)
    List<Long> lockContractsByCompanyId(@Param("companyId") Long companyId);
}
