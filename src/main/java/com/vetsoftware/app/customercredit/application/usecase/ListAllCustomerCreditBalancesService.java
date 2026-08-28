package com.vetsoftware.app.customercredit.application.usecase;

import com.vetsoftware.app.customercredit.application.dto.CustomerCreditBalanceDto;
import com.vetsoftware.app.customercredit.application.port.in.ListAllCustomerCreditBalancesUseCase;
import com.vetsoftware.app.customercredit.application.port.out.CustomerCreditBalanceRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/** Barrido de plataforma: saldos vivos de todas las clinicas. */
@Observed(name = "customer.credit.balance.list.all")
@Service
public class ListAllCustomerCreditBalancesService implements ListAllCustomerCreditBalancesUseCase {

    private final CustomerCreditBalanceRepository repository;

    public ListAllCustomerCreditBalancesService(CustomerCreditBalanceRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<CustomerCreditBalanceDto> listAll(int page, int pageSize) {
        return repository.findAll(page, pageSize).map(CustomerCreditBalanceDto::from);
    }
}
