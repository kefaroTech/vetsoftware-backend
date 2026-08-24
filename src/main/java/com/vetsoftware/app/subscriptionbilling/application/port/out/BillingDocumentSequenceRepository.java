package com.vetsoftware.app.subscriptionbilling.application.port.out;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionbilling.domain.BillingDocumentSequence;
import com.vetsoftware.app.subscriptionbilling.domain.DocumentNumber;
import java.util.Optional;

/**
 * El consecutivo interno de los documentos de cobro.
 *
 * <p>
 * <b>Sin empresa</b>: es un contador global de plataforma, la única tabla de
 * este slice que no lleva {@code company_id}.
 */
public interface BillingDocumentSequenceRepository {

    /**
     * Consume el siguiente número de la serie: lo lee y lo incrementa <b>en la
     * misma operación, con la fila bloqueada</b>.
     *
     * <p>
     * <b>Un consecutivo no se saca de un «máximo más uno»</b>: dos procesos
     * simultáneos leerían el mismo máximo y le darían el mismo número a dos
     * documentos distintos. El bloqueo pesimista sobre la fila del prefijo es lo
     * que serializa el <i>read-then-write</i>.
     *
     * <p>
     * <b>Se ejecuta dentro de la transacción del caso de uso</b>, nunca en un
     * {@code REQUIRES_NEW}: si el documento no llega a existir, el incremento se
     * deshace con él y la serie no deja huecos. Es la diferencia deliberada con el
     * consecutivo fiscal de la DIAN, que sí se reserva aparte porque allí el hueco
     * es lo prohibido.
     *
     * @throws com.vetsoftware.app.subscriptionbilling.domain.BillingDocumentSequenceNotFoundException
     *             si no hay serie declarada para ese prefijo. No se crea sola: una
     *             serie que se autocrea arranca en 1 sin que nadie lo haya decidido
     */
    DocumentNumber nextNumber(String prefix);

    BillingDocumentSequence save(BillingDocumentSequence sequence);

    Optional<BillingDocumentSequence> findByPrefix(String prefix);

    PageResult<BillingDocumentSequence> findAll(int page, int pageSize);
}
