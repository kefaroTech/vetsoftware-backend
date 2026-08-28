package com.vetsoftware.app.entitlement.application.usecase;

import com.vetsoftware.app.entitlement.application.dto.CompanyCapacityDto;
import com.vetsoftware.app.entitlement.application.port.in.ListUnreconciledCapacityCountersUseCase;
import com.vetsoftware.app.entitlement.application.port.out.UnreconciledCapacityQueryPort;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lee el lote pendiente de recuento. No cuenta ni corrige nada. */
@Service
public class ListUnreconciledCapacityCountersService
        implements
            ListUnreconciledCapacityCountersUseCase {

    private final UnreconciledCapacityQueryPort queryPort;

    public ListUnreconciledCapacityCountersService(UnreconciledCapacityQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyCapacityDto> list(LocalDateTime staleBefore, long afterId, int limit) {
        return queryPort.findUnreconciled(staleBefore, afterId, limit).stream()
                .map(CompanyCapacityDto::from).toList();
    }
}
