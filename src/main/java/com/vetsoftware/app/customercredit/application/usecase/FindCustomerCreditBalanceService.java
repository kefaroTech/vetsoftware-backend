package com.vetsoftware.app.customercredit.application.usecase;

import com.vetsoftware.app.customercredit.application.dto.CustomerCreditBalanceDto;
import com.vetsoftware.app.customercredit.application.port.in.FindCustomerCreditBalanceUseCase;
import com.vetsoftware.app.customercredit.application.port.out.CustomerCreditBalanceRepository;
import com.vetsoftware.app.customercredit.domain.CustomerCreditBalanceNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "customer.credit.balance.find")
@Service
public class FindCustomerCreditBalanceService implements FindCustomerCreditBalanceUseCase {

    private final CustomerCreditBalanceRepository repository;

    public FindCustomerCreditBalanceService(CustomerCreditBalanceRepository repository) {
        this.repository = repository;
    }

    /**
     * Que no exista la fila no se traduce a un cero: significa que a esta empresa
     * nunca se le abono nada. Inventar el cero haria indistinguibles «no tiene
     * saldo» y «nadie ha calculado su saldo», y la segunda es la que hay que ver en
     * un cuadre.
     */
    @Override
    public CustomerCreditBalanceDto findByCompanyId(Long companyId) {
        return repository.findByCompanyId(companyId).map(CustomerCreditBalanceDto::from)
                .orElseThrow(() -> new CustomerCreditBalanceNotFoundException(companyId));
    }
}
