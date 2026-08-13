package com.vetsoftware.app.openaccount.application.usecase;

import com.vetsoftware.app.openaccount.application.dto.OpenAccountsSummaryDto;
import com.vetsoftware.app.openaccount.application.port.in.GetOpenAccountsSummaryUseCase;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "open.account.summary")
@Service
public class GetOpenAccountsSummaryService implements GetOpenAccountsSummaryUseCase {
    private final OpenAccountRepository repository;

    public GetOpenAccountsSummaryService(OpenAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public OpenAccountsSummaryDto summarize(Long companyId, Long branchId) {
        return repository.summarize(companyId, branchId);
    }
}
