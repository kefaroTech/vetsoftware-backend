package com.vetsoftware.app.subscriptionpayment.application.port.out;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionpayment.domain.BillingDocumentApplication;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BillingDocumentApplicationRepository {

    BillingDocumentApplication save(BillingDocumentApplication application);

    Optional<BillingDocumentApplication> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Suma <strong>neta</strong> de lo aplicado desde un pago. Neta porque las
     * contra-aplicaciones son negativas: revertir una aplicacion libera su importe
     * para volver a aplicarlo, que es justamente lo que tiene que pasar.
     *
     * <p>
     * Devuelve cero, nunca {@code null}, para que el llamante no tenga que
     * distinguir "sin aplicaciones" de "aplicado cero".
     */
    BigDecimal sumAppliedFromPayment(Long paymentId, Long companyId);

    /** Suma neta de lo aplicado desde una nota credito. Mismo criterio. */
    BigDecimal sumAppliedFromSourceDocument(Long sourceDocumentId, Long companyId);

    /**
     * R3 sobre una retencion. Mismo criterio de suma neta.
     *
     * <p>
     * <strong>Sin esto, la misma retencion podria saldar la factura dos
     * veces.</strong> El certificado dice 7.160 y la cartera bajaria 14.320: la
     * clinica quedaria a paz y salvo por dinero que la DIAN nunca recibio, y el
     * descuadre saldria a la luz un ano despues, al conciliar los certificados.
     */
    BigDecimal sumAppliedFromWithholding(Long withholdingId, Long companyId);

    /**
     * R3 sobre un lote de saldo a favor. Mismo criterio de suma neta.
     *
     * <p>
     * El techo es lo que el lote concedio. Sin el, un lote de 100.000 podria saldar
     * trescientos mil: el saldo a favor dejaria de ser un saldo y pasaria a ser una
     * linea de credito sin limite.
     */
    BigDecimal sumAppliedFromCreditEntry(Long creditEntryId, Long companyId);

    /**
     * Aplicacion ya registrada con esta llave de idempotencia (R13). Se consulta
     * <strong>antes</strong> de insertar y dentro del bloqueo del origen: la
     * constraint unica convierte el duplicado en un error, y un 500 de clave
     * duplicada no es una respuesta idempotente.
     *
     * <p>
     * Acotada por empresa a proposito. A diferencia del par
     * {@code (gateway, gateway_reference)} de los pagos, aqui no hay un tercero que
     * genere la referencia: la escribe el propio cliente, asi que dos clinicas
     * pueden elegir la misma cadena sin que eso signifique nada.
     */
    Optional<BillingDocumentApplication> findByCompanyIdAndClientRequestId(Long companyId,
            String clientRequestId);

    /**
     * La contra-aplicacion que ya revierte esta aplicacion, si existe. Se consulta
     * antes de insertar para que un doble clic devuelva la reversa que ya se creo
     * en vez de chocar con {@code uq_bda_reversal}.
     */
    Optional<BillingDocumentApplication> findByReversalOfIdAndCompanyId(Long reversalOfId,
            Long companyId);

    /**
     * Facturas que este pago toca, sin repetir.
     *
     * <p>
     * Es lo que permite que confirmar o devolver un pago arrastre el recalculo de
     * su {@code settled_amount} (R4). Sin esto, aplicar un pago {@code PENDING} y
     * confirmarlo despues dejaria el saldo sin bajar: la clinica pago, el sistema
     * no se entera y la mora sigue corriendo.
     */
    List<Long> findTargetDocumentIdsByPaymentId(Long paymentId, Long companyId);

    PageResult<BillingDocumentApplication> findAllByTargetDocumentIdAndCompanyId(
            Long targetDocumentId, Long companyId, int page, int pageSize);
}
