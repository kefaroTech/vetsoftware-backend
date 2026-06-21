package com.vetsoftware.app.generalchargeopenaccount.infrastructure.persistence;

import com.vetsoftware.app.openaccount.application.port.in.AssertOpenAccountVersionUseCase;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountVersionGuard;
import org.springframework.stereotype.Component;

@Component
public class OpenAccountVersionGuardAdapter implements OpenAccountVersionGuard {
    private final AssertOpenAccountVersionUseCase assertVersionUseCase;

    public OpenAccountVersionGuardAdapter(AssertOpenAccountVersionUseCase assertVersionUseCase) {
        this.assertVersionUseCase = assertVersionUseCase;
    }

    @Override
    public void assertVersion(Long openAccountId, Long expectedVersion) {
        assertVersionUseCase.assertVersion(openAccountId, expectedVersion);
    }
}
