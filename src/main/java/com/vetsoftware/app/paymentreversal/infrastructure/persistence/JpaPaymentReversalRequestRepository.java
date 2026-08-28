package com.vetsoftware.app.paymentreversal.infrastructure.persistence;

import com.vetsoftware.app.paymentreversal.application.port.out.PaymentReversalRequestRepository;
import com.vetsoftware.app.paymentreversal.domain.PaymentReversalRequest;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPaymentReversalRequestRepository implements PaymentReversalRequestRepository {

    private final PaymentReversalRequestJpaRepository jpaRepository;
    private final PaymentReversalRequestJpaMapper mapper;

    public JpaPaymentReversalRequestRepository(PaymentReversalRequestJpaRepository jpaRepository,
            PaymentReversalRequestJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public PaymentReversalRequest save(PaymentReversalRequest request) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(request)));
    }

    @Override
    public Optional<PaymentReversalRequest> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompanyId(id, companyId).map(mapper::toDomain);
    }

    @Override
    public Optional<PaymentReversalRequest> findByCompanyIdAndPaymentId(Long companyId,
            Long paymentId) {
        return jpaRepository.findByCompanyIdAndPaymentId(companyId, paymentId)
                .map(mapper::toDomain);
    }

    /**
     * Lo mas reciente primero por fecha de queja, con el {@code id} de desempate.
     * Sin desempate, dos expedientes del mismo microsegundo pueden salir en dos
     * paginas o en ninguna.
     */
    @Override
    public PageResult<PaymentReversalRequest> findAllByCompanyId(Long companyId, int page,
            int pageSize) {
        return Pages.result(jpaRepository.findAllByCompanyId(companyId,
                Pages.request(page, pageSize, claimOrder())), mapper::toDomain);
    }

    /**
     * Orden inverso al del resto: lo que <strong>antes vence</strong> primero, que
     * es la unica ordenacion util en una cola de plazos.
     */
    @Override
    public PageResult<PaymentReversalRequest> findAllExpiringBefore(LocalDateTime before, int page,
            int pageSize) {
        Sort order = Sort.by(Sort.Direction.ASC, "deadlineAt")
                .and(Sort.by(Sort.Direction.ASC, "id"));
        return Pages.result(
                jpaRepository.findAllExpiringBefore(before, Pages.request(page, pageSize, order)),
                mapper::toDomain);
    }

    @Override
    public PageResult<PaymentReversalRequest> findAll(int page, int pageSize) {
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, claimOrder())),
                mapper::toDomain);
    }

    private static Sort claimOrder() {
        return Sort.by(Sort.Direction.DESC, "claimReceivedAt")
                .and(Sort.by(Sort.Direction.DESC, "id"));
    }
}
