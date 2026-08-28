package com.vetsoftware.app.paymentrefund.infrastructure.persistence;

import com.vetsoftware.app.paymentrefund.application.port.out.PaymentRefundRepository;
import com.vetsoftware.app.paymentrefund.domain.PaymentRefund;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPaymentRefundRepository implements PaymentRefundRepository {

    private final PaymentRefundJpaRepository jpaRepository;
    private final PaymentRefundJpaMapper mapper;

    public JpaPaymentRefundRepository(PaymentRefundJpaRepository jpaRepository,
            PaymentRefundJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public PaymentRefund save(PaymentRefund refund) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(refund)));
    }

    @Override
    public Optional<PaymentRefund> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompanyId(id, companyId).map(mapper::toDomain);
    }

    @Override
    public Optional<PaymentRefund> findByCompanyIdAndClientRequestId(Long companyId,
            String clientRequestId) {
        return jpaRepository.findByCompanyIdAndClientRequestId(companyId, clientRequestId)
                .map(mapper::toDomain);
    }

    @Override
    public BigDecimal sumRefundedByPaymentAndCompanyId(Long paymentId, Long companyId) {
        BigDecimal sum = jpaRepository.sumRefundedByPaymentAndCompanyId(paymentId, companyId);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    @Override
    public PageResult<PaymentRefund> findAllByCompanyId(Long companyId, int page, int pageSize) {
        return Pages.result(
                jpaRepository.findAllByCompanyId(companyId, Pages.request(page, pageSize, order())),
                mapper::toDomain);
    }

    @Override
    public PageResult<PaymentRefund> findAllByCompanyIdAndPaymentId(Long companyId, Long paymentId,
            int page, int pageSize) {
        return Pages.result(jpaRepository.findAllByCompanyIdAndPaymentId(companyId, paymentId,
                Pages.request(page, pageSize, order())), mapper::toDomain);
    }

    @Override
    public PageResult<PaymentRefund> findAll(int page, int pageSize) {
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, order())),
                mapper::toDomain);
    }

    /**
     * Orden total y estable: lo mas reciente primero por fecha de devolucion, con
     * el {@code id} de desempate. Sin desempate, dos devoluciones del mismo
     * microsegundo -las dos mitades de un reintento, por ejemplo- pueden salir en
     * dos paginas o en ninguna.
     */
    private static Sort order() {
        return Sort.by(Sort.Direction.DESC, "refundedAt").and(Sort.by(Sort.Direction.DESC, "id"));
    }
}
