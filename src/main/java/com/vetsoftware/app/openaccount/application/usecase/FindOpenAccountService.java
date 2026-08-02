package com.vetsoftware.app.openaccount.application.usecase;

import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import com.vetsoftware.app.openaccount.application.port.in.FindOpenAccountUseCase;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.domain.OpenAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "open.account.find")
@Service
public class FindOpenAccountService implements FindOpenAccountUseCase {
    private final OpenAccountRepository repository;

    public FindOpenAccountService(OpenAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public OpenAccountDto findById(Long id, Long companyId) {
        return OpenAccountDto.from(repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new OpenAccountNotFoundException(id)));
    }
}
