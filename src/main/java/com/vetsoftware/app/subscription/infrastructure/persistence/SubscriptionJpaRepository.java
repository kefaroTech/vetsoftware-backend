package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "company")
    Page<SubscriptionJpaEntity> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "company")
    Optional<SubscriptionJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    @EntityGraph(attributePaths = "company")
    Page<SubscriptionJpaEntity> findAllByCompany_Id(Long companyId, Pageable pageable);

    /**
     * El contrato vigente de una empresa. El filtro de estados lo pone el adaptador
     * con {@code SubscriptionStatus.CURRENT}, que es el unico sitio donde ese
     * criterio esta escrito — el mismo que alimenta {@code active_marker}.
     */
    @EntityGraph(attributePaths = "company")
    Optional<SubscriptionJpaEntity> findFirstByCompany_IdAndStatusIn(Long companyId,
            Collection<SubscriptionStatus> statuses);

    /**
     * Toma la fila del contrato con bloqueo pesimista. Es la primera linea de
     * defensa del solape de lineas: serializa el leer-y-luego-escribir de la
     * comprobacion, igual que en el solape de citas.
     *
     * <p>
     * Va acotada por empresa aunque el {@code id} ya identifique la fila: un
     * bloqueo sobre una fila ajena revelaria su existencia y ademas serviria de
     * puerta a operar sobre ella.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM SubscriptionJpaEntity s
            WHERE s.id = :id
              AND s.company.id = :companyId
            """)
    Optional<SubscriptionJpaEntity> lockByIdAndCompanyId(@Param("id") Long id,
            @Param("companyId") Long companyId);

    @Query(value = """
            SELECT s.*
            FROM subscriptions s
            WHERE s.enabled = TRUE
              AND s.status IN ('TRIALING', 'ACTIVE', 'PAST_DUE', 'READ_ONLY')
              AND s.id > :afterId
            ORDER BY s.id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<SubscriptionJpaEntity> lockLifecycleBatchAfter(@Param("afterId") long afterId,
            @Param("batchSize") int batchSize);
}
