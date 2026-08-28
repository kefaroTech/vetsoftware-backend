package com.vetsoftware.app.accountingexport.infrastructure.persistence;

import com.vetsoftware.app.accountingexport.domain.AccountingExportKind;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <strong>Sin una sola {@code @Query}.</strong> Las tres consultas las expresa
 * el derivador de nombres de Spring Data, asi que aqui no hay SQL que pueda
 * olvidarse de mover la {@code version} en su {@code SET}
 * ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}) ni proyectar un literal booleano
 * ({@code PROYECCION_SIN_LITERAL_BOOLEANO}). Los tres desenlaces pasan por el
 * ciclo leer-modificar-guardar, que es el unico camino que {@code @Version}
 * protege.
 *
 * <p>
 * Ningun metodo recibe {@code companyId} porque la tabla no tiene esa columna.
 */
public interface AccountingExportJpaRepository
        extends
            JpaRepository<AccountingExportJpaEntity, Long> {

    Page<AccountingExportJpaEntity> findAllByPeriodKey(String periodKey, Pageable pageable);

    /**
     * El ultimo intento de ese mes y esa clase.
     *
     * <p>
     * <strong>Consulta derivada y no un {@code select max(...)}</strong>: pedir la
     * primera fila del orden descendente da lo mismo y se apoya en
     * {@code uq_accounting_exports_attempt}, que ya indexa
     * {@code (period_key, export_kind, attempt_number)} — el maximo se resuelve por
     * el extremo del indice, sin agregado.
     */
    Optional<AccountingExportJpaEntity> findFirstByPeriodKeyAndExportKindOrderByAttemptNumberDesc(
            String periodKey, AccountingExportKind exportKind);
}
