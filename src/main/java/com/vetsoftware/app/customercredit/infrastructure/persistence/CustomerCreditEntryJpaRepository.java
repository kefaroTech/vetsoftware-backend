package com.vetsoftware.app.customercredit.infrastructure.persistence;

import com.vetsoftware.app.customercredit.domain.CreditEntryKind;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * <strong>Sin una sola {@code @Query} de {@code UPDATE} ni de
 * {@code DELETE}.</strong> No es una omision: el libro es de solo anadir, asi
 * que no hay nada que actualizar ni que borrar. Corregir un asiento es insertar
 * otro que lo compensa.
 */
public interface CustomerCreditEntryJpaRepository
        extends
            JpaRepository<CustomerCreditEntryJpaEntity, Long> {

    Optional<CustomerCreditEntryJpaEntity> findByIdAndCompanyId(Long id, Long companyId);

    Optional<CustomerCreditEntryJpaEntity> findByCompanyIdAndClientRequestId(Long companyId,
            String clientRequestId);

    /**
     * Las filas de una misma operacion de consumo, que comparten prefijo de llave.
     * Acotado por empresa como todo lo demas.
     */
    List<CustomerCreditEntryJpaEntity> findByCompanyIdAndClientRequestIdStartingWithOrderByIdAsc(
            Long companyId, String clientRequestIdPrefix);

    /**
     * Materia prima del neteo por lotes: las altas de la empresa. Se netean en el
     * adaptador contra sus consumos.
     */
    List<CustomerCreditEntryJpaEntity> findByCompanyIdAndEntryKind(Long companyId,
            CreditEntryKind entryKind);

    /** La otra mitad del neteo: lo que resta de cada lote. */
    List<CustomerCreditEntryJpaEntity> findByCompanyIdAndEntryKindIn(Long companyId,
            Collection<CreditEntryKind> entryKinds);

    Page<CustomerCreditEntryJpaEntity> findAllByCompanyId(Long companyId, Pageable pageable);

    /**
     * <strong>Barrido de plataforma: sin filtro de empresa a proposito.</strong>
     * Las altas de todas las clinicas que caducan antes de una fecha. Recorre todos
     * los tenants porque para eso existe —encontrar el saldo por vencir es una
     * tarea de plataforma— y el indice {@code ix_cce_expiring} lo respalda. El
     * unico caso de uso que llega hasta aqui esta cerrado a
     * {@code hasRole('SYSTEM')} a secas; declararlo asi en el changeset no exime al
     * codigo de esa regla.
     */
    @Query("""
            select e from CustomerCreditEntryJpaEntity e
            where e.entryKind = :entryKind
              and e.expiresOn is not null
              and e.expiresOn < :before
            """)
    Page<CustomerCreditEntryJpaEntity> findAllExpiringBefore(
            @Param("entryKind") CreditEntryKind entryKind, @Param("before") LocalDate before,
            Pageable pageable);
}
