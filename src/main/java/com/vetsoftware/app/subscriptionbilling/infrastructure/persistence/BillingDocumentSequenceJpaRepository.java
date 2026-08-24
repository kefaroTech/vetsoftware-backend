package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * El consecutivo interno.
 *
 * <p>
 * Las dos consultas de abajo son <b>una sola operación lógica</b> y se ejecutan
 * pegadas, dentro de la transacción del caso de uso. Ver
 * {@code JpaBillingDocumentSequenceRepository#nextNumber}.
 */
public interface BillingDocumentSequenceJpaRepository
        extends
            JpaRepository<BillingDocumentSequenceJpaEntity, Long> {

    Optional<BillingDocumentSequenceJpaEntity> findByPrefix(String prefix);

    /**
     * Lee el siguiente número <b>con la fila bloqueada</b>.
     *
     * <p>
     * Nativa para poder usar {@code FOR UPDATE}, que es lo que impide la carrera
     * que un «máximo más uno» no puede evitar: dos procesos simultáneos leerían el
     * mismo valor y se lo darían a dos documentos distintos. El segundo espera aquí
     * hasta que el primero confirme.
     *
     * <p>
     * Devuelve el valor y no la entidad a propósito: cargar la entidad la metería
     * en el contexto de persistencia y el {@code UPDATE} de la línea siguiente
     * podría acabar pisado por un <i>flush</i> con el valor viejo.
     */
    @Query(value = """
            SELECT next_value
            FROM billing_document_sequences
            WHERE prefix = :prefix
            FOR UPDATE
            """, nativeQuery = true)
    Optional<Long> lockNextValue(@Param("prefix") String prefix);

    /**
     * Incrementa la serie.
     *
     * <p>
     * <b>No nombra ninguna empresa, y es correcto</b>: esta tabla no tiene
     * {@code company_id} ni llega a {@code companies} por ningún camino, así que
     * {@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA} ni la mira — no hay empresa que
     * nombrar. <b>Tampoco mueve ninguna {@code version}</b>, porque la entidad va
     * exenta ({@code E6_YA_PROTEGIDO}): el bloqueo pesimista de arriba ya serializa
     * el incremento, y un 409 aquí rompería una emisión en vez de proteger nada.
     *
     * <p>
     * {@code next_value = next_value + 1} y no un valor calculado en Java: el
     * incremento lo hace el motor sobre la fila que este proceso ya tiene
     * bloqueada.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE billing_document_sequences
            SET next_value = next_value + 1
            WHERE prefix = :prefix
            """, nativeQuery = true)
    int advance(@Param("prefix") String prefix);
}
