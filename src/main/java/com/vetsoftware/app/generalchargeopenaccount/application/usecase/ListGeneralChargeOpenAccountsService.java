package com.vetsoftware.app.generalchargeopenaccount.application.usecase;

import com.vetsoftware.app.generalchargeopenaccount.application.dto.GeneralChargeOpenAccountDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.generalchargeopenaccount.application.port.in.ListGeneralChargeOpenAccountsUseCase;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.GeneralChargeOpenAccountRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "general.charge.open.account.list.all")
@Service
public class ListGeneralChargeOpenAccountsService implements ListGeneralChargeOpenAccountsUseCase {
    private final GeneralChargeOpenAccountRepository repository;

    public ListGeneralChargeOpenAccountsService(GeneralChargeOpenAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<GeneralChargeOpenAccountDto> listAll(Long companyId, int page, int pageSize) {
        return repository.findAllByCompanyId(companyId, page, pageSize)
                .map(GeneralChargeOpenAccountDto::from);
    }
}
