package com.vetsoftware.app.generalchargeopenaccount.infrastructure.persistence;

import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.openaccount.application.port.in.RecalculateOpenAccountUseCase;
import org.springframework.stereotype.Component;

@Component
public class OpenAccountRefresherAdapter implements OpenAccountRefresher {
    private final RecalculateOpenAccountUseCase recalculateOpenAccountUseCase;

    public OpenAccountRefresherAdapter(
            RecalculateOpenAccountUseCase recalculateOpenAccountUseCase) {
        this.recalculateOpenAccountUseCase = recalculateOpenAccountUseCase;
    }

    @Override
    public void refresh(Long companyId, Long openAccountId) {
        recalculateOpenAccountUseCase.recalculate(companyId, openAccountId);
    }
}
