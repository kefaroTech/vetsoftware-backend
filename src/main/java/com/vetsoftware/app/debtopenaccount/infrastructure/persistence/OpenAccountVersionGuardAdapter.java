package com.vetsoftware.app.debtopenaccount.infrastructure.persistence;

import com.vetsoftware.app.openaccount.application.port.in.AssertOpenAccountVersionUseCase;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountVersionGuard;
import org.springframework.stereotype.Component;

@Component
public class OpenAccountVersionGuardAdapter implements OpenAccountVersionGuard {
    private final AssertOpenAccountVersionUseCase assertVersionUseCase;

    public OpenAccountVersionGuardAdapter(AssertOpenAccountVersionUseCase assertVersionUseCase) {
        this.assertVersionUseCase = assertVersionUseCase;
    }

    @Override
    public void assertVersion(Long companyId, Long openAccountId, Long expectedVersion) {
        assertVersionUseCase.assertVersion(companyId, openAccountId, expectedVersion);
    }
}
