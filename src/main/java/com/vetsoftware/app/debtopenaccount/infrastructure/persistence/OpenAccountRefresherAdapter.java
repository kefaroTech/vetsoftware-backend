package com.vetsoftware.app.debtopenaccount.infrastructure.persistence;

import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.openaccount.application.port.in.RecalculateOpenAccountUseCase;
import org.springframework.stereotype.Component;

@Component
public class OpenAccountRefresherAdapter implements OpenAccountRefresher {
    private final RecalculateOpenAccountUseCase recalculateOpenAccountUseCase;

    public OpenAccountRefresherAdapter(RecalculateOpenAccountUseCase recalculateOpenAccountUseCase) {
        this.recalculateOpenAccountUseCase = recalculateOpenAccountUseCase;
    }

    @Override
    public void refresh(Long openAccountId) {
        recalculateOpenAccountUseCase.recalculate(openAccountId);
    }
}
