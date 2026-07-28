package com.vetsoftware.app.generalchargeopenaccount.application.usecase;

import com.vetsoftware.app.generalchargeopenaccount.application.dto.GeneralChargeOpenAccountDto;
import com.vetsoftware.app.generalchargeopenaccount.application.port.in.ListGeneralChargeOpenAccountsByOpenAccountUseCase;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.GeneralChargeOpenAccountRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "general.charge.open.account.list.by.open.account")
@Service
public class ListGeneralChargeOpenAccountsByOpenAccountService
        implements ListGeneralChargeOpenAccountsByOpenAccountUseCase {
    private final GeneralChargeOpenAccountRepository repository;

    public ListGeneralChargeOpenAccountsByOpenAccountService(GeneralChargeOpenAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<GeneralChargeOpenAccountDto> listByOpenAccount(Long openAccountId, Long companyId) {
        return repository.findByOpenAccountIdAndCompanyId(openAccountId, companyId).stream()
            .map(GeneralChargeOpenAccountDto::from).toList();
    }
}
