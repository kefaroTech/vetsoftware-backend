package com.vetsoftware.app.paymentattempt.infrastructure.persistence;

import com.vetsoftware.app.paymentattempt.application.port.out.PaymentAttemptRepository;
import com.vetsoftware.app.paymentattempt.domain.DeclineKind;
import com.vetsoftware.app.paymentattempt.domain.PaymentAttempt;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPaymentAttemptRepository implements PaymentAttemptRepository {

    private final PaymentAttemptJpaRepository jpaRepository;
    private final PaymentAttemptJpaMapper mapper;

    public JpaPaymentAttemptRepository(PaymentAttemptJpaRepository jpaRepository,
            PaymentAttemptJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public PaymentAttempt save(PaymentAttempt attempt) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(attempt)));
    }

    @Override
    public Optional<PaymentAttempt> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompanyId(id, companyId).map(mapper::toDomain);
    }

    /**
     * Orden total y estable: lo mas reciente primero, con el {@code id} de
     * desempate. Sin desempate, dos intentos del mismo microsegundo pueden salir en
     * dos paginas o en ninguna.
     */
    @Override
    public PageResult<PaymentAttempt> findAllByCompanyId(Long companyId, int page, int pageSize) {
        return Pages.result(jpaRepository.findAllByCompanyId(companyId,
                Pages.request(page, pageSize, mostRecentFirst())), mapper::toDomain);
    }

    /**
     * Aqui el orden es <strong>ascendente</strong> y por numero de intento: es el
     * historial de una factura, y "se intento cuatro veces" se lee del primero al
     * ultimo. El {@code id} desempata igual.
     */
    @Override
    public PageResult<PaymentAttempt> findAllByCompanyIdAndBillingDocumentId(Long companyId,
            Long billingDocumentId, int page, int pageSize) {
        Sort order = Sort.by(Sort.Direction.ASC, "attemptNumber")
                .and(Sort.by(Sort.Direction.ASC, "id"));
        return Pages.result(jpaRepository.findAllByCompanyIdAndBillingDocumentId(companyId,
                billingDocumentId, Pages.request(page, pageSize, order)), mapper::toDomain);
    }

    /** Lo mas vencido primero: la cola se atiende por antiguedad. */
    @Override
    public PageResult<PaymentAttempt> findAllDueForRetry(LocalDateTime dueBefore, int page,
            int pageSize) {
        Sort order = Sort.by(Sort.Direction.ASC, "nextAttemptAt")
                .and(Sort.by(Sort.Direction.ASC, "id"));
        return Pages.result(
                jpaRepository.findAllDueForRetry(dueBefore, Pages.request(page, pageSize, order)),
                mapper::toDomain);
    }

    @Override
    public PageResult<PaymentAttempt> findAll(int page, int pageSize) {
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, mostRecentFirst())),
                mapper::toDomain);
    }

    @Override
    public Optional<Integer> findMaxAttemptNumber(Long companyId, Long billingDocumentId) {
        return Optional
                .ofNullable(jpaRepository.findMaxAttemptNumber(companyId, billingDocumentId));
    }

    /**
     * El {@code CONFIGURATION} se excluye aqui, en el borde: es un fallo propio y
     * no gasta el presupuesto del cliente.
     */
    @Override
    public int countRetryableSince(Long companyId, Long billingDocumentId, LocalDateTime since) {
        return Math.toIntExact(jpaRepository.countChargeableSince(companyId, billingDocumentId,
                since, DeclineKind.CONFIGURATION));
    }

    private static Sort mostRecentFirst() {
        return Sort.by(Sort.Direction.DESC, "attemptedAt").and(Sort.by(Sort.Direction.DESC, "id"));
    }
}
