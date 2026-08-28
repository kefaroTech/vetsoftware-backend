package com.vetsoftware.app.paymentrefund.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * <strong>Sin ninguna {@code @Query} de {@code UPDATE} ni de
 * {@code DELETE}</strong>, y no es que aun no hayan hecho falta: la tabla solo
 * se agrega. Por eso tampoco le aplica {@code UPDATE_MASIVO_MUEVE_LA_VERSION}
 * -no hay {@code UPDATE} que pueda olvidarse de mover una version que la tabla
 * ni siquiera tiene- ni {@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA}.
 *
 * <p>
 * La unica {@code @Query} es de lectura: la suma de lo devuelto, que es la
 * mitad del tope que la base no puede expresar.
 */
public interface PaymentRefundJpaRepository extends JpaRepository<PaymentRefundJpaEntity, Long> {

    Optional<PaymentRefundJpaEntity> findByIdAndCompanyId(Long id, Long companyId);

    Optional<PaymentRefundJpaEntity> findByCompanyIdAndClientRequestId(Long companyId,
            String clientRequestId);

    /**
     * {@code coalesce} y no un {@code Optional}: sin devoluciones previas el
     * {@code SUM} de SQL vale {@code NULL}, y ese {@code null} viajaria hasta la
     * suma del tope y lo convertiria en un {@code NullPointerException} justo en el
     * camino que devuelve dinero. Cero es la respuesta correcta y es la que sale de
     * aqui.
     *
     * <p>
     * Acotada por empresa aunque {@code paymentId} ya parezca suficiente: una FK
     * ajena no es un filtro de tenant -el pago es de alguien-, mismo criterio que
     * BE-29.
     */
    @Query("""
            select coalesce(sum(r.amount), 0)
            from PaymentRefundJpaEntity r
            where r.paymentId = :paymentId and r.companyId = :companyId
            """)
    BigDecimal sumRefundedByPaymentAndCompanyId(@Param("paymentId") Long paymentId,
            @Param("companyId") Long companyId);

    Page<PaymentRefundJpaEntity> findAllByCompanyId(Long companyId, Pageable pageable);

    Page<PaymentRefundJpaEntity> findAllByCompanyIdAndPaymentId(Long companyId, Long paymentId,
            Pageable pageable);
}
