package com.vetsoftware.app.bankreceipt.application.port.out;

import com.vetsoftware.app.bankreceipt.domain.BankReceipt;
import com.vetsoftware.app.bankreceipt.domain.BankReceiptStatus;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.time.LocalDate;
import java.util.Optional;

/**
 * <strong>No hay variante acotada por empresa de nada, y no falta
 * ninguna.</strong> La tabla no tiene {@code company_id}: no existe la consulta
 * acotada que {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} echaria de menos, ni el
 * filtro que {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} exigiria. Lo que sostiene
 * el aislamiento aqui no es un {@code WHERE}: es que los seis puertos de
 * entrada de la feature estan cerrados a {@code hasRole('SYSTEM')} a secas.
 *
 * <p>
 * <strong>Y no hay borrado, ni logico ni fisico.</strong> Ni {@code delete}, ni
 * {@code disable}, ni reactivacion. Una entrada del extracto que no corresponde
 * a nadie se marca {@code DISCARDED} y queda.
 */
public interface BankReceiptRepository {

    BankReceipt save(BankReceipt receipt);

    Optional<BankReceipt> findById(Long id);

    /**
     * Si esa referencia bancaria ya entro con esa fecha.
     *
     * <p>
     * Se consulta <strong>antes</strong> de insertar porque
     * {@code uq_bank_receipts_reference} convierte el duplicado en un error del
     * driver, y recargar dos veces el extracto del mes —el error mas comun de este
     * proceso, que se hace a mano— merece un conflicto legible y no un 500.
     *
     * <p>
     * <strong>La comparacion es exacta</strong>, por la colacion {@code ascii_bin}
     * de la columna: dos referencias que solo difieren en mayusculas son entradas
     * distintas y las dos entran.
     */
    boolean existsByBankReferenceAndReceivedOn(String bankReference, LocalDate receivedOn);

    /** Barrido completo del extracto. Solo lo consume un puerto SYSTEM. */
    PageResult<BankReceipt> findAll(int page, int pageSize);

    /**
     * La bandeja. Recorre {@code ix_bank_receipts_inbox (status, received_on)} en
     * el mismo orden en que el indice esta escrito, que es lo que hace que la
     * consulta mas frecuente de la feature no ordene en memoria.
     */
    PageResult<BankReceipt> findAllByStatus(BankReceiptStatus status, int page, int pageSize);
}
