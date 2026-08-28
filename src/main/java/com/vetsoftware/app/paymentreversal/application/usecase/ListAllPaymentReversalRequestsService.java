package com.vetsoftware.app.paymentreversal.application.usecase;

import com.vetsoftware.app.paymentreversal.application.dto.PaymentReversalRequestDto;
import com.vetsoftware.app.paymentreversal.application.port.in.ListAllPaymentReversalRequestsUseCase;
import com.vetsoftware.app.paymentreversal.application.port.out.PaymentReversalRequestRepository;
import com.vetsoftware.app.paymentreversal.domain.PaymentReversalRequest;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * {@code companyId} es un filtro opcional: si llega, delega en la consulta
 * acotada; si no, en el barrido cross-tenant. Las dos ramas conviven en la
 * misma clase a proposito, que es el patron que las reglas de BE-COV reconocen
 * como legitimo.
 */
@Observed(name = "payment.reversal.list.all")
@Service
public class ListAllPaymentReversalRequestsService
        implements
            ListAllPaymentReversalRequestsUseCase {

    private final PaymentReversalRequestRepository repository;

    public ListAllPaymentReversalRequestsService(PaymentReversalRequestRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<PaymentReversalRequestDto> listAll(Long companyId, int page, int pageSize) {
        PageResult<PaymentReversalRequest> found = companyId == null
                ? repository.findAll(page, pageSize)
                : repository.findAllByCompanyId(companyId, page, pageSize);
        return found.map(PaymentReversalRequestDto::from);
    }
}
