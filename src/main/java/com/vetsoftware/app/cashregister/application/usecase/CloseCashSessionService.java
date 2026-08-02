package com.vetsoftware.app.cashregister.application.usecase;

import com.vetsoftware.app.cashregister.application.command.CloseCashSessionCommand;
import com.vetsoftware.app.cashregister.application.dto.CashSessionView;
import com.vetsoftware.app.cashregister.application.port.in.CloseCashSessionUseCase;
import com.vetsoftware.app.cashregister.application.port.out.CashMetrics;
import com.vetsoftware.app.cashregister.application.port.out.CashSessionRepository;
import com.vetsoftware.app.cashregister.domain.CashPaymentMethod;
import com.vetsoftware.app.cashregister.domain.CashSession;
import com.vetsoftware.app.cashregister.domain.CashSessionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cierra la caja: arma el conteo declarado por método y delega en el agregado
 * el cálculo esperado vs contado → diferencia y la materialización de los
 * {@code CashSessionCount}. El guard de "solo si está abierta" vive en el
 * dominio.
 */
@Observed(name = "cash.register.close.session")
@Service
public class CloseCashSessionService implements CloseCashSessionUseCase {

    private final CashSessionRepository repository;
    private final CashMetrics cashMetrics;

    public CloseCashSessionService(CashSessionRepository repository, CashMetrics cashMetrics) {
        this.repository = repository;
        this.cashMetrics = cashMetrics;
    }

    @Override
    @Transactional
    public CashSessionView close(CloseCashSessionCommand command, boolean adminOverride) {
        CashSession session = repository
                .findByIdAndCompany(command.sessionId(), command.companyId())
                .orElseThrow(() -> new CashSessionNotFoundException(command.sessionId()));
        if (!adminOverride
                && !Objects.equals(session.getOpenedByEmployeeId(), command.closedByEmployeeId())) {
            throw new AccessDeniedException(
                    "Only the employee who opened the cash session or an admin can close it");
        }
        Map<CashPaymentMethod, BigDecimal> counted = new LinkedHashMap<>();
        if (command.counts() != null) {
            for (CloseCashSessionCommand.Count c : command.counts()) {
                if (c.method() != null && c.countedAmount() != null)
                    counted.put(c.method(), c.countedAmount());
            }
        }
        session.close(command.closedByEmployeeId(), counted, command.note());
        CashSession saved = repository.save(session);
        cashMetrics.closed(saved.getCounts().stream().map(count -> count.difference()).toList());
        return CashSessionView.from(saved);
    }
}
