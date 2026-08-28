package com.vetsoftware.app.bankreceipt.infrastructure.persistence;

import com.vetsoftware.app.bankreceipt.domain.BankReceiptStatus;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <strong>Sin una sola {@code @Query}.</strong> Las tres consultas de la
 * feature las expresa el derivador de nombres de Spring Data, asi que aqui no
 * hay SQL que pueda olvidarse de mover la {@code version} en su {@code SET}
 * ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}, #53) ni
 * {@code UPDATE}/{@code DELETE} al que
 * {@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA} pudiera pedirle un filtro de
 * empresa que la tabla no tiene. Toda escritura pasa por el ciclo
 * leer-modificar-guardar, que es el unico camino que {@code @Version} protege.
 *
 * <p>
 * <strong>Sin {@code @EntityGraph}</strong>, y no por descuido: la entidad no
 * tiene ni una asociacion, asi que no hay N+1 que evitar.
 */
public interface BankReceiptJpaRepository extends JpaRepository<BankReceiptJpaEntity, Long> {

    /**
     * <strong>La igualdad la resuelve el motor bajo la colacion de la columna, que
     * es {@code ascii_bin}</strong> — es decir, byte a byte. Por eso este
     * {@code exists} no descarta como duplicada la segunda consignacion del dia
     * cuando el banco emite {@code AB12} y {@code ab12}: para MySQL son dos cadenas
     * distintas, igual que para {@code uq_bank_receipts_reference}. Si alguien
     * devolviera la columna a la colacion heredada del esquema, este metodo
     * empezaria a mentir sin cambiar una linea de Java.
     */
    boolean existsByBankReferenceAndReceivedOn(String bankReference, LocalDate receivedOn);

    /**
     * La bandeja. El par {@code (status, received_on)} del {@code WHERE} y del
     * {@code ORDER BY} es exactamente {@code ix_bank_receipts_inbox}, en ese orden.
     */
    Page<BankReceiptJpaEntity> findAllByStatus(BankReceiptStatus status, Pageable pageable);
}
