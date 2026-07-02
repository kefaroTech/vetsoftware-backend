package com.vetsoftware.app.generalchargeopenaccount.application.usecase;

import com.vetsoftware.app.generalchargeopenaccount.application.dto.GeneralChargeOpenAccountDto;
import com.vetsoftware.app.generalchargeopenaccount.application.port.in.ListGeneralChargeOpenAccountsUseCase;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.GeneralChargeOpenAccountRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "general_charge_open_account.list_all")
@Service
public class ListGeneralChargeOpenAccountsService implements ListGeneralChargeOpenAccountsUseCase {
    private final GeneralChargeOpenAccountRepository repository;

    public ListGeneralChargeOpenAccountsService(GeneralChargeOpenAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<GeneralChargeOpenAccountDto> listAll(Long companyId) {
        return repository.findAllByCompanyId(companyId).stream().map(GeneralChargeOpenAccountDto::from).toList();
    }
}
