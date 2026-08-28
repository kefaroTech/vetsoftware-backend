package com.vetsoftware.app.paymentrefund.application.port.out;

import com.vetsoftware.app.paymentrefund.domain.PaymentRefund;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * <strong>No existe ningun {@code findById(Long)} ancho, y es
 * deliberado.</strong> {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} (BE-COV) marca
 * al caso de uso que conoce la variante ancha y no la acotada; la forma de no
 * poder equivocarse es que la ancha no exista. Toda lectura por id de este
 * slice lleva la empresa.
 *
 * <p>
 * Y <strong>ninguna escritura salvo {@code save}</strong>: la tabla solo se
 * agrega. No hay {@code update}, no hay {@code delete} y no hay reactivacion,
 * porque una devolucion no se edita ni se oculta -se compensa con otra fila-.
 */
public interface PaymentRefundRepository {

    PaymentRefund save(PaymentRefund refund);

    Optional<PaymentRefund> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Devolucion ya registrada con esta llave de idempotencia. Se consulta
     * <strong>antes</strong> de insertar (R13): la constraint
     * {@code uq_payment_refunds_client_request} convierte el duplicado en un error,
     * y un 500 en la cara del operador no es una respuesta idempotente -vuelve a
     * darle al boton-.
     */
    Optional<PaymentRefund> findByCompanyIdAndClientRequestId(Long companyId,
            String clientRequestId);

    /**
     * Lo ya devuelto sobre un pago. Es la mitad del tope que la base no puede
     * cuidar: MySQL prohibe subconsultas en un {@code CHECK}, asi que «la suma de
     * devoluciones no supera el pago» solo se puede comprobar preguntando.
     *
     * <p>
     * Devuelve {@link BigDecimal#ZERO} cuando no hay ninguna, nunca {@code null}:
     * un {@code null} aqui se propagaria a una suma y convertiria el tope en un
     * {@code NullPointerException} en mitad de una devolucion.
     */
    BigDecimal sumRefundedByPaymentAndCompanyId(Long paymentId, Long companyId);

    PageResult<PaymentRefund> findAllByCompanyId(Long companyId, int page, int pageSize);

    PageResult<PaymentRefund> findAllByCompanyIdAndPaymentId(Long companyId, Long paymentId,
            int page, int pageSize);

    /** Barrido de plataforma cross-tenant. Solo lo consume un puerto SYSTEM. */
    PageResult<PaymentRefund> findAll(int page, int pageSize);
}
