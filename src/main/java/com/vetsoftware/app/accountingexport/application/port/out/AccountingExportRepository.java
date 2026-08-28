package com.vetsoftware.app.accountingexport.application.port.out;

import com.vetsoftware.app.accountingexport.domain.AccountingExport;
import com.vetsoftware.app.accountingexport.domain.AccountingExportKind;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.Optional;

/**
 * <strong>Ningun metodo recibe {@code companyId}</strong>: los libros son de
 * VetSoftware y {@code accounting_exports} no tiene esa columna.
 */
public interface AccountingExportRepository {

    AccountingExport save(AccountingExport export);

    Optional<AccountingExport> findById(Long id);

    /** La bandeja del mes. Sirve a {@code ix_accounting_exports_status}. */
    PageResult<AccountingExport> findAllByPeriodKey(String periodKey, int page, int pageSize);

    /**
     * <strong>Barrido de plataforma</strong>: la bandeja completa, mas reciente
     * primero. Sirve a {@code ix_accounting_exports_generated}, que no lleva
     * empresa delante porque la tabla no la tiene.
     */
    PageResult<AccountingExport> findAll(int page, int pageSize);

    /**
     * El ultimo numero de intento de ese mes y esa clase, o vacio si no hay
     * ninguno.
     *
     * <p>
     * Es lo que permite calcular el siguiente sin pedirselo al llamador. <b>No es
     * una garantia de unicidad</b>: dos generaciones concurrentes leerian el mismo
     * maximo y la segunda chocaria contra {@code uq_accounting_exports_attempt} —
     * que es exactamente lo que tiene que pasar, y la unica respuesta que no
     * miente.
     */
    Optional<Integer> findLastAttemptNumber(String periodKey, AccountingExportKind exportKind);
}
