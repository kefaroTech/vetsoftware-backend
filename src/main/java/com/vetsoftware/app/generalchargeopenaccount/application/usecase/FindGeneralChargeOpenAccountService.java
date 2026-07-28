package com.vetsoftware.app.generalchargeopenaccount.application.usecase;

import com.vetsoftware.app.generalchargeopenaccount.application.dto.GeneralChargeOpenAccountDto;
import com.vetsoftware.app.generalchargeopenaccount.application.port.in.FindGeneralChargeOpenAccountUseCase;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.GeneralChargeOpenAccountRepository;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "general.charge.open.account.find")
@Service
public class FindGeneralChargeOpenAccountService implements FindGeneralChargeOpenAccountUseCase {
    private final GeneralChargeOpenAccountRepository repository;

    public FindGeneralChargeOpenAccountService(GeneralChargeOpenAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public GeneralChargeOpenAccountDto findById(Long id, Long companyId) {
        return GeneralChargeOpenAccountDto.from(repository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new GeneralChargeOpenAccountNotFoundException(id)));
    }
}
